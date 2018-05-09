//
// Created by lars on 9-5-18.
//

#include <stddef.h>
#include <stdlib.h>
#include "andl.h"
#include <string.h>

Node* new_node(char* name, int token, int numPlace) {
    Node* node = malloc(sizeof(node));

    char f[40];
    strcpy(f, name);

    node->name = f;
    node->token = token;
    node->numPlace = numPlace;
    node->next = NULL;

    return node;
}

Node* add_node(Node* first, Node* toAdd) {
    //toAdd->next = first;
    return  toAdd;
}