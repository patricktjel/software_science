#Readme.md
Who: Lars Meijer(s2021749) and Patrick Thijssen(s2009609)  
How to run: our jar is packaged with a few examples. You can run those examples on Windows with the following commands;  
```Shell
java -jar ss-program-verification.jar Example_1.java
```
On Linux and Mac: (*See also instructions below*) **replace /path/to/lib**
```Shell
java -Djava.library.path=/path/to/lib -jar ss-program-verification.jar Example_1.java
```
 
The source code (Java maven project) can be found in ss_program_verification_source folder, the most important file is the main.java file.  
##What to do
Run the program using the commands above (and instructions below)  

##What do we support
It's Java code with a few additions/limitations;
  * Only the first method of a class file is parsed.
  * For every assignment you have to add it's type, so `i = 0` is not allowed, instead you should use `int i = 0`
  * There is support for the following binary operators:
  ```
  <=, <, >=, >, +, -, =, ==, !=, &&, ||, %, *
  ```
  * There is support for the following unary operators:
  ```
  !, assert
  ```
  * There is support for a while loop in the format of our second example.
    Note that the first line of the while body should be a comment stating the decreases variable,
    the invariant of the loop and the list of variables that change:
  ```
  while (i < 5) {
  //    i; i >= 0 && i <= 5; i, x
        int i = i + 1;
  }
  ```
  * In which `i < 5` is our condition. The comment consists of the following 3 parts:
    * `i` which is our decreasing variable
    * `i >= 0 && i <= 5` is our invariant
    * The last `i, x` is the list of (comma separated) variables.
  * i-- is not supported and should be replaced by i = i - 1 (same for i++), -y should be replaced by 0-y (see examples 8_1 ect.)  
  * Language features not mentioned in this readme are not supported.
  
## Installing Java bindings
Clone the z3 git repository.
Follow the instructions to install using make but **when running `python scripts/mk_make.py` add --java**, 
the instructions are located at https://github.com/Z3Prover/z3#building-z3-using-make-and-gccclang  

Next locate the file libz3java.so (On mac: libz3java.dylib ) (it is probably located in /usr/lib) and enter the folder path (so /path/to/lib and not /path/to/lib/libz3java.so)
 instead of /path/to/lib in the java command to run.


