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
    char b[256];
    snprintf(b, 256, "./BDD.dot");
    FILE *f = fopen(b, "w+");
    mtbdd_fprintdot(f, bdd);
    fclose(f);
}

void bdd() {
    LACE_ME;

    //create variables and protect them
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

    BDD initState = sylvan_and(sylvan_not(CS), sylvan_and(W, sylvan_not(F)));

    BDD oneToThree = sylvan_and(sylvan_and(sylvan_not(CS), CS1),
                                sylvan_and(W, sylvan_not(W1)));

    BDD threeToFour = sylvan_and(CS, sylvan_not(CS1));

    BDD threeToTwo = sylvan_and(sylvan_and(CS,sylvan_not(CS1)),
                                sylvan_and(sylvan_not(F), F1));

    BDD twoToOne = sylvan_and(sylvan_and(sylvan_not(W), W1),
                              sylvan_and(F, sylvan_not(F1)));

    BDD transitions[4] = {twoToOne, oneToThree, threeToFour, threeToTwo};

//    sylvan set for sylvan_exists
    BDD setOneToThree = sylvan_set_empty();
    sylvan_protect(&setOneToThree);
    setOneToThree = sylvan_set_add(setOneToThree,1);
    setOneToThree = sylvan_set_add(setOneToThree,2);

    BDD setThreeToFour = sylvan_set_empty();
    sylvan_protect(&setThreeToFour);
    setThreeToFour = sylvan_set_add(setThreeToFour,1);

    BDD setThreeToTwo = sylvan_set_empty();
    sylvan_protect(&setThreeToTwo);
    setThreeToTwo = sylvan_set_add(setThreeToTwo,1);
    setThreeToTwo = sylvan_set_add(setThreeToTwo,3);

    BDD setTwoToOne = sylvan_set_empty();
    sylvan_protect(&setTwoToOne);
    setTwoToOne = sylvan_set_add(setTwoToOne,2);
    setTwoToOne = sylvan_set_add(setTwoToOne,3);

    BDD result = sylvan_set_empty();
    sylvan_protect(&result);
    result = sylvan_set_add(result,1);
    result = sylvan_set_add(result,2);
    result = sylvan_set_add(result,3);

    BDD transitions_set[4] = {setTwoToOne, setOneToThree, setThreeToFour, setThreeToTwo};

//    sylvan map for sylvan compose
    BDDMAP mapOneToThree = sylvan_map_empty();
    sylvan_protect(&mapOneToThree);
    mapOneToThree = sylvan_map_add(mapOneToThree, 11, sylvan_ithvar(1));
    mapOneToThree = sylvan_map_add(mapOneToThree, 21, sylvan_ithvar(2));

    BDDMAP mapThreeToFour = sylvan_map_empty();
    sylvan_protect(&mapThreeToFour);
    mapThreeToFour = sylvan_map_add(mapThreeToFour, 11, sylvan_ithvar(1));

    BDDMAP mapThreeToTwo = sylvan_map_empty();
    sylvan_protect(&mapThreeToTwo);
    mapThreeToTwo = sylvan_map_add(mapThreeToTwo, 11, sylvan_ithvar(1));
    mapThreeToTwo = sylvan_map_add(mapThreeToTwo, 31, sylvan_ithvar(3));

    BDDMAP mapTwoToOne = sylvan_map_empty();
    sylvan_protect(&mapTwoToOne );
    mapTwoToOne  = sylvan_map_add(mapTwoToOne , 21, sylvan_ithvar(2));
    mapTwoToOne  = sylvan_map_add(mapTwoToOne , 31, sylvan_ithvar(3));

    BDD transitions_map[4] = {mapTwoToOne, mapOneToThree, mapThreeToFour, mapThreeToTwo};

//    while
    BDD cur = initState;
    BDD vis = cur;
    do {
        vis = cur;
        for (int i = 0; i < 4; i = i + 1) {
            BDD r = sylvan_and(cur, transitions[i]);

            r = sylvan_exists(r, transitions_set[i]);
            r = sylvan_compose(r, transitions_map[i]);

            cur = sylvan_or(cur, r);
        }
    } while (cur != vis);

    printf("%lf", sylvan_satcount(cur, result));
    printf("%lu", sylvan_nodecount(cur));
    visualize_bdd(cur);

    //unprotect the variables
    sylvan_unprotect(&CS);
    sylvan_unprotect(&CS1);
    sylvan_unprotect(&W);
    sylvan_unprotect(&W1);
    sylvan_unprotect(&F);
    sylvan_unprotect(&F1);
    sylvan_unprotect(&setOneToThree);
    sylvan_unprotect(&result);
    sylvan_unprotect(&setThreeToFour);
    sylvan_unprotect(&setThreeToTwo);
    sylvan_unprotect(&setTwoToOne);
    sylvan_unprotect(&mapOneToThree);
    sylvan_unprotect(&mapThreeToFour);
    sylvan_unprotect(&mapThreeToTwo);
    sylvan_unprotect(&mapTwoToOne);
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