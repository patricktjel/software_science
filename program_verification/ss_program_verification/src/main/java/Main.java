import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.stmt.*;
import com.microsoft.z3.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final String PATH_LETTER = "c";
    private static Map<String, Variable> varss = new HashMap<>();
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
        File file = new File(args[0]);
        CompilationUnit compilationUnit = JavaParser.parse(file);
        FileInputStream in = new FileInputStream(args[0]);

        // parse the file
        CompilationUnit cu = JavaParser.parse(in);
        MethodDeclaration method = (MethodDeclaration) compilationUnit.getChildNodes().get(0).getChildNodes().get(1);
        for (Parameter parameter : method.getParameters()) {
            varss.put(parameter.getNameAsString(), new Variable(parameter.getNameAsString(), parameter.getTypeAsString()));
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
        varss.put(PATH_LETTER, new Variable(PATH_LETTER, "Bool"));
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

    /**
     * Parse a statement in the form of 'while ...'
     * @param node The node containingthe while statement
     */
    private static void parseWhile(WhileStmt node) {
        String[] comment = node.getChildNodes().get(1).getChildNodes().get(0).getComment().get().getContent().split(";");
        String modifies = comment[1].trim();
        Expression parsed_inv = JavaParser.parseExpression(comment[0]);

        // invariant & condition should hold
        {
            Tree<String> invariant = parseBinExpression((BinaryExpr) parsed_inv);
            Tree<String> condition = parseBinExpression((BinaryExpr) node.getCondition());

            Tree<String> tree = new Tree<>("assertinv");
            tree.addLeftNode(new Tree<>("&&"));
            tree.getLeft().addLeftNode(invariant);
            tree.getLeft().addRightNode(condition);
            System.out.println("(assertinv (" + parsed_inv + " && " + node.getCondition() + "))");
            tree.print(0);

            lines.add(new Tree<>("push"));
            lines.add(tree);
            lines.add(new Tree<>("check-sat"));
            lines.add(new Tree<>("pop"));
        }
        // create a new path variable
        {
            Tree<String> condition_path = new Tree<>("=");
            condition_path.addLeftNode(new Tree<>(varss.get(PATH_LETTER).getNext()));
            Tree<String> and = new Tree<>("&&");
            and.addLeftNode(new Tree<>(varss.get(PATH_LETTER).getPrevious()));
            and.addRightNode(parseBinExpression((BinaryExpr) node.getCondition()));
            condition_path.addRightNode(and);
            lines.add(condition_path);
        }
        // the body
        {
            varss.get(modifies).getNext();
            Tree<String> invariant = parseBinExpression((BinaryExpr) parsed_inv);
            Tree<String> condition = parseBinExpression((BinaryExpr) node.getCondition());

            // check condition and assert on old value.
            Tree<String> tree = new Tree<>("&&");
            tree.addLeftNode(invariant);
            tree.addRightNode(condition);
            lines.add(tree);

            Node expr = node.getChildNodes().get(1).getChildNodes().get(0);
            Tree<String> body = parseExpression(expr);
            System.out.println(expr.getChildNodes().get(0));
            body.print(0);
            lines.add(body);
        }
        //afterwards the invariant should still hold
        {
            Tree<String> invariant = parseBinExpression((BinaryExpr) parsed_inv);

            Tree<String> tree = new Tree<>("assertinv");
            tree.addLeftNode(invariant);
            System.out.println("(assertinv (" + parsed_inv + ") )");

            lines.add(new Tree<>("push"));
            invariant.print(0);
            lines.add(tree);
            lines.add(new Tree<>("check-sat"));
            lines.add(new Tree<>("pop"));
        }
    }

    /**
     * Parse a statement in the form of 'assert ... '
     * @param node The node containing the assert statement
     */
    private static void parseAssert(AssertStmt node) {
        lines.add(new Tree<>("push"));
        Tree<String> a = new Tree<>("assert");
        Tree<String> and = new Tree<>("&&");
        a.addLeftNode(and);
        and.addLeftNode(new Tree<>("c_" + path));
        and.addRightNode(parseBinExpression((BinaryExpr) node.getCheck()));
        a.print(0);
        lines.add(a);
        lines.add(new Tree<>("check-sat"));
        lines.add(new Tree<>("pop"));
    }

    /**
     * Parses an expression and
     * @param node the expression to parse
     */
    private static Tree<String> parseExpression(Node node) {
        //Expression Check if the path condition holds,
        VariableDeclarator decl = (VariableDeclarator) node.getChildNodes().get(0).getChildNodes().get(0);
        String name = decl.getNameAsString();

        // if the variable is declared in this line create the variable and add it to the list
        boolean variableExists = varss.containsKey(name);
        if (!variableExists) {
            varss.put(name, new Variable(name, decl.getTypeAsString()));
        }

        Tree<String> tree = new Tree<>("=");

        Tree<String> op = new Tree<>("?");
        tree.addRightNode(op);
        op.addLeftNode(new Tree<>(varss.get(PATH_LETTER).getCurrent()));

        Tree<String> thenElse = new Tree<>(":");
        op.addRightNode(thenElse);

        if (decl.getInitializer().get() instanceof BinaryExpr) {
            thenElse.addLeftNode(parseBinExpression((BinaryExpr) decl.getInitializer().get()));
        } else {
            thenElse.addLeftNode(new Tree<>(decl.getInitializer().get().toString()));
        }

        // the variable isn't introduced in this instance of this method so we need the next instance of the variable.
        if (variableExists) {
            varss.get(name).getNext();
        }

        tree.addLeftNode(new Tree<>(varss.get(name).getCurrent()));
        thenElse.addRightNode(new Tree<>(varss.get(name).getPrevious()));
        return tree;
    }

    /**
     * Creates trees for ITE statements (one tree for the if, one for if body, one for else, one for else body and one for the close of the if)
     * @param node the node containing the ite statement
     */
    private static void parseITE(IfStmt node) {
        // If condition
        String oldPath = varss.get(PATH_LETTER).getCurrent();
        String ifPath = varss.get(PATH_LETTER).getNext();
        String elsePath = varss.get(PATH_LETTER).getNext();

        BinaryExpr con = (BinaryExpr) node.getCondition();

        // if condition path value
        {
            Tree<String> tree = new Tree<>("=");
            tree.addLeftNode(new Tree<>(ifPath));
            Tree<String> and = new Tree<>("&&");
            tree.addRightNode(and);
            and.addLeftNode(new Tree<>(oldPath));
            Tree<String> iteTree = parseBinExpression(con);
            and.addRightNode(iteTree);
            tree.print(0);
            lines.add(tree);
        }

        //If body
        {
            Tree<String> expTree = parseExpression((node).getThenStmt().getChildNodes().get(0));
            lines.add(expTree);
        }

        //Else condition path value
        {
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
        }

        //Only print an if body if the if body is present
        if (node.getElseStmt().isPresent()) {
           Tree<String> expr = parseExpression((node).getElseStmt().get().getChildNodes().get(0));
           lines.add(expr);
        }

        //End-if tree path condition
        {
            Tree<String> endIf = new Tree<>("=");
            endIf.addLeftNode(new Tree<>(varss.get(PATH_LETTER).getNext()));
            Tree<String> disjunction = new Tree<>("||");
            endIf.addRightNode(disjunction);
            disjunction.addLeftNode(new Tree<>(ifPath));
            disjunction.addRightNode(new Tree<>(elsePath));
            endIf.print(0);
            lines.add(endIf);
        }
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
            if (node.getLeft() instanceof IntegerLiteralExpr) {
                root.addLeftNode(new Tree<>(node.getLeft().toString()));
            } else {
                root.addLeftNode(new Tree<>(varss.get(node.getLeft().toString()).getCurrent()));
            }
        }

        //Check right node
        if (node.getRight() instanceof BinaryExpr) {
            root.addRightNode(parseBinExpression((BinaryExpr) node.getRight()));
        } else {

            if (node.getRight() instanceof IntegerLiteralExpr) {
                root.addRightNode(new Tree<>(node.getRight().toString()));
            } else {
                root.addRightNode(new Tree<>(varss.get(node.getRight().toString()).getCurrent()));
            }
        }
        return root;
    }

    /**
     * Creates smt2 format and prints it to the command line
     */
    private static void parseToZ3 () {
        Context ctx = new Context();

        // first declare all (path)variables
        varss.forEach((k,v) -> {
            v.getVariables().forEach(s -> {
                Expr expr = null;

                switch (v.getType().toLowerCase()){
                    case "int":
                        expr = ctx.mkIntConst(s);
                        break;
                    case "bool":
                        expr = ctx.mkBoolConst(s);
                }

                System.out.println(expr.getFuncDecl());
                z3Vars.put(s, expr);
            });
        });

        // set default path condition value
        printAssert(ctx.mkEq(z3Vars.get("c_0"), ctx.mkBool(true)));

        // create an assertion for every code line.
        lines.forEach(tree -> {
            Object expr = parseSSATree(tree, ctx);
            printAssert(expr);
        });

        ctx.close();
    }

    /**
     * Surrounds and smt expression with (assert ..)
     * @param smt2 the expression
     */
    private static void printAssert(Object smt2) {
        if (smt2 != null) {
            if (smt2 instanceof Expr) {
                System.out.println("(assert " + smt2 + ")");
            } else {
                System.out.println(smt2);
            }
        }
    }

    /**
     * parses one tree (e.g. one line of a file)
     * @param tree The tree to parse
     * @param ctx the context of z3
     * @return the z3 expression or null if the expression was already printed (e.g. to add push/pop)
     */
    @SuppressWarnings("ConstantConditions")
    private static Object parseSSATree(Tree<String> tree, Context ctx) {
        // also part of the base case
        switch (tree.getData().toLowerCase()){
            case "check-sat":
                return "(check-sat)";
            case "pop":
                return "(pop)";
            case "push":
                return "(push)";
        }

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
                return ctx.mkAnd((BoolExpr) parseSSATree(tree.getLeft().getLeft(), ctx),
                        ctx.mkNot((BoolExpr) parseSSATree(tree.getLeft().getRight(), ctx)));
            case "assertinv":
                return ctx.mkNot(ctx.mkAnd((BoolExpr) parseSSATree(tree.getLeft().getLeft(), ctx),
                        (BoolExpr) parseSSATree(tree.getLeft().getRight(), ctx)));
            case "=>":
                return ctx.mkImplies((BoolExpr) parseSSATree(tree.getLeft(), ctx),
                        (BoolExpr) parseSSATree(tree.getRight(), ctx));
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
                return ctx.mkEq((Expr)parseSSATree(tree.getLeft(), ctx),
                        (Expr)parseSSATree(tree.getRight(), ctx));
            case "!=":
                return ctx.mkNot(ctx.mkEq((Expr)parseSSATree(tree.getLeft(), ctx),
                        (Expr)parseSSATree(tree.getRight(), ctx)));
            case "!":
                return ctx.mkNot((BoolExpr) parseSSATree(tree.getLeft(), ctx));
            case "?":
                return ctx.mkITE((BoolExpr) parseSSATree(tree.getLeft(), ctx),
                        (Expr)parseSSATree(tree.getRight().getLeft(), ctx),
                        (Expr)parseSSATree(tree.getRight().getRight(), ctx));
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
