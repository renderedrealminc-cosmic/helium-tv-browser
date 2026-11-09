#include "helium_native.h"
#include <unistd.h>
#include <sys/sysinfo.h>
#include <sstream>

TvHardwarePerformanceMonitor::TvHardwarePerformanceMonitor() 
    : current_fps(60.0), dropped_frames_count(0) {
    last_frame_time = std::chrono::high_resolution_clock::now();
}

void TvHardwarePerformanceMonitor::record_frame() {
    auto now = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double, std::milli> elapsed = now - last_frame_time;
    last_frame_time = now;

    double ms = elapsed.count();
    if (ms > 0.0) {
        double instant_fps = 1000.0 / ms;
        // Exponential moving average for smooth TV FPS readings
        current_fps = (0.9 * current_fps) + (0.1 * instant_fps);

        // Standard 60Hz threshold is 16.6ms, dropped if > 25ms
        if (ms > 25.0) {
            dropped_frames_count++;
        }
    }
}

double TvHardwarePerformanceMonitor::get_current_fps() const {
    return current_fps;
}

int TvHardwarePerformanceMonitor::get_dropped_frames() const {
    return dropped_frames_count;
}

void TvHardwarePerformanceMonitor::reset() {
    current_fps = 60.0;
    dropped_frames_count = 0;
    last_frame_time = std::chrono::high_resolution_clock::now();
}

static TvHardwarePerformanceMonitor g_perf_monitor;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_org_helium_tv_browser_engine_TvNativeCore_nativeInitHardwareOptimizer(
    JNIEnv* /* env */,
    jclass /* clazz */) {
    LOGI("Helium TV Native C++ Hardware Engine Initialized");
    g_perf_monitor.reset();
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_org_helium_tv_browser_engine_TvNativeCore_nativeRecordFrame(
    JNIEnv* /* env */,
    jclass /* clazz */) {
    g_perf_monitor.record_frame();
}

JNIEXPORT jdouble JNICALL
Java_org_helium_tv_browser_engine_TvNativeCore_nativeGetFps(
    JNIEnv* /* env */,
    jclass /* clazz */) {
    return g_perf_monitor.get_current_fps();
}

JNIEXPORT jint JNICALL
Java_org_helium_tv_browser_engine_TvNativeCore_nativeGetDroppedFrames(
    JNIEnv* /* env */,
    jclass /* clazz */) {
    return g_perf_monitor.get_dropped_frames();
}

JNIEXPORT jstring JNICALL
Java_org_helium_tv_browser_engine_TvNativeCore_nativeGetHardwareCapabilities(
    JNIEnv* env,
    jclass /* clazz */) {
    struct sysinfo info;
    std::ostringstream ss;

    if (sysinfo(&info) == 0) {
        long total_ram_mb = info.totalram * info.mem_unit / (1024 * 1024);
        long free_ram_mb = info.freeram * info.mem_unit / (1024 * 1024);
        int cpu_cores = sysconf(_SC_NPROCESSORS_ONLN);

        ss << "Cores: " << cpu_cores 
           << " | RAM: " << free_ram_mb << "MB free / " << total_ram_mb << "MB total"
           << " | Engine: C++20 + Rust NDK";
    } else {
        ss << "Standard Android TV Hardware detected";
    }

    return env->NewStringUTF(ss.str().c_str());
}

} // extern "C"
