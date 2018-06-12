#Readme.md
Who: Lars Meijer(s2021749) and Patrick Thijssen(s2009609)  
How to run: our jar is packaged with 2 examples. You can run those examples with the following commands;  
```
java -jar ss-program-verification.jar Example_1.jar
java -jar ss-program-verification.jar Example_2.jar
```
  * The first example is an example that always returns an even number. (Which is the same code as last week)
  * The second example is with a while loop which counts to five.  
  * Another option is to run our program with your own file.
 
What to do: see the file Main.java  
What to inspect: if our output is right.

##What do we support
It's Java code with a few additions/limitations;
  * Only the first method of a class file is parsed.
  * For every assignment you have to add it's type, so `i = 0`is not allowed, instead you should use `int i = 0`
  * The code has to be in Static Single Assignment format. 
  * There is support for the following binary operators:
  ```
  <=, <, >=, >, +, -, =, ==, !=, &&, ||, %, *, ? :
  ```
  * There is support for the following unary operators:
  ```
  !, assert
  ```
  * There is support for a while loop in the format of our second example:
  ```
          while (i < 5) {
  //            i >= 0 && i <= 5
              int i = i + 1;
          }
  ```
  * In which `i < 5` is our condition. the comment `i >=0 && i <= 5` is our invariant
  and `int i = i + 1` is our body.
  * Language features not mentioned in this readme are not supported.