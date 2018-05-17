#ifndef SS_CTL_H
#define SS_CTL_H

typedef struct l_node L_node;
struct l_node {
    char* symbol;
    L_node *next;
};

L_node* create_L_node(char* symbol);

L_node* add_L_node(L_node* first, char* toAdd);

void print_L_node(L_node* node);

typedef struct tree_node Tree_node;
struct tree_node {
    L_node *data;
    Tree_node *left;
    Tree_node *right;
};

Tree_node* create_Tree_node(L_node *data);

Tree_node* add_Tree_node(Tree_node* parent, char* to_add);

void print_Tree_node(Tree_node* node);

Tree_node* root;

#endif //SS_CTL_H