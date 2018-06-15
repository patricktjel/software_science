#Readme.md
Who: Lars Meijer(s2021749) and Patrick Thijssen(s2009609)  
How to run: our jar is packaged with a few examples. You can run those enxamples on Windows with the following commands;  
```Shell
java -jar ss-program-verification.jar Example_1.jar
java -jar ss-program-verification.jar Example_2.jar
java -jar ss-program-verification.jar Example_3.jar
```
On Linux: (See also instructions below) **replace /path/to/lib**
```Shell
java -Djava.library.path=/path/to/lib -jar ss-program-verification.jar Example_1.java
```

  * The first example is an example that always returns an even number. (Which is the same code as last week)
  * The second example is with a while loop which counts to five.  
  * Another option is to run our program with your own file.
 
The source code (Java maven project) can be found in ss_program_verification_source.rar, the most important file is the main.java file.  
##What to do
Run the program using the commands above (and instructions below)  
##What to inspect/questions
Wether our output is right, and especially of the output for (basic) loops is correct.  
We would also like to know how complex the programs that  we have to be able to handle can be, right now there are still some serious limitations.  
Lastly, can you run the jar using the provided instructions?  

##What do we support
It's Java code with a few additions/limitations;
  * Only the first method of a class file is parsed.
  * For every assignment you have to add it's type, so `i = 0`is not allowed, instead you should use `int i = 0`
  * If/then/while bodies can only have a single line of code.
  * There is support for the following binary operators:
  ```
  <=, <, >=, >, +, -, =, ==, !=, &&, ||, %, *
  ```
  * There is support for the following unary operators:
  ```
  !, assert
  ```
  * There is support for a while loop in the format of our second example:
  ```
          while (i < 5) {
  //            i >= 0 && i <= 5; i
              int i = i + 1;
          }
  ```
  * In which `i < 5` is our condition. the comment `i >=0 && i <= 5` is our invariant
  and `int i = i + 1` is our body. Note that the first line of the while body should be a comment stating an invariant of the loop and the variables that change. 
  * There only support for terminating loops (e.g. there is no 'decreases')
  * Language features not mentioned in this readme are not supported.
  
## Installing Java bindings
Clone the z3 git repository.
Follow the instructions to install using make but **when running `python scripts/mk_make.py` add --java**, 
the instructions are located at https://github.com/Z3Prover/z3#building-z3-using-make-and-gccclang  

Next locate the file libz3java.so (On mac: libz3java.dylib ) (it is probably located in /usr/lib) and enter the folder path (so /path/to/lib and not /path/to/lib/libz3java.so)
 instead of /path/to/lib in the java command to run.


