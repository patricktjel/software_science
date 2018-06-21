//These are the loops based on the examples given in class (i-- has been replaced by i = i - 1, -i is replaced by 0 - 1 to ease our parsing)
public class Example {

    // y >= 0
    public void count_to_five(int y) {
        int x = 0;
        int i = y;
        while (i > 0) {
//           0 - i; i*2 + x == y*2 && i >= 0; i
            int i = i - 1;
            int x = x + 2;
        }
        assert x == 2 * y;
    }
}

// expected result:  unsat unsat sat unsat