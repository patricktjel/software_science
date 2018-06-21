//Simple if-else statement written in SSA code
public class Example {

    public void mul_to_even(int n1, int n2, int n10) {
        int n3 = n1 + n2 + n10;
        int n4 = 0;

        // n4
        if (n3 % 2 == 0) {
            int n4 = n3 * 3;
        } else {
            int n4 = n3 * 2;
        }
        assert n4 % 2 == 0;
    }
}

// expected result:  unsat