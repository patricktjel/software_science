public class Example {

    public int mul_to_even(int n1, int n2) {
        int n3 = n1 + n2;
        if (n3 % 2 == 0) {
            n4 = n3 * 3;
        } else {
            n4 = n3 * 2;
        }
        assert n4 % 2 == 0;
        return n4;
    }
}
