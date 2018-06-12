import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.microsoft.z3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static Map<String, String> vars = new HashMap<>();
    private static int path = 0;
    private static final ArrayList<Tree<String>> lines = new ArrayList<>();

    private static Map<String, Expr> z3Vars = new HashMap<>();


    /**
     * The first method gets parsed.
     *
     * Parsing based on https://tomassetti.me/parsing-in-java/#javaLibraries
     */
    public static void main(String[] args) throws IOException {
        CompilationUnit compilationUnit = JavaParser.parseResource(args[0]);
        MethodDeclaration method = (MethodDeclaration) compilationUnit.getChildNodes().get(0).getChildNodes().get(1);
        for (Parameter parameter : method.getParameters()) {
            vars.put(parameter.getNameAsString(), parameter.getTypeAsString());
        }
        BlockStmt code = (BlockStmt) method.getChildNodes().get(method.getChildNodes().size() - 1);

        parseMethodToSSA(code);
    }

    /**
     * Parses the content of a method
     *
     * @param node the node containing the method
     */
    static void parseMethodToSSA(Node node) {
        //Set up initial Path variables
        vars.put("c_0", "Bool");
        for (Node child : node.getChildNodes()) {
            if (child instanceof ExpressionStmt) {
                Tree<String> tree = parseExpression(child);
                tree.print(0);
                lines.add(tree);
            } else if (child instanceof IfStmt) {
                parseITE((IfStmt) child);
            } else if (child instanceof AssertStmt) {
                parseAssert((AssertStmt) child);
            } else if (child instanceof WhileStmt) {
                parseWhile((WhileStmt) child);
            }
        }
        parseToZ3();
    }

    private static void parseWhile(WhileStmt node) {
        // invariant & condition should hold
        {
            String inv = node.getChildNodes().get(1).getChildNodes().get(0).getComment().get().getContent();
            Expression parsed_inv = JavaParser.parseExpression(inv);
            Tree<String> invariant = parseBinExpression((BinaryExpr) parsed_inv);
            Tree<String> condition = parseBinExpression((BinaryExpr) node.getCondition());
            Tree<String> tree = new Tree<>("assertinv");
            tree.addLeftNode(new Tree<>("&&"));
            tree.getLeft().addLeftNode(invariant);
            tree.getLeft().addRightNode(condition);
            System.out.println("(assertinv (" + parsed_inv + " && " + node.getCondition() + "))");
            tree.print(0);
            lines.add(tree);
        }
        // the body
        {
            Node expr = node.getChildNodes().get(1).getChildNodes().get(0);
            Tree<String> body = parseExpression(expr);
            System.out.println(expr.getChildNodes().get(0));
            body.print(0);
            lines.add(body);
        }
        //afterwards the invariant should still hold
        {
            String inv = node.getChildNodes().get(1).getChildNodes().get(0).getComment().get().getContent();
            Expression parsed_inv = JavaParser.parseExpression(inv);
            Tree<String> invariant = new Tree<>("assertinv");
            invariant.addLeftNode(parseBinExpression((BinaryExpr) parsed_inv));
            System.out.println("(assertinv (" + parsed_inv + ") )");
            invariant.print(0);
            lines.add(invariant);
        }
    }

    /**
     * Parse a statement in the form of 'assert ... '
     * @param node The node containing the assert statement
     */
    private static void parseAssert(AssertStmt node) {
        Tree<String> a = new Tree<>("assert");
        Tree<String> and = new Tree<>("&&");
        a.addLeftNode(and);
        and.addLeftNode(new Tree<>("c_" + path));
        and.addRightNode(parseBinExpression((BinaryExpr) node.getCheck()));
        a.print(0);
        lines.add(a);
    }

    /**
     * Parses an expression and
     * @param node
     */
    private static Tree<String> parseExpression(Node node) {
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

        return tree;
    }

    private static void parseITE(IfStmt node) {
        // If statements
        String ifPath = "c_" + (path + 1);
        String elsePath = "c_" + (path + 2);
        String oldPath = "c_" + path;
        vars.put(ifPath, "Bool");
        vars.put(elsePath, "Bool");


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
        tree.print(0);
        path++;
        Tree<String> expTree = parseExpression((node).getThenStmt().getChildNodes().get(0));
        tree.print(0);
        lines.add(expTree);
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
        elseTree.print(0);
        path++;

        //Only print an if body if the if body is present
        if (node.getElseStmt().isPresent()) {
           Tree expr = parseExpression((node).getElseStmt().get().getChildNodes().get(0));
           lines.add(expr);
        }

        path++;
        Tree<String> endIf = new Tree<>("=");
        endIf.addLeftNode(new Tree<>("c_" + path));
        Tree<String> disjunction = new Tree<>("||");
        endIf.addRightNode(disjunction);
        disjunction.addLeftNode(new Tree<>(ifPath));
        disjunction.addRightNode(new Tree<>(elsePath));
        endIf.print(0);
        lines.add(endIf);
        vars.put("c_" + path, "Bool");
    }

    /**
     * Recursively parse a binary expression and builds the tree representing this expression
     * @param node the node
     * @return the tree build for this expression
     */
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
        if (smt2 != null) {
            System.out.println("(assert " + smt2 + ")");
        }
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
                System.out.println("(push)");
                BoolExpr boolExpr = ctx.mkAnd((BoolExpr) parseSSATree(tree.getLeft().getLeft(), ctx),
                        ctx.mkNot((BoolExpr) parseSSATree(tree.getLeft().getRight(), ctx)));
                printAssert(boolExpr);
                System.out.println("(check-sat)");
                System.out.println("(pop)");
                return null;
            case "assertinv":
                System.out.println("(push)");
                BoolExpr expr = ctx.mkNot(ctx.mkAnd((BoolExpr) parseSSATree(tree.getLeft().getLeft(), ctx),
                        (BoolExpr) parseSSATree(tree.getLeft().getRight(), ctx)));
                printAssert(expr);
                System.out.println("(check-sat)");
                System.out.println("(pop)");
                return null;
            case "<=":
                return ctx.mkLe((ArithExpr) parseSSATree(tree.getLeft(), ctx),
                        (ArithExpr) parseSSATree(tree.getRight(), ctx));
            case "<":
                return ctx.mkLt((ArithExpr) parseSSATree(tree.getLeft(), ctx),
                        (ArithExpr) parseSSATree(tree.getRight(), ctx));
            case ">=":
                return ctx.mkGe((ArithExpr) parseSSATree(tree.getLeft(), ctx),
                        (ArithExpr) parseSSATree(tree.getRight(), ctx));
            case ">":
                return ctx.mkGt((ArithExpr) parseSSATree(tree.getLeft(), ctx),
                        (ArithExpr) parseSSATree(tree.getRight(), ctx));
            case "==":
            case "=":
                return ctx.mkEq(parseSSATree(tree.getLeft(), ctx),
                        parseSSATree(tree.getRight(), ctx));
            case "!=":
                return ctx.mkNot(ctx.mkEq(parseSSATree(tree.getLeft(), ctx),
                        parseSSATree(tree.getRight(), ctx)));
            case "!":
                return ctx.mkNot((BoolExpr) parseSSATree(tree.getLeft(), ctx));
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
