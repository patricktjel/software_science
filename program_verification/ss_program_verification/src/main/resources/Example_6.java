//Empty if body
public class Example {

    public void mul_to_even(int n1, int n2, int n10) {
        int n3 = n1 + n2 + n10;
        int n4 = 0;
        int n5 = 0;
        int n5 = 2;
        if (n3 % 2 == 0) {
            int n4 = n3 * 3;
            int n5 = n4 * 2;
        }

        assert n5 % 2 != 0;
    }
}

// expected result: sat (unsat if n5 % 2 == 0)