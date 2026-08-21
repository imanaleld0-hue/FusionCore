#ifndef FUSIONCORE_EXPORTS_H
#define FUSIONCORE_EXPORTS_H
#include "external/dobby.h"

#ifdef __cplusplus
extern "C"
{
#endif

void init_bridge_helper(const char *libraryPath);

dobby_dummy_func_t hook(void *address, dobby_dummy_func_t replace_delegate, bool specialReturnBuffer);

void unhook(void *target);

void create_alert(const char *title, const char *message);

void write_log(const char *text);

void write_log_level(int level, const char *text);

void fusion_set_auth_token(const char *token);
void fusion_set_auth_user_id(const char *userId);
void fusion_set_auth_display_name(const char *displayName);
void fusion_set_auth_email(const char *email);
void fusion_clear_auth();
const char* fusion_get_auth_token();
const char* fusion_get_auth_user_id();
bool fusion_is_auth_active();

#ifdef __cplusplus
}
#endif

#endif
