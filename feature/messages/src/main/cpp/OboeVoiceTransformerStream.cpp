#include "OboeVoiceTransformerStream.h"
#include <cstring>
#include <algorithm>

static const double PI = 3.14159265358979323846;

OboeVoiceTransformerStream::OboeVoiceTransformerStream() {
    mReadBuffer.resize(4096, 0);
}

OboeVoiceTransformerStream::~OboeVoiceTransformerStream() {
    closeStreams();
}

bool OboeVoiceTransformerStream::start() {
    if (mIsRunning.load()) {
        return true;
    }

    // 1. Open Input Stream (Mic Capture)
    oboe::AudioStreamBuilder inBuilder;
    inBuilder.setDirection(oboe::Direction::Input)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Shared)
        ->setSampleRate(kSampleRate)
        ->setChannelCount(kChannelCount)
        ->setFormat(oboe::AudioFormat::I16)
        ->setInputPreset(oboe::InputPreset::VoiceCommunication);

    oboe::Result inResult = inBuilder.openStream(inputStream);
    if (inResult != oboe::Result::OK) {
        LOGE("Failed to open Oboe Input Stream: %s", oboe::convertToText(inResult));
        return false;
    }

    // 2. Open Output Stream (Playback)
    oboe::AudioStreamBuilder outBuilder;
    outBuilder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Shared)
        ->setSampleRate(kSampleRate)
        ->setChannelCount(kChannelCount)
        ->setFormat(oboe::AudioFormat::I16)
        ->setUsage(oboe::Usage::VoiceCommunication)
        ->setDataCallback(this)
        ->setErrorCallback(this);

    oboe::Result outResult = outBuilder.openStream(outputStream);
    if (outResult != oboe::Result::OK) {
        LOGE("Failed to open Oboe Output Stream: %s", oboe::convertToText(outResult));
        inputStream->close();
        inputStream.reset();
        return false;
    }

    // 3. AudioStream Buffer Size Management (Size = latency)
    // Query getFramesPerBurst to set buffer capacity to 2 * burst for low latency
    int32_t burst = outputStream->getFramesPerBurst();
    LOGI("Oboe Output Stream: getFramesPerBurst = %d", burst);
    auto bufferSizeResult = outputStream->setBufferSizeInFrames(burst * 2);
    if (bufferSizeResult) {
        LOGI("Oboe Output Stream buffer set to %d frames (2 * burst)", bufferSizeResult.value());
    } else {
        LOGW("Failed to set buffer size: %s", oboe::convertToText(bufferSizeResult.error()));
    }

    // Ensure read buffer is large enough for bursts
    if (mReadBuffer.size() < (size_t)(burst * 4)) {
        mReadBuffer.resize(burst * 4, 0);
    }

    // 4. Start recording and playback streams
    oboe::Result startIn = inputStream->requestStart();
    if (startIn != oboe::Result::OK) {
        LOGE("Failed to start Oboe Input Stream: %s", oboe::convertToText(startIn));
        closeStreams();
        return false;
    }

    oboe::Result startOut = outputStream->requestStart();
    if (startOut != oboe::Result::OK) {
        LOGE("Failed to start Oboe Output Stream: %s", oboe::convertToText(startOut));
        closeStreams();
        return false;
    }

    mIsRunning.store(true);
    LOGI("Oboe Voice Transformer streams started successfully");
    return true;
}

void OboeVoiceTransformerStream::stop() {
    mIsRunning.store(false);
    if (outputStream) {
        outputStream->requestStop();
    }
    if (inputStream) {
        inputStream->requestStop();
    }
}

void OboeVoiceTransformerStream::closeStreams() {
    mIsRunning.store(false);

    if (outputStream) {
        outputStream->stop();
        outputStream->close();
        outputStream.reset();
        LOGI("Oboe Output Stream closed");
    }

    if (inputStream) {
        inputStream->stop();
        inputStream->close();
        inputStream.reset();
        LOGI("Oboe Input Stream closed");
    }
}

