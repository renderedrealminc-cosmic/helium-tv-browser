#ifndef HELIUM_NATIVE_H
#define HELIUM_NATIVE_H

#include <jni.h>
#include <string>
#include <vector>
#include <chrono>
#include <android/log.h>

#define LOG_TAG "HeliumNativeCpp"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

class TvHardwarePerformanceMonitor {
private:
    std::chrono::high_resolution_clock::time_point last_frame_time;
    double current_fps;
    int dropped_frames_count;

public:
    TvHardwarePerformanceMonitor();
    void record_frame();
    double get_current_fps() const;
    int get_dropped_frames() const;
    void reset();
};

#endif // HELIUM_NATIVE_H
