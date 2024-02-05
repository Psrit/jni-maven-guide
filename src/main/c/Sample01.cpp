#include "learn_jni_Sample01.h"
#include <string.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_learn_jni_Sample01_square(JNIEnv *env, jobject obj, jint n) {
    return n * n;
}

JNIEXPORT jboolean JNICALL Java_learn_jni_Sample01_aBool(JNIEnv *env, jobject obj, jboolean boolean) {
    return !boolean;
}

JNIEXPORT jstring JNICALL Java_learn_jni_Sample01_text(JNIEnv *env, jobject obj, jstring string) {
    const char *str = env->GetStringUTFChars(string, 0);
    char cap[128];
    strcpy(cap, str);
    env->ReleaseStringUTFChars(string, str);
    return env->NewStringUTF(cap);
}

JNIEXPORT jint JNICALL Java_learn_jni_Sample01_sum(JNIEnv *env, jobject obj, jintArray array) {
    int i, sum = 0;
    jsize len = env->GetArrayLength(array);
    jint *body = env->GetIntArrayElements(array, 0);
    for (i = 0; i < len; i++) {
        sum += body[i];
    }
    env->ReleaseIntArrayElements(array, body, 0);
    return sum;
}

#ifdef __cplusplus
} // extern "C"
#endif