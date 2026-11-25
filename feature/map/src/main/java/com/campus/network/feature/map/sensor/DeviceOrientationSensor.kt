package com.campus.network.feature.map.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.campus.network.feature.map.domain.DeviceOrientationState
import com.campus.network.feature.map.domain.GeoSpatialMath
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Streams smoothed device pitch (0 deg = flat, 90 deg = upright portrait) and
 * true-ish compass azimuth from `Sensor.TYPE_ROTATION_VECTOR`, driving the
 * hysteresis tilt-to-switch gate (< 35 deg 2D map, > 45 deg AR camera).
 */
@Singleton
class DeviceOrientationSensor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    fun observeOrientation(): Flow<DeviceOrientationState> = callbackFlow {
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val accelSensor = if (rotationSensor == null) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            null
        }
        val magSensor = if (rotationSensor == null) {
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        } else {
            null
        }

        if (rotationSensor == null && accelSensor == null) {
            trySend(DeviceOrientationState())
            awaitClose { }
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val gravityValues = FloatArray(3)
        val magneticValues = FloatArray(3)
        var hasMagneticSample = false
        var smoothedPitch = 0f
        var smoothedRoll = 0f
        var smoothedSinAz = 0f
        var smoothedCosAz = 1f
        var hasInitialSample = false
        var arActive = false

        fun emitSmoothed(rawPitchDeg: Float, rawAzDeg: Float, rawRollDeg: Float) {
            if (!hasInitialSample) {
                hasInitialSample = true
                smoothedPitch = rawPitchDeg
                smoothedRoll = rawRollDeg
                smoothedSinAz = sin(Math.toRadians(rawAzDeg.toDouble())).toFloat()
                smoothedCosAz = cos(Math.toRadians(rawAzDeg.toDouble())).toFloat()
            } else {
                val alpha = 0.18f
                smoothedPitch += alpha * (rawPitchDeg - smoothedPitch)
                smoothedRoll += alpha * (rawRollDeg - smoothedRoll)
                val rSin = sin(Math.toRadians(rawAzDeg.toDouble())).toFloat()
                val rCos = cos(Math.toRadians(rawAzDeg.toDouble())).toFloat()
                smoothedSinAz += alpha * (rSin - smoothedSinAz)
                smoothedCosAz += alpha * (rCos - smoothedCosAz)
            }

            val azLen = sqrt((smoothedSinAz * smoothedSinAz + smoothedCosAz * smoothedCosAz).toDouble())
                .coerceAtLeast(1e-4)
            val normAzDeg = (((Math.toDegrees(
                atan2(smoothedSinAz / azLen, smoothedCosAz / azLen)
            ) % 360.0) + 360.0) % 360.0).toFloat()

            arActive = GeoSpatialMath.computeTiltHysteresis(arActive, smoothedPitch)

            trySend(
                DeviceOrientationState(
                    pitchDegrees = smoothedPitch,
                    azimuthDegrees = normAzDeg,
                    rollDegrees = smoothedRoll,
                    isArPitchActive = arActive
                )
            )
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR,
                    Sensor.TYPE_GAME_ROTATION_VECTOR,
                    Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                        @Suppress("DEPRECATION")
                        val displayRot = runCatching { windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0 }
                            .getOrDefault(Surface.ROTATION_0)
                        val (axisX, axisY) = when (displayRot) {
                            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                        }
                        SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)

                        val screenUpZ = remappedMatrix[7].coerceIn(-1f, 1f)
                        val rawPitchDeg = Math.toDegrees(asin(screenUpZ.toDouble())).toFloat()

                        SensorManager.getOrientation(remappedMatrix, orientationAngles)
                        val rawAzimuthRad = if (rawPitchDeg > 35f) {
                            val fwdEast = -remappedMatrix[2]
                            val fwdNorth = -remappedMatrix[5]
                            atan2(fwdEast.toDouble(), fwdNorth.toDouble())
                        } else {
                            orientationAngles[0].toDouble()
                        }
                        val rawAzDeg = (((Math.toDegrees(rawAzimuthRad) % 360.0) + 360.0) % 360.0).toFloat()
                        val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                        emitSmoothed(rawPitchDeg, rawAzDeg, rollDeg)
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (event.values.size >= 3) {
                            magneticValues[0] = event.values[0]
                            magneticValues[1] = event.values[1]
                            magneticValues[2] = event.values[2]
                            hasMagneticSample = true
                        }
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        if (event.values.size < 3) return
                        gravityValues[0] = event.values[0]
                        gravityValues[1] = event.values[1]
                        gravityValues[2] = event.values[2]

                        val gx = gravityValues[0].toDouble()
                        val gy = gravityValues[1].toDouble()
                        val gz = gravityValues[2].toDouble()
                        val gNorm = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(1e-3)

                        // Pitch: 0 deg when phone is flat (gy ~ 0, gz ~ 9.8), 90 deg when held upright in portrait (gy ~ 9.8)
                        val sinPitch = (gy / gNorm).coerceIn(-1.0, 1.0)
                        val rawPitchDeg = Math.toDegrees(asin(sinPitch)).toFloat().coerceAtLeast(0f)
                        val rawRollDeg = Math.toDegrees(atan2(-gx, gz.coerceAtLeast(0.1))).toFloat()

                        var rawAzDeg = 0f
                        if (hasMagneticSample && SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, magneticValues)) {
                            SensorManager.getOrientation(rotationMatrix, orientationAngles)
                            rawAzDeg = (((Math.toDegrees(orientationAngles[0].toDouble()) % 360.0) + 360.0) % 360.0).toFloat()
                        }
                        emitSmoothed(rawPitchDeg, rawAzDeg, rawRollDeg)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelSensor?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
            }
            magSensor?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
