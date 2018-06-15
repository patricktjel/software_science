/**
 * This tree represents SSA code (in order)
 * each line of SSA code should have its own tree
 *
 * @param <T> Generic type of the tree
 */
public class Tree<T> {
    private T data;
    private Tree<T> left;
    private Tree<T> right;

    public Tree(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public Tree<T> getLeft() {
        return left;
    }

    public Tree<T> getRight() {
        return right;
    }

    /**
     * Adds a left node to this tree
     * @param toAdd the tree to add
     */
    public void addLeftNode(Tree<T> toAdd) {
        if (left != null) {
            throw new IllegalArgumentException("Tree already has a left node");
        }
        this.left = toAdd;
    }

    /**
     * Adds a right node to this tree
     * @param toAdd the tree to add
     */
    public void addRightNode(Tree<T> toAdd) {
        if (right != null) {
            throw new IllegalArgumentException("Tree already has a right node");
        }
        this.right = toAdd;
    }

    /**
     * Prints a visual representation of the tree for debug purposes
     * @param depth the depth (number of tabs)
     */
    public void printDebug(int depth) {

        for (int i = 0; i < depth; i++) {
            System.out.print("\t");
        }
        System.out.println(data);

        if (left != null) {
            left.printDebug(depth + 1);
        }
        if (right != null) {
            right.printDebug(depth + 1);
        }
    }

    /**
     * Prints the tree in order (in oder to generate SSA code from the tree
     * @param s self node to print
     */
    public static void printInOrder(Tree<String> s) {
        if (s == null) {
            return;
        }
        printInOrder(s.left);
        System.out.print(s.getData() + " ");
        printInOrder(s.right);
    }
}
