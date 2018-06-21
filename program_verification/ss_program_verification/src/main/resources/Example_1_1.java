//Example 1 without SSA java
public class Example {

    public void mul_to_even(int n1, int n2, int n10) {
        int n = n1 + n2 + n10;

        if (n % 2 == 0) {
            int n = n * 3;
        } else {
            int n = n * 2;
        }
        assert n % 2 == 0;
    }
}
// expected result: unsat