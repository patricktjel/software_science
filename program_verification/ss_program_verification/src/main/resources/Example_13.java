//Loop with correct invariant
public class Example {

    // y >= 0 (Required not process, need to manually add  (assert (= c_0 (y_0 > 0)))
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

// expected result:  unsatg unsat unsat unsat