void OboeVoiceTransformerStream::setParameters(float pitchSemiTones, float formantScale, bool isCyberSynth) {
    mPitchSemiTones.store(pitchSemiTones);
    mFormantScale.store(formantScale);
    mIsCyberSynth.store(isCyberSynth);
}

float OboeVoiceTransformerStream::getRms() const {
    return mCurrentRms.load();
}

void OboeVoiceTransformerStream::processBuffer(int16_t *buffer, int32_t length) {
    if (length <= 0 || buffer == nullptr) return;

    // Calculate input RMS
    double sumSquares = 0.0;
    for (int32_t i = 0; i < length; ++i) {
        double s = buffer[i];
        sumSquares += s * s;
    }
    float rms = static_cast<float>(std::sqrt(sumSquares / length) / 32768.0);
    float prevRms = mCurrentRms.load();
    mCurrentRms.store(std::clamp(prevRms * 0.7f + rms * 0.3f, 0.0f, 1.0f));

    float pitchSemi = mPitchSemiTones.load();
    float formant = mFormantScale.load();
    bool isCyber = mIsCyberSynth.load();

    // Natural bypass
    if (pitchSemi == 0.0f && formant == 1.0f && !isCyber) {
        return;
    }

    // Cyber Synth: Robotic ring modulation
    if (isCyber) {
        const double carrierFreq = 140.0;
        const double phaseInc = 2.0 * PI * carrierFreq / kSampleRate;
        for (int32_t i = 0; i < length; ++i) {
            mPhaseAccumulator += phaseInc;
            if (mPhaseAccumulator > 2.0 * PI) {
                mPhaseAccumulator -= 2.0 * PI;
            }
            double modulation = std::sin(mPhaseAccumulator);
            double val = buffer[i] * (0.6 + 0.4 * modulation);
            buffer[i] = static_cast<int16_t>(std::clamp(static_cast<int32_t>(val), -32768, 32767));
        }
        return;
    }

    // Vocal Pitch & Formant Shift
    double pitchRatio = std::pow(2.0, static_cast<double>(pitchSemi) / 12.0);
    double effectiveRatio = pitchRatio * (1.0 / std::sqrt(static_cast<double>(formant)));

    std::vector<int16_t> temp(buffer, buffer + length);
    double inIdx = 0.0;

    for (int32_t outIdx = 0; outIdx < length; ++outIdx) {
        int32_t idxFloor = static_cast<int32_t>(inIdx);
        float frac = static_cast<float>(inIdx - idxFloor);

        if (idxFloor + 1 < length) {
            float s0 = temp[idxFloor];
            float s1 = temp[idxFloor + 1];
            float sample = s0 + frac * (s1 - s0);

            // Formant resonance filter
            if (formant > 1.05f) {
                // High-frequency emphasis for woman / child oral resonance
                float prev = (idxFloor > 0) ? temp[idxFloor - 1] : s0;
                sample = sample + 0.35f * (sample - prev);
            } else if (formant < 0.95f) {
                // Low-frequency enhancement for man / studio deep chest body
                float prev = (idxFloor > 0) ? temp[idxFloor - 1] : s0;
                sample = 0.65f * sample + 0.35f * prev;
            }

            buffer[outIdx] = static_cast<int16_t>(std::clamp(static_cast<int32_t>(sample), -32768, 32767));
        } else {
            buffer[outIdx] = (idxFloor < length) ? temp[idxFloor] : 0;
        }

        inIdx += effectiveRatio;
        if (inIdx >= length - 2) {
            inIdx = 0.0;
        }
    }
}

