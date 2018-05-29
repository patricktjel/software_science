//
// Created by lars on 9-5-18.
//

#include <stddef.h>
#include <stdlib.h>
#include "andl.h"
#include <string.h>
#include <stdio.h>

/**
 * Implementation of the methods in andl.h
 * See andl.h for further details
 */

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
    Node* cursor = head;
    while(cursor!=NULL)
    {
        if(strcmp(cursor->name,name) == 0)
            return cursor;
        cursor = cursor->next;
    }
    return NULL;
}

void printNode(Node* node) {
    printf("%s, %d, %d -> ", node->name, node->token, node->numPlace);
    if(node->next != NULL) {
        printNode(node->next);
    }
}

TNode* createTNode(char* name) {
    TNode* node = malloc(sizeof(TNode));
    node->name = name;
    node->conditions = NULL;
    node->next = NULL;
    node->number = 0;

    return node;
}

TNode* addTNode(TNode* head, TNode* add) {
    add->next = head;
    add->number =  (head != NULL) ? head->number + 1 : 0;
    return  add;
}

TNode* search_function(TNode* head, char* function_name) {
    TNode* cursor = head;
    while(cursor!=NULL)
    {
        if(strcmp(cursor->name,function_name) == 0)
            return cursor;
        cursor = cursor->next;
    }
    return NULL;
}

TNode* addCNode(TNode* tnode, char* name, int op) {
    CNode* prev = NULL;
    CNode* condition = tnode->conditions;

    while (condition != NULL) {
        if (strcmp(condition->name, name) == 0) {
           if (prev == NULL) {
               tnode->conditions = condition->next;
               return tnode;
           } else {
               prev->next = condition->next;
               return tnode;
           }
        }
        prev = condition;
        condition = condition->next;
    }


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