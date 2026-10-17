#pragma once

#include <oboe/Oboe.h>
#include <jni.h>
#include <atomic>
#include <vector>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "OboeVoiceTransformer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

class OboeVoiceTransformerStream : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback {
public:
    OboeVoiceTransformerStream();
    virtual ~OboeVoiceTransformerStream();

    bool start();
    void stop();
    void closeStreams();

    void setParameters(float pitchSemiTones, float formantScale, bool isCyberSynth);
    float getRms() const;

    // Direct buffer DSP transformation for in-flight VoIP packets
    void processBuffer(int16_t *buffer, int32_t length);

    // Oboe callbacks
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override;
    void onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) override;

private:
    std::shared_ptr<oboe::AudioStream> inputStream;
    std::shared_ptr<oboe::AudioStream> outputStream;

    std::atomic<float> mPitchSemiTones{0.0f};
    std::atomic<float> mFormantScale{1.0f};
    std::atomic<bool> mIsCyberSynth{false};
    std::atomic<float> mCurrentRms{0.0f};

    std::atomic<bool> mIsRunning{false};

    // DSP State
    double mPhaseAccumulator = 0.0;
    std::vector<int16_t> mReadBuffer;

    static constexpr int32_t kSampleRate = 16000;
    static constexpr int32_t kChannelCount = 1;
};
