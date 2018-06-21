//Loop with correct invariant
public class Example {

    public void count_to_five(int n1, int n2) {
        int i = 0;
        while (i < 5) {
//           5-i; i >= 0 && i <= 5; i
            int i = i + 1;
        }
        assert i == 5;
    }
}

// expected result:  unsat unsat unsat unsat