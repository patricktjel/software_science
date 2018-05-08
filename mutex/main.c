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
    snprintf(b, 256, "./BDD-%d.dot", i);
    FILE *f = fopen(b, "w+");
    mtbdd_fprintdot_nc(f, bdd);
    fclose(f);
}

void bdd() {
    LACE_ME;

    BDD one  = sylvan_true;
    BDD zero = sylvan_false;

    BDD CS = sylvan_ithvar(1);
    sylvan_protect(&CS);
    BDD CS1 = sylvan_ithvar(11);
    sylvan_protect(&CS1);
    BDD W = sylvan_ithvar(2);
    sylvan_protect(&W);
    BDD W1 = sylvan_ithvar(21);
    sylvan_protect(&W1);
    BDD F = sylvan_ithvar(3);
    sylvan_protect(&F);
    BDD F1 = sylvan_ithvar(31);
    sylvan_protect(&F1);

    BDD set = sylvan_set_empty();
    set = sylvan_set_add(set,CS);
    set = sylvan_set_add(set,W);
    set = sylvan_set_add(set,F);

    BDD initState = sylvan_and(sylvan_not(CS), sylvan_and(W, sylvan_not(F)));

    BDD oneToThree = sylvan_and(sylvan_and(sylvan_not(CS), CS1),
                                sylvan_and(W, sylvan_not(W1)));

    BDD threeToFour = sylvan_and(CS, sylvan_not(CS1));

    BDD threeToTwo = sylvan_and(sylvan_and(CS,sylvan_not(CS1)),
                                sylvan_and(sylvan_not(F), F1));

    BDD TwoToOne = sylvan_and(sylvan_and(sylvan_not(W), W1),
                              sylvan_and(F, sylvan_not(F1)));



    visualize_bdd(initState);

    sylvan_unprotect(&CS);
    sylvan_unprotect(&CS1);
    sylvan_unprotect(&W);
    sylvan_unprotect(&W1);
    sylvan_unprotect(&F);
    sylvan_unprotect(&F1);
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