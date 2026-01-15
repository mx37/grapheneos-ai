/**
 * Stub implementation when llama.cpp sources are not present
 * Returns meaningful error messages to guide users
 */

#include <jni.h>
#include <android/log.h>
#include <string>

#define TAG "LlamaJNI-Stub"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("LlamaJNI stub loaded - llama.cpp sources not available");
    return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeInit(JNIEnv* env, jclass clazz) {
    LOGI("Stub: nativeInit called");
    return JNI_TRUE;  // Return true to allow app to run, models just won't work
}

JNIEXPORT jboolean JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeLoadModel(
    JNIEnv* env, jclass clazz, jstring modelPath, jint nCtx, jint nThreads, jboolean useGpu) {
    LOGE("Cannot load model: llama.cpp not compiled into APK");
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeUnloadModel(JNIEnv* env, jclass clazz) {
    LOGI("Stub: nativeUnloadModel");
}

JNIEXPORT jboolean JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeIsModelLoaded(JNIEnv* env, jclass clazz) {
    return JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeGenerate(
    JNIEnv* env, jclass clazz, jstring prompt, jint maxTokens, jfloat temperature, jfloat topP, jobject callback) {
    return env->NewStringUTF("[Local AI unavailable. To enable:\n1. Clone llama.cpp into app/src/main/cpp/llama/llama.cpp\n2. Rebuild the app with NDK support]");
}

JNIEXPORT void JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeStopGeneration(JNIEnv* env, jclass clazz) {
}

JNIEXPORT jstring JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeGetModelInfo(JNIEnv* env, jclass clazz) {
    return env->NewStringUTF("{\"status\":\"stub\",\"message\":\"llama.cpp not available\"}");
}

JNIEXPORT jlong JNICALL
Java_com_satory_graphenosai_llm_LlamaCppBridge_nativeGetMemoryUsage(JNIEnv* env, jclass clazz) {
    return 0;
}

} // extern "C"
