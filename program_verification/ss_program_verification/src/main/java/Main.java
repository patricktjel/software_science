import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.microsoft.z3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static Map<String, String> vars = new HashMap<>();
    private static List<String> asserts = new ArrayList<>();
    private static int path = 0;
    private static final ArrayList<Tree<String>> lines = new ArrayList<>();


    private static Map<String, Expr> z3Vars = new HashMap<>();

    //https://tomassetti.me/parsing-in-java/#javaLibraries
    public static void main(String[] args) throws IOException {
        CompilationUnit compilationUnit = JavaParser.parseResource("Example.java");
        MethodDeclaration method = (MethodDeclaration) compilationUnit.getChildNodes().get(0).getChildNodes().get(1);
        for (Parameter parameter : method.getParameters()) {
            vars.put(parameter.getNameAsString(), parameter.getTypeAsString());
        }
        BlockStmt code = (BlockStmt) method.getChildNodes().get(method.getChildNodes().size() - 1);

        parseMethodToSSA(code);
    }

    static void parseMethodToSSA(Node node) {
        //Set up initial Path variables
        vars.put("c_0", "Bool");
        for (Node child : node.getChildNodes()) {
            if (child instanceof ExpressionStmt) {
                parseExpression(child);
            } else if (child instanceof IfStmt) {
                parseITE((IfStmt) child);
            } else if (child instanceof AssertStmt) {
                parseAssert((AssertStmt) child);
            }
        }
        parseToZ3();
    }

    private static void parseAssert(AssertStmt node) {
        addAssertion("assert (" + "c_" + path + " && " + node.getCheck().toString() + ")");
        Tree<String> a = new Tree<>("assert");
        Tree<String> and = new Tree<>("&&");
        a.addLeftNode(and);
        and.addLeftNode(new Tree<>("c_" + path));
        and.addRightNode(parseBinExpression((BinaryExpr) node.getCheck()));
        a.print(0);
        lines.add(a);
    }

    private static void parseExpression(Node node) {
        //System.out.println("Expression .... + " + node.toString());
        //Expression Check if the path condition holds,
        VariableDeclarator decl = (VariableDeclarator) node.getChildNodes().get(0).getChildNodes().get(0);
        String name = decl.getNameAsString();
        String pathName = "c_" + path;
        vars.put(name, decl.getTypeAsString());

        Tree<String> tree = new Tree<>("=");
        tree.addLeftNode(new Tree<>(name));

        Tree<String> op = new Tree<>("?");
        tree.addRightNode(op);
        op.addLeftNode(new Tree<>(pathName));

        Tree<String> thenElse = new Tree<>(":");
        op.addRightNode(thenElse);
        thenElse.addRightNode(new Tree<>(name));
        if (decl.getInitializer().get() instanceof BinaryExpr) {
            thenElse.addLeftNode(parseBinExpression((BinaryExpr) decl.getInitializer().get()));
        } else {
            thenElse.addLeftNode(new Tree<>(decl.getInitializer().get().toString()));
        }

        lines.add(tree);
        tree.print(0);
        // This has the form of var_name = path_con ? decl : var_name
        addAssertion(name + " = " + pathName + " ? " + decl.getInitializer().get().toString() + " : " + name);
    }

    private static void parseITE(IfStmt node) {
        // If statements
        String ifPath = "c_" + (path + 1);
        String elsePath = "c_" + (path + 2);
        String oldPath = "c_" + path;
        vars.put(ifPath, "Bool");
        vars.put(elsePath, "Bool");

        //If condition
        addAssertion(ifPath + " = " + oldPath + " && (" + ((node).getCondition().toString() + ")"));

        BinaryExpr con = (BinaryExpr) node.getCondition();

        //Build a tree
        Tree<String> tree = new Tree<>("=");
        tree.addLeftNode(new Tree<>(ifPath));
        Tree<String> and = new Tree<>("&&");
        tree.addRightNode(and);
        and.addLeftNode(new Tree<>(oldPath));


        Tree<String> iteTree = parseBinExpression(con);
        //tree.addLeftNode();
        //If body

        and.addRightNode(iteTree);
        tree.print(0);
        lines.add(tree);
        path++;
        parseExpression((node).getThenStmt().getChildNodes().get(0));
        //Else statement

        Tree<String> elseTree = new Tree<>("=");
        elseTree.addLeftNode(new Tree<>(elsePath));

        Tree<String> conjunction = new Tree<>("&&");
        elseTree.addRightNode(conjunction);
        conjunction.addLeftNode(new Tree<>(oldPath));

        Tree<String> negation = new Tree<>("!");
        conjunction.addRightNode(negation);
        negation.addLeftNode(new Tree<>(ifPath));

        lines.add(elseTree);
        path++;

        //Only print an if body if the if body is present
        if (node.getElseStmt().isPresent()) {
            parseExpression((node).getElseStmt().get().getChildNodes().get(0));
        }

        path++;
        addAssertion("c_" + path + " = " + ifPath + " || " + elsePath);
        Tree<String> endIf = new Tree<>("=");
        endIf.addLeftNode(new Tree<>("c_" + path));
        Tree<String> disjunction = new Tree<>("||");
        endIf.addRightNode(disjunction);
        disjunction.addLeftNode(new Tree<>(ifPath));
        disjunction.addRightNode(new Tree<>(elsePath));
        vars.put("c_" + path, "Bool");
    }

    private static void addAssertion(String assertion) {
        System.out.println(assertion);
        asserts.add(assertion);
    }

    private static Tree<String> parseBinExpression(BinaryExpr node) {
        Tree<String> root =  new Tree<>(node.getOperator().asString());
        //Check left node
        if (node.getLeft() instanceof BinaryExpr) {
            root.addLeftNode(parseBinExpression((BinaryExpr) node.getLeft()));
        } else {
            root.addLeftNode(new Tree<>(node.getLeft().toString()));
        }

        //Check right node
        if (node.getRight() instanceof BinaryExpr) {
            root.addRightNode(parseBinExpression((BinaryExpr) node.getRight()));
        } else {
            root.addRightNode(new Tree<>(node.getRight().toString()));
        }
        return root;
    }

    private static void parseToZ3 () {
        Context ctx = new Context();

        // first declare all (path)variables
        vars.forEach((k, v) -> {
            Expr expr = null;
            if (v.toLowerCase().equals("int")) {
                expr = ctx.mkIntConst(k);
            } else if (v.toLowerCase().equals("bool")) {
                expr = ctx.mkBoolConst(k);
            }
            System.out.println(expr.getFuncDecl());
            z3Vars.put(k, expr);
        });

        // set default path condition value
        printAssert(ctx.mkEq(z3Vars.get("c_0"), ctx.mkBool(true)));

        lines.forEach(tree -> {
            Expr expr = parseSSATree(tree, ctx);
            printAssert(expr);
        });

        ctx.close();
    }

    private static void printAssert(Expr smt2) {
        System.out.println("(assert " + smt2 + ")");
    }

    private static Expr parseSSATree(Tree<String> tree, Context ctx) {
        // base case; it's a leave
        if (tree.getLeft() == null && tree.getRight() == null) {
            if (z3Vars.containsKey(tree.getData())) {
                return z3Vars.get(tree.getData());
            } else {
                return ctx.mkInt(tree.getData());
            }
        }

        // else parse it's leaves
        switch (tree.getData().toLowerCase()) {
            case "assert":
                return parseSSATree(tree.getLeft(), ctx);
            case "==":
            case "=":
                return ctx.mkEq(parseSSATree(tree.getLeft(), ctx),
                        parseSSATree(tree.getRight(), ctx));
            case "?":
                return ctx.mkITE((BoolExpr) parseSSATree(tree.getLeft(), ctx),
                        parseSSATree(tree.getRight().getLeft(), ctx),
                        parseSSATree(tree.getRight().getRight(), ctx));
            case "&&":
                return ctx.mkAnd((BoolExpr) parseSSATree(tree.getLeft(), ctx),
                        (BoolExpr) parseSSATree(tree.getRight(), ctx));
            case "||":
                return ctx.mkOr((BoolExpr) parseSSATree(tree.getLeft(), ctx),
                        (BoolExpr) parseSSATree(tree.getRight(), ctx));
            case "%":
                return ctx.mkMod((IntExpr)parseSSATree(tree.getLeft(), ctx),
                        (IntExpr) parseSSATree(tree.getRight(),ctx));
            case "+":
                return ctx.mkAdd((ArithExpr)parseSSATree(tree.getLeft(), ctx),
                        (ArithExpr)parseSSATree(tree.getRight(), ctx));
            case "-":
                return ctx.mkSub((ArithExpr)parseSSATree(tree.getLeft(), ctx),
                        (ArithExpr)parseSSATree(tree.getRight(), ctx));
            case "*":
                return ctx.mkMul((ArithExpr)parseSSATree(tree.getLeft(), ctx),
                        (ArithExpr)parseSSATree(tree.getRight(), ctx));
            default:
                return null;
        }
    }
}
