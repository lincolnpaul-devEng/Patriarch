package com.campus.network.core.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest

/**
 * Runtime defenses against reverse engineering and tampering.
 *
 * These checks raise the cost of attack; they cannot make a client app unbreakable.
 * Authorization must still be enforced server side.
 */
object RuntimeIntegrityGuard {

    enum class Threat {
        DEBUGGER_ATTACHED,
        TRACER_PROCESS,
        INSTRUMENTATION_FRAMEWORK,
        ROOTED_DEVICE,
        EMULATOR,
        SIGNATURE_MISMATCH,
        UNTRUSTED_INSTALLER,
        DEBUGGABLE_BUILD
    }

    data class Report(val threats: Set<Threat>) {
        /** Threats that should hard-stop a release build. */
        val isCritical: Boolean
            get() = threats.any {
                it == Threat.DEBUGGER_ATTACHED ||
                    it == Threat.TRACER_PROCESS ||
                    it == Threat.INSTRUMENTATION_FRAMEWORK ||
                    it == Threat.SIGNATURE_MISMATCH ||
                    it == Threat.DEBUGGABLE_BUILD
            }

        /** Threats that should disable sensitive features (escrow, QR release) but not kill the app. */
        val isElevatedRisk: Boolean
            get() = threats.any { it == Threat.ROOTED_DEVICE || it == Threat.EMULATOR || it == Threat.UNTRUSTED_INSTALLER }
    }

    private val ROOT_PATHS = listOf(
        "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
        "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
        "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su",
        "/data/adb/magisk", "/sbin/.magisk"
    )

    private val INSTRUMENTATION_MARKERS = listOf(
        "frida", "gum-js-loop", "gmain", "linjector", "xposed", "lsposed", "substrate", "zygisk"
    )

    /**
     * @param expectedSignatureSha256 hex SHA-256 of the release signing certificate. Blank skips the check
     *        (debug builds / until the release key is provisioned).
     * @param enforceInstaller when true, flags installs that did not come from Google Play.
     */
    fun evaluate(
        context: Context,
        expectedSignatureSha256: String,
        enforceInstaller: Boolean = false
    ): Report {
        val threats = mutableSetOf<Threat>()
        if (isDebuggerAttached()) threats += Threat.DEBUGGER_ATTACHED
        if (isBeingTraced()) threats += Threat.TRACER_PROCESS
        if (hasInstrumentationFramework()) threats += Threat.INSTRUMENTATION_FRAMEWORK
        if (isRooted()) threats += Threat.ROOTED_DEVICE
        if (isEmulator()) threats += Threat.EMULATOR
        if (isDebuggableBuild(context)) threats += Threat.DEBUGGABLE_BUILD
        if (expectedSignatureSha256.isNotBlank() && !signatureMatches(context, expectedSignatureSha256)) {
            threats += Threat.SIGNATURE_MISMATCH
        }
        if (enforceInstaller && !isInstalledFromPlayStore(context)) threats += Threat.UNTRUSTED_INSTALLER
        return Report(threats)
    }

    private fun isDebuggerAttached(): Boolean =
        Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    private fun isDebuggableBuild(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /** ptrace/strace/gdb attach shows up as a non-zero TracerPid. */
    private fun isBeingTraced(): Boolean = runCatching {
        BufferedReader(FileReader("/proc/self/status")).useLines { lines ->
            lines.firstOrNull { it.startsWith("TracerPid:") }
                ?.substringAfter(':')?.trim()?.toIntOrNull()?.let { it != 0 } ?: false
        }
    }.getOrDefault(false)

    private fun hasInstrumentationFramework(): Boolean {
        val mapsHit = runCatching {
            BufferedReader(FileReader("/proc/self/maps")).useLines { lines ->
                lines.any { line ->
                    val l = line.lowercase()
                    INSTRUMENTATION_MARKERS.any { l.contains(it) }
                }
            }
        }.getOrDefault(false)
        if (mapsHit) return true

        val threadHit = runCatching {
            File("/proc/self/task").listFiles()?.any { task ->
                val name = File(task, "comm").readText().trim().lowercase()
                name.contains("frida") || name.contains("gum-js") || name.contains("gdbus")
            } ?: false
        }.getOrDefault(false)
        if (threadHit) return true

        // Default frida-server port on loopback.
        return runCatching {
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", 27042), 120); true }
        }.getOrDefault(false)
    }

    private fun isRooted(): Boolean {
        if (Build.TAGS?.contains("test-keys") == true) return true
        if (ROOT_PATHS.any { File(it).exists() }) return true
        return runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val found = p.inputStream.bufferedReader().readLine() != null
            p.destroy()
            found
        }.getOrDefault(false)
    }

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator", true) ||
            Build.MODEL.contains("Emulator", true) ||
            Build.MODEL.contains("Android SDK built for", true) ||
            Build.MANUFACTURER.contains("Genymotion", true) ||
            Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.contains("sdk_gphone") || Build.PRODUCT.contains("vbox")

    @Suppress("DEPRECATION")
    private fun signatureMatches(context: Context, expectedHex: String): Boolean = runCatching {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo ?: return@runCatching false
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val expected = expectedHex.replace(":", "").lowercase()
        signatures?.any { sig ->
            val digest = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
            digest.joinToString("") { "%02x".format(it) } == expected
        } == true
    }.getOrDefault(false)

    @Suppress("DEPRECATION")
    private fun isInstalledFromPlayStore(context: Context): Boolean = runCatching {
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            context.packageManager.getInstallerPackageName(context.packageName)
        }
        installer == "com.android.vending"
    }.getOrDefault(false)
}
