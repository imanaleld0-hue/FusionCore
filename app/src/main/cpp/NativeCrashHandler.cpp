#include <jni.h>
#include <android/log.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <ucontext.h>
#include <dlfcn.h>

#define LOG_TAG "FusionCore_NativeCrash"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static char log_file_path[256];
static struct sigaction old_sa[NSIG];

void crash_handler(int sig, siginfo_t *info, void *context) {
    int fd = open(log_file_path, O_WRONLY | O_APPEND | O_CREAT, 0666);
    if (fd >= 0) {
        char buffer[1024];
        ucontext_t *uc = (ucontext_t *)context;
        snprintf(buffer, sizeof(buffer),
                 "\n========== NATIVE CRASH ==========\n"
                 "Fatal signal %d\n"
                 "Fault address: %p\n"
                 "PID: %d, TID: %d\n"
                 "==================================\n",
                 sig, info->si_addr, getpid(), gettid());
        write(fd, buffer, strlen(buffer));
        close(fd);
    }
    LOGE("Native crash handled. Signal: %d, Addr: %p", sig, info->si_addr);
    sigaction(sig, &old_sa[sig], NULL);
    kill(getpid(), sig);
}

extern "C" JNIEXPORT void JNICALL
Java_dev_allofus_fusioncore_logging_NativeCrashHandler_init(JNIEnv *env, jclass clazz, jstring path) {
    const char *path_str = env->GetStringUTFChars(path, 0);
    snprintf(log_file_path, sizeof(log_file_path), "%s/Logs.txt", path_str);
    env->ReleaseStringUTFChars(path, path_str);

    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_flags = SA_RESTART | SA_SIGINFO;
    sa.sa_sigaction = crash_handler;
    sigemptyset(&sa.sa_mask);

    sigaction(SIGSEGV, &sa, &old_sa[SIGSEGV]);
    sigaction(SIGABRT, &sa, &old_sa[SIGABRT]);
    sigaction(SIGILL,  &sa, &old_sa[SIGILL]);
    sigaction(SIGBUS,  &sa, &old_sa[SIGBUS]);
}
