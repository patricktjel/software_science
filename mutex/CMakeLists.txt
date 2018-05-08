cmake_minimum_required(VERSION 3.10)
project(mutex C)

set(CMAKE_C_STANDARD 11)

add_executable(mutex main.c LinkedList.cpp LinkedList.c)