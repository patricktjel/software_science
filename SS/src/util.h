#ifndef UTIL_H
#define UTIL_H
/**
 * \brief prints a message on stderr, and ends it with a new line
 */
extern void warn(const char *message, ...);

/**
 * \brief allocates size bytes of memory, and exits with 1 if the
 * amount of memory is not available.
 */
extern void *mmalloc(size_t size);

/**
 * \brief reallocates ptr to size bytes of memory, and exits with 1 if
 * the amount of memory is not available.
 */
extern void *rrealloc(void *ptr, size_t size);
#endif
