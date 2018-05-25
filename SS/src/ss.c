#include <config.h>

#include <stdio.h>

#include <sylvan.h>

#include <andl.h>
#include <CTL.h>
#include <andl-lexer.h>
#include <ss-andl-parser.h>
#include <util.h>
#include <libxml/parser.h>
#include <libxml/tree.h>
#include <stdbool.h>
#include "andl.h"


bool debug = false;

/**
 * Load the andl file in \p name.
 * \p andl_context: The user context available when paring the andl file.
 * \p name: the name of the andl file to parse.
 * \return: 0 on success, 1 on failure.
 */
int
load_andl(andl_context_t *andl_context, const char *name)
{
    int res;
    FILE *f = fopen(name, "r");
    if (f == NULL) {
        warn("Could not open file '%s'", name);
        res = 1;
    } else {
        // initialize the lexer
        yyscan_t scanner;
        andl_lex_init(&scanner);
        // make the lexer read the file f
        andl_set_in(f, scanner);

        // zero the andl_context
        memset(andl_context, 0, sizeof(andl_context_t));

        // parse the andl file
        const int pres = andl_parse(scanner, andl_context);

        // destroy the lexer
        andl_lex_destroy(scanner);
        fclose(f);
        res = andl_context->error || pres;
    }

    return res;
}

/**
 * Initializes Sylvan. The number of lace workers will be automatically
 * detected. The size of the node table, and cache are set to sensible
 * defaults. We initialize the BDD package (not LDD, or MTBDD).
 */
void
init_sylvan()
{
    int n_workers = 0; // auto-detect
    lace_init(n_workers, 0);
    lace_startup(0, NULL, NULL);

    /* initialize the node table and cache with minimum size 2^20 entries, and
     * maximum 2^25 entries */
    sylvan_init_package(1LL<<20,1LL<<25,1LL<<20,1LL<<25);

    // initialize Sylvan's BDD sub system
    sylvan_init_bdd();
}

/**
 * Deinialize Sylvan. If Sylvan is compiled with
 * -DSYLVAN_STATS=ON, then statistics will be print,
 * such as the number of nodes in the node table.
 */
void
deinit_sylvan()
{
    sylvan_stats_report(stderr);
    sylvan_quit();
    lace_exit();
}

/**
 * Here you should implement whatever is required for the Software Science lab class.
 * \p andl_context: The user context that is used while parsing
 * the andl file.
 * The default implementation right now, is to print several
 * statistics of the parsed Petri net.
 */

void visualize_bdd(BDD bdd) {
    char b[256];
    snprintf(b, 256, "./BDD.dot");
    FILE *f = fopen(b, "w+");
    mtbdd_fprintdot(f, bdd);
    fclose(f);
}

// creates an ithvar OR and nithvar.
BDD create_var(Node* cur){
    LACE_ME;

    int name = cur->numPlace * 2;

    BDD var;
    if (cur->token == 1) {
        var = sylvan_ithvar(name);
    } else {
        var = sylvan_nithvar(name);
    }
    return var;
}

BDD create_prime_var(Node* cur, int places){
    LACE_ME;

    int name = cur->numPlace * 2 + 1;

    BDD var;
    if (cur->token == 1) {
        var = sylvan_nithvar(name);
    } else {
        var = sylvan_ithvar(name);
    }
    return var;
}

BDD prev(BDD* tran, BDD* set_p, BDD* map_p, BDD current, int current_index, int len_tran){
    LACE_ME;

    //rename current (map has to max from no prime to prime)
    sylvan_compose(current, map_p[current_index]);

    // E x'     (set has to be of type prime)
    BDD next_states = sylvan_true;
    for (int i = 0; i < len_tran; i++) {
        BDD r = sylvan_exists(tran[i], set_p[current_index]);
        next_states = sylvan_or(next_states, r);
        sylvan_relprev(tran[i], set_p[current_index], map_p[current_index]);
    }


    return next_states;
}

void check_formulas(BDD* tran, BDD* set, BDD* set_p, BDD* map, BDD* map_p, BDD initState, int len_tran) {

    BDD test = prev(tran, set_p, map_p, initState, 0, len_tran);
    visualize_bdd(test);
}

