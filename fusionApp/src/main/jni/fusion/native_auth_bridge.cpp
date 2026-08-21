#include <jni.h>
#include "exports.h"
#include "logger.h"

extern "C" {

JNIEXPORT void JNICALL
Java_dev_allofus_fusioncore_bridge_NativeAuthBridge_notifyAuthSuccess(
    JNIEnv *env, jclass clazz,
    jstring userId, jstring displayName, jstring email, jstring idToken)
{
    (void)clazz;
    const char *cUserId = env->GetStringUTFChars(userId, nullptr);
    const char *cDisplayName = env->GetStringUTFChars(displayName, nullptr);
    const char *cEmail = env->GetStringUTFChars(email, nullptr);
    const char *cToken = env->GetStringUTFChars(idToken, nullptr);

    fusion_set_auth_user_id(cUserId);
    fusion_set_auth_display_name(cDisplayName);
    fusion_set_auth_email(cEmail);
    fusion_set_auth_token(cToken);

    env->ReleaseStringUTFChars(userId, cUserId);
    env->ReleaseStringUTFChars(displayName, cDisplayName);
    env->ReleaseStringUTFChars(email, cEmail);
    env->ReleaseStringUTFChars(idToken, cToken);

    log(LogLevel::INFO, "NativeAuthBridge", "Auth success notified from Java");
}

JNIEXPORT void JNICALL
Java_dev_allofus_fusioncore_bridge_NativeAuthBridge_notifyAuthLogout(JNIEnv *env, jclass clazz)
{
    (void)env;
    (void)clazz;
    fusion_clear_auth();
}

JNIEXPORT jboolean JNICALL
Java_dev_allofus_fusioncore_bridge_NativeAuthBridge_isNativeAuthActive(JNIEnv *env, jclass clazz)
{
    (void)env;
    (void)clazz;
    return fusion_is_auth_active() ? JNI_TRUE : JNI_FALSE;
}

}

