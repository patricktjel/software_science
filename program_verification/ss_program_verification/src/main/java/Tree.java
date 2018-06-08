public class Tree<T> {
    private T data;
    private Tree<T> left;
    private  Tree<T> right;

    public Tree(T data) {
        this.data = data;
    }

    public void addLeftNode(Tree<T> toAdd) {
        this.left = toAdd;
    }

    public void addRightNode(Tree<T> toAdd) {
        this.right = toAdd;
    }

    public void print(int depth) {

        for (int i = 0; i < depth; i++) {
            System.out.print("\t");
        }
        System.out.println(data);

        if (left != null) {
            left.print(depth + 1);
        }
        if (right != null) {
            right.print(depth + 1);
        }
    }
}
