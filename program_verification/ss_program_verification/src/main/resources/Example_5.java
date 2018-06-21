//Multi line if-else body without changing variables in the body (should be sat instead of unsat)
public class Example {

    public void mul_to_even(int n1, int n2, int n10) {
        int n3 = n1 + n2 + n10;

        if (n3 % 2 == 0) {
            int n4 = n3 * 3;
            int n5 = n4 * 2;
        } else {
            int n4 = n3 * 2;
            int n5 = n4 * 2;
        }
        assert n5 % 2 == 0;
    }
}

// expected result: sat (n5 does not exist outside of the scope)