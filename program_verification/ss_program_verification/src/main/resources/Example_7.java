//Empty if body
public class Example {

    public void swap(int x, int y) {
        int tmp = x;

        if (tmp > y) {
            int x = y;
        } else {
            int tmp = y;
        }
        int y = tmp;
    }
}

// expected result: just runnable