#include <stdio.h>
#include <sylvan.h>

int main() {
    init();



    quit();
    return 0;
}

void init() {
    // 0 = auto    −    detect  number  of  Lace  workers
    int n_workers = 0;

    // initialize Lace with a deque size of 4M
    int deque_size = 40960000;

    lace_init(n_workers, deque_size);
    lace_startup(0, NULL, NULL);

    /* initialize Sylvan's node table and operations cache
     * with at least 2^20 entries, and at most 2^25 entries */
    sylvan_init_package(1LL<<20,1LL<<25,1LL<<20,1LL<<25);
    sylvan_init_bdd();
}

void quit() {
    /* if Sylvan is compiled with -DSYLVAN_STATS=ON
     * then print statistics on stderr. */
    sylvan_stats_report(stderr);
    // deinitialize Sylvan
    sylvan_quit();
    // deinitialize Lace
    lace_exit();
}