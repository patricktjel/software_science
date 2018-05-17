#include "CTL.h"
#include <stdlib.h>
#include <stdio.h>


L_node* create_L_node(char* symbol) {
    L_node* node = malloc(sizeof(L_node));
    node->symbol = symbol;
    node->next = NULL;

    return node;
}

L_node* add_L_node(L_node* first, char* symbol) {
    L_node* node = create_L_node(symbol);
    node->next = first;
    return  node;
}

Tree_node* create_Tree_node(L_node *data) {
    Tree_node* node = malloc(sizeof(Tree_node));
    node->data = data;
    node->left = NULL;
    node->right = NULL;

    return node;
}

void print_L_node(L_node* node) {
    printf("%s -> ", node->symbol);
    if(node->next != NULL) {
        print_L_node(node->next);
    }
}

Tree_node* add_Tree_node(Tree_node* parent, char* to_add) {
    Tree_node* node = create_Tree_node(create_L_node(to_add));

    // if there is no parent yet, we just created the root.
    if (parent == NULL) {
        root = node;
        return node;
    }

    if (parent->left == NULL) {
        parent->left = node;
    } else { //right == NULL
        parent->right = node;
    }

    return parent;
}



void print_Tree_node(Tree_node* node) {
    if (node == NULL) {
        return;
    }
    print_L_node(node->data);
    printf("\n");
    print_Tree_node(node -> left);
    printf("\n");
    print_Tree_node(node ->right);
    printf("\n");
}