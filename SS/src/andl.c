//
// Created by lars on 9-5-18.
//

#include <stddef.h>
#include <stdlib.h>
#include "andl.h"
#include <string.h>

Node* new_node(char* name, int token, int numPlace) {
    Node* node = malloc(sizeof(Node));
    node->name = name;
    node->token = token;
    node->numPlace = numPlace;
    node->next = NULL;

    return node;
}

Node* add_node(Node* first, Node* toAdd) {
    toAdd->next = first;
    return  toAdd;
}

Node* search(Node* head,char* name) {
    Node *cursor = head;
    while(cursor!=NULL)
    {
        if(strcmp(cursor->name,name) == 0)
            return cursor;
        cursor = cursor->next;
    }
    return NULL;
}

TNode* createTNode(char* name) {
    TNode* node = malloc(sizeof(TNode));
    node->name = name;
    node->conditions = NULL;
    node->next = NULL;

    return node;
}

TNode* addTNode(TNode* head, TNode* add) {
    add->next = head;
    return  add;
}

TNode* addCNode(TNode* tnode, char* name, int op) {
    CNode* node = malloc(sizeof(CNode));
    node->name = name;
    node->op = op;
    node->next = tnode->conditions;

    tnode->conditions = node;
    return tnode;
}

void printTNode(TNode* node) {
    printf("%s -> ", node->name);
    printCNode(node->conditions);

    if (node->next != NULL) {
        printTNode(node->next);
    }
}

void printCNode(CNode* node) {
    printf("%s %d-> ", node->name, node->op);

    if (node->next != NULL) {
        printCNode(node->next);
    } else {
        printf("\n");
    }
}