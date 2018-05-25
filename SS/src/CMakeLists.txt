#project(SS)
#
#set(CMAKE_CXX_STANDARD 11)
#
#find_library(SYLVAN_LIB sylvan)
#find_package(LibXml2 REQUIRED)
#find_package(BISON REQUIRED)
#find_package(FLEX REQUIRED)
#
#include_directories(. ${LIBXML2_INCLUDE_DIR} ${CMAKE_CURRENT_BINARY_DIR} ${FLEX_INCLUDE_DIRS})
#
#file(GLOB XML2_HEADERS ${LIBXML2_INCLUDE_DIR}/libxml/*.h)
#
#BISON_TARGET(PARSER andl-parser.y ${CMAKE_CURRENT_BINARY_DIR}/andl-parser.c)
#FLEX_TARGET(LEXER andl-lexer.l ${CMAKE_CURRENT_BINARY_DIR}/andl-lexer.c DEFINES_FILE ${CMAKE_CURRENT_BINARY_DIR}/andl-lexer.h)
#ADD_FLEX_BISON_DEPENDENCY(LEXER PARSER)
#
##file(GLOB EXTRA_HEADERS ${CMAKE_CURRENT_BINARY_DIR}/*.hh)
#
#add_executable(
#        SS
#        ss.c
#        config.h
#        ${XML2_HEADERS}
#        andl.h
#        ${FLEX_LEXER_OUTPUTS}
#        ${BISON_PARSER_OUTPUTS}
#        ${EXTRA_HEADERS}
#        ${FLEX_INCLUDE_DIR}/FlexLexer.h
#        util.c
#        util.h
#)
#
#target_link_libraries(
#        SS
#        "${SYLVAN_LIB}"
#        ${LIBXML2_LIBRARIES}
#        ${FLEX_LIBRARIES}
#        pthread
#        hwloc
#)