oboe::DataCallbackResult OboeVoiceTransformerStream::onAudioReady(
    oboe::AudioStream *oboeStream,
    void *audioData,
    int32_t numFrames
) {
    int16_t *outputData = static_cast<int16_t*>(audioData);

    if (!mIsRunning.load() || inputStream == nullptr) {
        std::memset(outputData, 0, numFrames * sizeof(int16_t));
        return oboe::DataCallbackResult::Continue;
    }

    if (mReadBuffer.size() < (size_t)numFrames) {
        mReadBuffer.resize(numFrames, 0);
    }

    // Read mic input non-blocking (timeout 0)
    auto readResult = inputStream->read(mReadBuffer.data(), numFrames, 0);
    if (readResult && readResult.value() > 0) {
        int32_t framesRead = readResult.value();

        // Process audio in-place through DSP
        processBuffer(mReadBuffer.data(), framesRead);

        // Copy into audioData
        std::memcpy(outputData, mReadBuffer.data(), framesRead * sizeof(int16_t));

        // Fill remaining frames with silence if underrun
        if (framesRead < numFrames) {
            std::memset(outputData + framesRead, 0, (numFrames - framesRead) * sizeof(int16_t));
        }
    } else {
        // No input frames ready yet
        std::memset(outputData, 0, numFrames * sizeof(int16_t));
    }

    return oboe::DataCallbackResult::Continue;
}

void OboeVoiceTransformerStream::onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) {
    LOGW("Oboe AudioStream error after close: %s", oboe::convertToText(error));
}

// -------------------------------------------------------------
// JNI Exports for VoiceTransformerEngine.kt
// -------------------------------------------------------------

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_campus_network_feature_messages_audio_VoiceTransformerEngine_nativeInit(
    JNIEnv *env,
    jobject thiz
) {
    auto *engine = new OboeVoiceTransformerStream();
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT jboolean JNICALL
Java_com_campus_network_feature_messages_audio_VoiceTransformerEngine_nativeStartStream(
    JNIEnv *env,
    jobject thiz,
    jlong handle
) {
    auto *engine = reinterpret_cast<OboeVoiceTransformerStream*>(handle);
    if (engine == nullptr) return JNI_FALSE;
    return engine->start() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_campus_network_feature_messages_audio_VoiceTransformerEngine_nativeStopStream(
    JNIEnv *env,
    jobject thiz,
    jlong handle
) {
    auto *engine = reinterpret_cast<OboeVoiceTransformerStream*>(handle);
    if (engine != nullptr) {
        engine->stop();
    }
}

JNIEXPORT void JNICALL
Java_com_campus_network_feature_messages_audio_VoiceTransformerEngine_nativeSetParameters(
    JNIEnv *env,
    jobject thiz,
    jlong handle,
    jfloat pitchSemiTones,
    jfloat formantScale,
    jboolean isCyberSynth
) {
    auto *engine = reinterpret_cast<OboeVoiceTransformerStream*>(handle);
    if (engine != nullptr) {
        engine->setParameters(pitchSemiTones, formantScale, isCyberSynth);
    }
}

JNIEXPORT jfloat JNICALL
Java_com_campus_network_feature_messages_audio_VoiceTransformerEngine_nativeGetRms(
    JNIEnv *env,
    jobject thiz,
    jlong handle
) {
    auto *engine = reinterpret_cast<OboeVoiceTransformerStream*>(handle);
    return (engine != nullptr) ? engine->getRms() : 0.0f;
}

JNIEXPORT void JNICALL
Java_com_campus_network_feature_messages_audio_VoiceTransformerEngine_nativeProcessBuffer(
    JNIEnv *env,
    jobject thiz,
    jlong handle,
    jshortArray inputBuffer,
    jint length
) {
    auto *engine = reinterpret_cast<OboeVoiceTransformerStream*>(handle);
    if (engine == nullptr || inputBuffer == nullptr || length <= 0) return;

    jshort *elements = env->GetShortArrayElements(inputBuffer, nullptr);
    if (elements != nullptr) {
        engine->processBuffer(reinterpret_cast<int16_t*>(elements), length);
        env->ReleaseShortArrayElements(inputBuffer, elements, 0);
    }
}

JNIEXPORT void JNICALL
Java_com_campus_network_feature_messages_audio_VoiceTransformerEngine_nativeClose(
    JNIEnv *env,
    jobject thiz,
    jlong handle
) {
    auto *engine = reinterpret_cast<OboeVoiceTransformerStream*>(handle);
    if (engine != nullptr) {
        engine->closeStreams();
        delete engine;
    }
}

} // extern "C"
