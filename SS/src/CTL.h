#ifndef SS_CTL_H
#define SS_CTL_H

typedef struct l_node L_node;
struct l_node {
    char* symbol;
    L_node *next;
};

/**
 * Creates an L (list) node for saving the "is fireable" predicates in the leaves of the tree
 *
 * @param symbol
 * @return
 */
L_node* create_L_node(char* symbol);

/**
 * Prepends an item to list of nodes
 * @param first
 * @param symbol
 * @return
 */
L_node* add_L_node(L_node* first, char* toAdd);

/**
 * @param first the first (root) element
 * @return the lenght of the list
 */
int length(L_node* first);

/**
 * Prints the list of nodes
 * @param node the node to start with
 */
void print_L_node(L_node* node);


//Tree for CTL formulas
typedef struct tree_node Tree_node;
struct tree_node {
    L_node *data;
    Tree_node *left;
    Tree_node *right;
};

/**
 * Creates a node for the tree (used for building a tree of CTL Formulas)
 * @param data
 * @return
 */
Tree_node* create_Tree_node(L_node *data);

/**
 *
 * @param parent the parent
 * @param to_add the char (E.g. A E && ||)
 * @return the created tree node
 */
Tree_node* add_Tree_node(Tree_node* parent, char* to_add);

/**
 * Adds a node to the tree
 * @param parent the parent to which this node should be a added
 * @param data the list if l nodes containing the "is firable"  predicates if the node is a leaf
 * @return the created node
 */
Tree_node* add_Tree_data(Tree_node* parent, L_node* data);

/**
 * Prints a visualisation of a node
 * @param node the root node
 * @param depth the depth of the node to print
 */
void print_Tree_node(Tree_node* node, int depth);

Tree_node* root;
Tree_node* formula[16];
void add_tree_to_array();

#endif //SS_CTL_H