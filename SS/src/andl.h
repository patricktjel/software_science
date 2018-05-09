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


//Crude linked list implementation
typedef struct node Node;
struct node {
    char* name;
    int token;
    int numPlace;
    Node *next;
};

Node* new_node(char* name, int token, int numPlace);

Node* add_node(Node* first, Node* toAdd);

Node* search(Node* head,char* data);

void printNode(Node* node);

//Condition node for a linked list
typedef struct cnode CNode;
struct cnode {
    char* name;
    int op;
    CNode *next;
};

//transition node for a linked list, with inside a linkedlist of nodition nodes.
typedef struct tnode TNode;
struct tnode {
    char* name;
    CNode* conditions;
    TNode* next;
};

TNode* createTNode(char* name);

TNode* addTNode(TNode* head, TNode* add);

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
