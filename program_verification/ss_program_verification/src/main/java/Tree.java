import javax.xml.crypto.Data;

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
}
