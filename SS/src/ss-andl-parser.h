/* A Bison parser, made by GNU Bison 3.0.4.  */

/* Bison interface for Yacc-like parsers in C

   Copyright (C) 1984, 1989-1990, 2000-2015 Free Software Foundation, Inc.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

/* As a special exception, you may create a larger work that contains
   part or all of the Bison parser skeleton and distribute that work
   under terms of your choice, so long as that work isn't itself a
   parser generator using the skeleton or a modified version thereof
   as a parser skeleton.  Alternatively, if you modify or redistribute
   the parser skeleton itself, you may (at your option) remove this
   special exception, which will cause the skeleton and the resulting
   Bison output files to be licensed under the GNU General Public
   License without this special exception.

   This special exception was added by the Free Software Foundation in
   version 2.2 of Bison.  */

#ifndef YY_ANDL_SS_ANDL_PARSER_H_INCLUDED
# define YY_ANDL_SS_ANDL_PARSER_H_INCLUDED
/* Debug traces.  */
#ifndef ANDL_DEBUG
# if defined YYDEBUG
#if YYDEBUG
#   define ANDL_DEBUG 1
#  else
#   define ANDL_DEBUG 0
#  endif
# else /* ! defined YYDEBUG */
#  define ANDL_DEBUG 0
# endif /* ! defined YYDEBUG */
#endif  /* ! defined ANDL_DEBUG */
#if ANDL_DEBUG
extern int andl_debug;
#endif
/* "%code requires" blocks.  */
#line 20 "andl-parser.y" /* yacc.c:1909  */

#include <config.h>
#include <stdio.h>
#include <andl.h>
#ifdef YYDEBUG
#undef YYDEBUG
#define YYDEBUG 1
#endif

#line 62 "ss-andl-parser.h" /* yacc.c:1909  */

/* Token type.  */
#ifndef ANDL_TOKENTYPE
# define ANDL_TOKENTYPE
  enum andl_tokentype
  {
    PN = 258,
    IDENT = 259,
    LBRAC = 260,
    RBRAC = 261,
    LCURLY = 262,
    RCURLY = 263,
    COLON = 264,
    CONSTANTS = 265,
    PLACES = 266,
    DISCRETE = 267,
    SEMICOLON = 268,
    ASSIGN = 269,
    NUMBER = 270,
    PLUS = 271,
    MIN = 272,
    AMP = 273,
    TRANSITIONS = 274
  };
#endif

/* Value type.  */
#if ! defined ANDL_STYPE && ! defined ANDL_STYPE_IS_DECLARED

union ANDL_STYPE
{
#line 45 "andl-parser.y" /* yacc.c:1909  */

    char *text;
    int number;
    arc_dir_t dir;

#line 100 "ss-andl-parser.h" /* yacc.c:1909  */
};

typedef union ANDL_STYPE ANDL_STYPE;
# define ANDL_STYPE_IS_TRIVIAL 1
# define ANDL_STYPE_IS_DECLARED 1
#endif

/* Location type.  */
#if ! defined ANDL_LTYPE && ! defined ANDL_LTYPE_IS_DECLARED
typedef struct ANDL_LTYPE ANDL_LTYPE;
struct ANDL_LTYPE
{
  int first_line;
  int first_column;
  int last_line;
  int last_column;
};
# define ANDL_LTYPE_IS_DECLARED 1
# define ANDL_LTYPE_IS_TRIVIAL 1
#endif



int andl_parse (void *scanner, andl_context_t *andl_context);

#endif /* !YY_ANDL_SS_ANDL_PARSER_H_INCLUDED  */
