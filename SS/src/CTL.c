#include "CTL.h"
#include <stdlib.h>
#include <stdio.h>

/**
 * Implementation of the methods in CTL.h
 * See CTL.h for further documentation
 */

L_node* create_L_node(char* symbol) {
    L_node* node = malloc(sizeof(L_node));
    node->symbol = symbol;
    node->next = NULL;

    return node;
}

L_node* add_L_node(L_node* first, char* symbol) {
    L_node* node = create_L_node(symbol);
    node->next = first;
    return node;
}

int length(L_node* first) {
    int length = 0;
    while (first != NULL) {
        length = length + 1;
        first = first->next;
    }
    return length;
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
    L_node* node = create_L_node(to_add);

    return add_Tree_data(parent, node);
}

Tree_node* add_Tree_data(Tree_node* parent, L_node* data) {
    Tree_node* node = create_Tree_node(data);

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

    return node;
}

void print_Tree_node(Tree_node* node, int depth) {
    if (node == NULL) {
        return;
    }
    for (int i = 0; i < depth; i++){
        printf("\t");
    }
    print_L_node(node->data);
    printf("\n");

    print_Tree_node(node -> left, depth + 1);
    print_Tree_node(node ->right, depth + 1);
    if ( node->left  != NULL && (node->left->left != NULL && node->left->right != NULL) ||
        node->right != NULL && (node->right->left != NULL && node->right->right != NULL)) {
        printf("\n");
    }
}

int formula_index = 0;

void add_tree_to_array() {
    formula[formula_index] = root;
    formula_index = formula_index + 1;
}