void
do_ss_things(andl_context_t *andl_context)
{
    LACE_ME;

    warn("The name of the Petri net is: %s", andl_context->name);
    warn("There are %d transitions", andl_context->num_transitions);
    warn("There are %d places", andl_context->num_places);
    warn("There are %d in arcs", andl_context->num_in_arcs);
    warn("There are %d out arcs", andl_context->num_out_arcs);

    // create initstate
    Node* cursor = andl_context->head;
    BDD initState = create_var(cursor);
    sylvan_protect(&initState);
    cursor = cursor->next;
    while(cursor != NULL) {
        initState = sylvan_and(create_var(cursor), initState);
        cursor = cursor->next;
    }

    BDD transitions[andl_context->num_transitions];
    BDD transitions_set[andl_context->num_transitions];
    BDD transitions_set_p[andl_context->num_transitions];
    BDD transitions_map[andl_context->num_transitions];
    BDD transitions_map_p[andl_context->num_transitions];

    TNode* t_cursor = andl_context->tHead;

    for (int i = 0; i < andl_context->num_transitions; i++) {

        CNode* c_cursor = t_cursor->conditions;

        BDD tran = sylvan_true;
        sylvan_protect(&tran);
        BDD set = sylvan_set_empty();
        sylvan_protect(&set);
        BDD set_p = sylvan_set_empty();
        sylvan_protect(&set_p);
        BDDMAP map = sylvan_map_empty();
        sylvan_protect(&map);
        BDDMAP map_p = sylvan_map_empty();
        sylvan_protect(&map_p);

        while (c_cursor != NULL) {

            //create condition and add it to the tran set.
            Node* node = search(andl_context->head, c_cursor->name);

            if (c_cursor->op) {
                node->token = 0;
            } else {
                node->token = 1;
            }

            BDD var = create_var(node);
            sylvan_protect(&var);
            BDD prime = create_prime_var(node, andl_context->num_places);
            sylvan_protect(&prime);

            BDD condition = sylvan_and(var, prime);
            tran = sylvan_and(tran, condition);

            //create the condition for the set.
            set = sylvan_set_add(set, node->numPlace * 2);
            set_p = sylvan_set_add(set_p, node->numPlace * 2 + 1);

            //create the condition for the map.
            map = sylvan_map_add(map, node->numPlace * 2 + 1, sylvan_ithvar(node->numPlace * 2));
            map_p = sylvan_map_add(map_p, sylvan_ithvar(node->numPlace * 2), node->numPlace * 2 + 1);

            if(debug) warn("cnode %d of %d (%s done)", node->numPlace, andl_context->num_places, c_cursor->name);

            c_cursor = c_cursor->next;
            sylvan_unprotect(&var);
            sylvan_unprotect(&prime);

        }

        transitions[i] = tran;
        transitions_set[i] = set;
        transitions_set_p[i] = set_p;
        transitions_map[i] = map;
        transitions_map_p[i] = map_p;

        if(debug) warn("%d of %d (%s DONE)", i+1, andl_context->num_transitions, t_cursor->name);

        t_cursor = t_cursor->next;

    }

    if(debug) warn("Done building transition BDDS");

    //    while
    BDD cur = initState;
    sylvan_protect(&cur);
    BDD vis = cur;
    sylvan_protect(&vis);

    do {
        vis = cur;
        for (int i = 0; i < andl_context->num_transitions; i = i + 1) {
            BDD r = sylvan_and(cur, transitions[i]);

            r = sylvan_exists(r, transitions_set[i]);
            r = sylvan_compose(r, transitions_map[i]);

            cur = sylvan_or(cur, r);
        }
    } while (cur != vis);

    if (debug) warn("Done building state space");

    //create result set
    BDD result = sylvan_set_empty();
    sylvan_protect(&result);
    cursor = andl_context->head;
    while (cursor != NULL) {
        result = sylvan_set_add(result, cursor->numPlace * 2);
        cursor = cursor->next;
    }

    if (debug) warn("Done result set for satcount");

    //print result
    warn("satcount of: %lf", sylvan_satcount(cur, result));
    warn("nodecount of: %lu", sylvan_nodecount(cur));

//    visualize_bdd(cur);
    check_formulas(transitions, transitions_set, transitions_set_p, transitions_map, transitions_map_p, initState, andl_context->num_transitions);

    sylvan_unprotect(&cur);
    sylvan_unprotect(&vis);
    sylvan_unprotect(&result);
}

/**
 * \brief An in-order parser of the given XML node.
 *
 * The default implementation is to print the temporal logic formula
 * on stderr.
 */
