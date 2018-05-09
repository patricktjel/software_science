#include <config.h>

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include <util.h>

void
warn(const char *message, ...)
{
    va_list args;
    va_start(args, message);
    char msg[vsnprintf(NULL, 0, message, args) + 1];
    va_end(args);
    va_start(args, message);
    vsprintf(msg, message, args); 
    va_end(args);
    
    char buff[100];
    time_t now = time(NULL);
    strftime(buff, 100, "%Y-%m-%d %H:%M:%S", localtime (&now));
    fprintf(stderr, "%s: %s\n", buff, msg);
}

void*
mmalloc(size_t size) {
    void *const res = malloc(size);
    if (res == NULL) {
        warn("Unable to malloc %zu bytes", size);
        exit(1);
    } else return res;
}

void*
rrealloc(void *ptr, size_t size) {
    void *const res = realloc(ptr, size);
    if (res == NULL) {
        warn("Unable to realloc %zu bytes", size);
        exit(1);
    } else return res;
}
