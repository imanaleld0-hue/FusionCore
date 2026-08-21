#include "exports.h"
#include "logger.h"
#include "hooking/safehook.h"
#include <cstring>
#include <string>

static std::string g_auth_token;
static std::string g_auth_user_id;
static std::string g_auth_display_name;
static std::string g_auth_email;
static bool g_auth_active = false;

void init_bridge_helper(const char *libraryPath)
{
    safehook_setup_bridge_helper(libraryPath);
}

dobby_dummy_func_t hook(void *address, dobby_dummy_func_t replace_delegate, bool specialReturnBuffer)
{
    return safehook_create_hook(address, replace_delegate, specialReturnBuffer);
}

void unhook(void *target)
{
    safehook_destroy_hook(target);
}

void create_alert(const char *title, const char *message)
{
    (void)title;
    (void)message;
}

void write_log(const char *text)
{
    log(LogLevel::INFO, "Fusion.NET", text);
}

void write_log_level(int level, const char *text)
{
    LogLevel logLevel = static_cast<LogLevel>(level);
    log(logLevel, "Fusion.NET", text);
}


extern "C" void fusion_set_auth_token(const char *token)
{
    g_auth_token = token ? token : "";
    log_format(LogLevel::INFO, "FusionAuth", "Token set: {}...", g_auth_token.substr(0, 6));
}

extern "C" void fusion_set_auth_user_id(const char *userId)
{
    g_auth_user_id = userId ? userId : "";
}

extern "C" void fusion_set_auth_display_name(const char *displayName)
{
    g_auth_display_name = displayName ? displayName : "";
}

extern "C" void fusion_set_auth_email(const char *email)
{
    g_auth_email = email ? email : "";
}

extern "C" void fusion_clear_auth()
{
    g_auth_token.clear();
    g_auth_user_id.clear();
    g_auth_display_name.clear();
    g_auth_email.clear();
    g_auth_active = false;
    log(LogLevel::INFO, "FusionAuth", "Auth cleared");
}

extern "C" const char* fusion_get_auth_token()
{
    return g_auth_token.c_str();
}

extern "C" const char* fusion_get_auth_user_id()
{
    return g_auth_user_id.c_str();
}

extern "C" bool fusion_is_auth_active()
{
    return g_auth_active && !g_auth_token.empty();
}
