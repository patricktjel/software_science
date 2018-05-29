#ifndef ANDL_H
#define ANDL_H

/**
 * Stores information while parsing .andl files.
 * Feel free to modify this file as you like.
 */

/**
 * \brief The direction of an arc:
 *  - ARC_IN is a place to transition arc,
 *  - ARC_OUT is a transition to place arc.
 */
typedef enum {
    ARC_IN,
    ARC_OUT,
} arc_dir_t;


//Struct for creating linked  list of places
typedef struct node Node;
struct node {
    char* name;
    int token;
    int numPlace;
    Node *next;
};

/**
 * Creates a new linked  list node representing a place
 * @param name the name of this plate
 * @param token the number of tokes this place has
 * @param numPlace the number (index) of this place (Staring at 1 instead of 0)
 * @return
 */
Node* new_node(char* name, int token, int numPlace);

/**
 * Prepends a node to the list of places
 * @param first The root node
 * @param toAdd the node to add
 * @return the added node (new root)
 */
Node* add_node(Node* first, Node* toAdd);

/**
 * Search for a node with the specified data
 * @param head the head of the list
 * @param data the data to search for
 * @return the node if found, NULL otherwise
 */
Node* search(Node* head,char* data);

/**
 * Recursively prints the list of nodes
 * @param node the node to start with
 */
void printNode(Node* node);

//Condition node for a linked list
typedef struct cnode CNode;
struct cnode {
    char* name;
    int op;
    CNode *next;
};

//transition node for a linked list, each tnode has a list of condition nodes.
typedef struct tnode TNode;
struct tnode {
    char* name;
    int number;
    CNode* conditions;
    TNode* next;
};

/**
 * Create a transition node
 * @param name the name of the tnode
 * @return the created tnode
 */
TNode* createTNode(char* name);

/**
 * Prepend a transition to the list of transitions
 * @param head the head of the list to add to
 * @param add the node to add
 * @return the newly added  (and now head) node
 */
TNode* addTNode(TNode* head, TNode* add);


TNode* search_function(TNode* head, char* function_name);

TNode* addCNode(TNode* node, char* name, int op);

void printTNode(TNode* node);

void printCNode(CNode* node);

/**
 * \brief A struct to store information while parsing
 * an andl file.
 */
typedef struct {
    // the name of the Petri net
    char *name;

    // the name of the current transition being parsed
    char *current_trans;

    // the number of places in the Petri net
    int num_places;

    // the number of transitions in the Petri net
    int num_transitions;

    // the number of place-transition arcs in the Petri net
    int num_in_arcs;

    // the number of transition-place arcs in the Petri net
    int num_out_arcs;

    // whether an error has occurred during parsing
    int error;

    //Random number
    Node* head;

    TNode* tHead;
} andl_context_t;

#endif
