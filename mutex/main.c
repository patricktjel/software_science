#include <stdio.h>
#include <sylvan.h>
#include <stdbool.h>

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

void visualize_bdd(BDD bdd) {
    int i = 0;
    char b[256];
    snprintf(b, 256, "/tmp/sylvan/BDD-%d.dot", i);
    FILE *f = fopen(b, "w+");
    sylvan_fprintdot(f, bdd);
    fclose(f);
}

BDD* allocate_var() {
    BDD* my_var = (BDD*)calloc(sizeof(BDD), 1);
    sylvan_protect(my_var);
    return my_var;
}

void free_var(BDD* my_var) {
    sylvan_unprotect(my_var);
    free(my_var);
}

void bdd() {
    LACE_ME;
    // init variables
//    bool cs = false;
//    int wait = 1;
//    int finished = 0;


    BDD zero = sylvan_false;
    BDD one  = sylvan_true;

    BDD test = sylvan_ithvar(2);
    assert(sylvan_high(test) == one);
    assert(sylvan_low(test) == zero);
    visualize_bdd(test);
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

int main() {
    init();

    bdd();

    quit();
    return 0;
}