static int
parse_formula(xmlNode *node, Tree_node* parent)
{
    int res = 0;
    // first check if the node is not a NULL pointer.
    if (node == NULL) {
        res = 1;
        warn("Invalid XML");
    // only parse xml nodes, skip other parts of the XML file.
    } else if (node->type != XML_ELEMENT_NODE) {
        res = parse_formula(xmlNextElementSibling(node), parent);
    }
    // parse forAll
    else if (xmlStrcmp(node->name, (const xmlChar*) "all-paths") == 0) {
        parent = add_Tree_node(parent, "A");
        res = parse_formula(xmlFirstElementChild(node), parent);
    // parse Exists
    } else if (xmlStrcmp(node->name, (const xmlChar*) "exists-path") == 0) {
        parent = add_Tree_node(parent, "E");
        res = parse_formula(xmlFirstElementChild(node), parent);
    // parse Globally
    } else if (xmlStrcmp(node->name, (const xmlChar*) "globally") == 0) {
        parent = add_Tree_node(parent, "G");
        res = parse_formula(xmlFirstElementChild(node), parent);
    // parse Finally
    } else if (xmlStrcmp(node->name, (const xmlChar*) "finally") == 0) {
        parent = add_Tree_node(parent, "F");
        res = parse_formula(xmlFirstElementChild(node), parent);
    // parse neXt
    } else if (xmlStrcmp(node->name, (const xmlChar*) "next") == 0) {
        parent = add_Tree_node(parent, "X");
        res = parse_formula(xmlFirstElementChild(node), parent);
    // parse Until
    } else if (xmlStrcmp(node->name, (const xmlChar*) "until") == 0) {
        parent = add_Tree_node(parent, "U");
        res = parse_formula(xmlFirstElementChild(node), parent);
        res |= parse_formula(xmlNextElementSibling(xmlFirstElementChild(node)), parent);
    // parse before
    } else if (xmlStrcmp(node->name, (const xmlChar*) "before") == 0) {
        res = parse_formula(xmlFirstElementChild(node), parent);
    // parse reach
    } else if (xmlStrcmp(node->name, (const xmlChar*) "reach") == 0) {
        res = parse_formula(xmlFirstElementChild(node), parent);
    // parse negation
    } else if (xmlStrcmp(node->name, (const xmlChar*) "negation") == 0) {
        parent = add_Tree_node(parent, "!");
        res = parse_formula(xmlFirstElementChild(node), parent);
    // parse conjunction
    } else if (xmlStrcmp(node->name, (const xmlChar*) "conjunction") == 0) {
        parent = add_Tree_node(parent, "&&");
        res = parse_formula(xmlFirstElementChild(node), parent);
        res |= parse_formula(xmlNextElementSibling(xmlFirstElementChild(node)), parent);
    // parse disjunction
    } else if (xmlStrcmp(node->name, (const xmlChar*) "disjunction") == 0) {
        parent = add_Tree_node(parent, "||");
        res = parse_formula(xmlFirstElementChild(node), parent);
        res |= parse_formula(xmlNextElementSibling(xmlFirstElementChild(node)), parent);
    // parse is-fireable: atomic predicate!
    } else if (xmlStrcmp(node->name, (const xmlChar*) "is-fireable") == 0) {
        res = parse_formula(xmlFirstElementChild(node), parent);
    // parse transition (part of the atomic predicate)
    } else if (xmlStrcmp(node->name, (const xmlChar*) "transition") == 0) {
        L_node* data = NULL;
        for (xmlNode *transition = node; transition != NULL; transition = xmlNextElementSibling(transition)) {
            data = add_L_node(data, xmlNodeGetContent(transition));
            fprintf(stderr, "%s,", xmlNodeGetContent(transition));
        }
        add_Tree_data(parent, data);
    } else {
        res = 1;
        warn("Invalid xml node '%s'", node->name);
    }
    return res;
}

/**
 * \brief recursively parse the given XML node.
 */
static int
parse_xml(xmlNode *node)
{
    int res = 0;
    // first check if the node is not a NULL pointer.
    if (node == NULL) {
        res = 1;
        warn("Invalid XML");
    // only parse xml nodes, skip other parts of the XML file.
    } else if (node->type != XML_ELEMENT_NODE) res = parse_xml(xmlNextElementSibling(node));
    // parse property-set
    else if (xmlStrcmp(node->name, (const xmlChar*) "property-set") == 0) {
        // loop over all children that are property nodes
        for (xmlNode *property = xmlFirstElementChild(node);
                property != NULL && !res;
                property = xmlNextElementSibling(property)) {
            res = parse_xml(property);
        }
    // parse property
    } else if (xmlStrcmp(node->name, (const xmlChar*) "property") == 0) {
        warn("parsing property");
        res = parse_xml(xmlFirstElementChild(node));
    // parse id of property
    } else if (xmlStrcmp(node->name, (const xmlChar*) "id") == 0) {
        warn("Property id is: %s", xmlNodeGetContent(node));
        res = parse_xml(xmlNextElementSibling(node));
    // parse description of property
    } else if (xmlStrcmp(node->name, (const xmlChar*) "description") == 0) {
        warn("Property description is: %s", xmlNodeGetContent(node));
        res = parse_xml(xmlNextElementSibling(node));
    // parse the formula
    } else if (xmlStrcmp(node->name, (const xmlChar*) "formula") == 0) {
        warn("Parsing formula...");
        res = parse_formula(xmlFirstElementChild(node), NULL);
        printf("\n");
//        print_Tree_node(root, 0);
        add_tree_to_array();
        printf("\n");
    // node not recognized
    } else {
        res = 1;
        warn("Invalid xml node '%s'", node->name);
    }

    return res;
}

/**
 * \brief parses the XML file name.
 *
 * \returns 0 on success, 1 on failure.
 */
static int
load_xml(const char* name)
{
    int res;

    LIBXML_TEST_VERSION
    warn("parsing formulas file: %s", name);
    xmlDoc *doc = xmlReadFile(name, NULL, 0);
    if (doc == NULL) res = 1;
    else {
        xmlNode *node = xmlDocGetRootElement(doc);
        res = parse_xml(node);
    }

    return res;
}

Tree_node* check_formula(Tree_node* formula) {
    if (formula == NULL) {
        return NULL;
    }

    Tree_node* result = check_formula(formula->left);
    if (result != NULL) {
        formula->left = result;
    }
    result = check_formula(formula->right);
    if (result != NULL) {
        formula->right = result;
    }

    Tree_node* start = NULL;

//  replace EF
    if (strcmp(formula->data->symbol, "E") == 0) {
        char* symbol = formula->left->data->symbol;
        if (strcmp(symbol,"F") == 0) {
            start = add_Tree_node(NULL, "E");
            Tree_node* tree = add_Tree_node(start, "U");
            add_Tree_node(tree, "1");
            tree->right = formula->left->left;
        }
    }

    if (strcmp(formula->data->symbol, "A") == 0) {
        char *symbol = formula->left->data->symbol;

//      replace AX
        if (strcmp(symbol, "X") == 0) {
            start = add_Tree_node(NULL, "!");
            Tree_node *tree = add_Tree_node(start, "E");
            tree = add_Tree_node(tree, "X");
            tree = add_Tree_node(tree, "!");
            tree->left = formula->left->left;
//      replace AG
        } else if (strcmp(symbol, "G") == 0) {
            start = add_Tree_node(NULL, "!");
            Tree_node *tree = add_Tree_node(start, "E");
            tree = add_Tree_node(tree, "U");
            add_Tree_node(tree, "1");
            tree = add_Tree_node(tree, "!");
            tree->left = formula->left->left;
//      replace AF
        } else if (strcmp(symbol, "F") == 0) {
            start = add_Tree_node(NULL, "!");
            Tree_node* tree = add_Tree_node(start, "E");
            tree = add_Tree_node(tree, "G");
            tree = add_Tree_node(tree, "!");
            tree->left = formula->left->left;
//       replace AU
        } else if (strcmp(symbol, "U") == 0) {
            Tree_node* phi = formula->left->left;
            Tree_node* psi = formula->left->right;

            start = add_Tree_node(NULL, "!");
            Tree_node* tree = add_Tree_node(start, "||");
            Tree_node* left = add_Tree_node(tree, "E");
            Tree_node* right = add_Tree_node(tree, "E");

            //first right part
            tree = add_Tree_node(right, "G");
            tree = add_Tree_node(tree, "!");
            tree->left = psi;

            //first left part
            tree = add_Tree_node(left, "U");
            left = add_Tree_node(tree, "!");
            right = add_Tree_node(tree, "&&");

            //second left part
            left->left = psi;

            //second right part
            left = add_Tree_node(right, "!");
            right = add_Tree_node(right, "!");

            //third left part
            left->left = phi;

            //third right part
            right->left = psi;
        }
    }
    return start;
}

/**
 * \brief main. First parse the .andl file is parsed. And optionally parse the
 * XML file next.
 *
 * \returns 0 on success, 1 on failure.
 */
int main(int argc, char** argv)
{
    int res;
    if (argc >= 2) {
        andl_context_t andl_context;
        const char *name = argv[1];
        res = load_andl(&andl_context, name);
        if (res) warn("Unable to parse file '%s'", name);
        else {
            warn("Successful parse of file '%s' :)", name);
            if (argc == 3) {
                const char *formulas = argv[2];
                res = load_xml(formulas);

                for (int i = 0; i < 16; i++) {
                    Tree_node* result = check_formula(formula[i]);
                    if (result != NULL) {
                        formula[i] = result;
                    }
                }

//                print_Tree_node(formula[10],0);

                if (res) warn("Unable to load xml '%s'", formulas);
            }
            init_sylvan();
            // execute the main body of code
            do_ss_things(&andl_context);
            deinit_sylvan();
        }
    } else {
        warn("Usage: %s <petri-net>.andl [<CTL-formulas>.xml]", argv[0]);
        res = 1;
    }

    return res;
}

