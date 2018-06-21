import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.microsoft.z3.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static final String PATH_LETTER = "c";
    private static Map<String, Variable> vars = new HashMap<>();

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

        // parse the file
        MethodDeclaration method = (MethodDeclaration) compilationUnit.getChildNodes().get(0).getChildNodes().get(1);
        for (Parameter parameter : method.getParameters()) {
            vars.put(parameter.getNameAsString(), new Variable(parameter.getNameAsString(), parameter.getTypeAsString()));
        }
        BlockStmt code = (BlockStmt) method.getChildNodes().get(method.getChildNodes().size() - 1);

        parseMethodToSSA(code);
        printSSA();
        parseToZ3();
    }

    /**
     * Parses the content of a method
     *
     * @param node the node containing the method
     */
    private static void parseMethodToSSA(Node node) {
        //Set up initial Path variables
        vars.put(PATH_LETTER, new Variable(PATH_LETTER, "Bool"));
        for (Node child : node.getChildNodes()) {
            if (child instanceof ExpressionStmt) {
                Tree<String> tree = parseExpression(child);
                lines.add(tree);
            } else if (child instanceof IfStmt) {
                parseITE((IfStmt) child);
            } else if (child instanceof AssertStmt) {
                parseAssert((AssertStmt) child);
            } else if (child instanceof WhileStmt) {
                parseWhile((WhileStmt) child);
            }
        }
    }

    /**
     * Parse a statement in the form of 'while ...'
     * @param node The node containing the while statement
     */
    private static void parseWhile(WhileStmt node) {
        String[] comment = node.getChildNodes().get(1).getChildNodes().get(0).getComment().get().getContent().split(";");
        Expression decreases = JavaParser.parseExpression(comment[0]);
        Expression parsed_inv = JavaParser.parseExpression(comment[1]);
        String modifies = comment[2].trim();

        // invariant should hold
        {
            Tree<String> invariant = parseBinExpression((BinaryExpr) parsed_inv);

            Tree<String> tree = new Tree<>("assertinv");
            tree.addRightNode(invariant);

            lines.add(new Tree<>("push"));
            lines.add(tree);
            lines.add(new Tree<>("check-sat"));
            lines.add(new Tree<>("pop"));
        }
        // create a new path variable
        {
            Tree<String> condition_path = new Tree<>("=");
            condition_path.addLeftNode(new Tree<>(vars.get(PATH_LETTER).getNext()));
            Tree<String> and = new Tree<>("&&");
            and.addLeftNode(new Tree<>(vars.get(PATH_LETTER).getPrevious()));
            and.addRightNode(parseBinExpression((BinaryExpr) node.getCondition()));
            condition_path.addRightNode(and);
            lines.add(condition_path);
        }

        // save the decreases value
        Tree<String> decreases_left;
        // the body
        {
            vars.get(modifies).getNext();

            // save the decreases value at the beginning of the loop body
            if (decreases instanceof BinaryExpr) {
                decreases_left = parseBinExpression((BinaryExpr) decreases);
            } else {
                String val = ((NameExpr) decreases).getName().toString();
                decreases_left = new Tree<>(vars.get(val).getCurrent());
            }

            Tree<String> invariant = parseBinExpression((BinaryExpr) parsed_inv);
            Tree<String> condition = parseBinExpression((BinaryExpr) node.getCondition());

            // check condition and assert on old value.
            Tree<String> tree = new Tree<>("=>");
            tree.addLeftNode(new Tree<>(vars.get(PATH_LETTER).getCurrent()));
            tree.addRightNode(new Tree<>("&&"));
            tree.getRight().addLeftNode(invariant);
            tree.getRight().addRightNode(condition);
            lines.add(tree);

            parseBody(node.getChildNodes().get(1).getChildNodes());
        }
        //afterwards the invariant should still hold
        {
            Tree<String> tree = new Tree<>("&&");
            tree.addLeftNode(new Tree<>(vars.get(PATH_LETTER).getCurrent()));

            Tree<String> assertinv = new Tree<>("assertinv");
            Tree<String> invariant = parseBinExpression((BinaryExpr) parsed_inv);
            assertinv.addRightNode(invariant);

            tree.addRightNode(assertinv);

            lines.add(new Tree<>("push"));
            lines.add(tree);
            lines.add(new Tree<>("check-sat"));
            lines.add(new Tree<>("pop"));
        }
        // decreases
        {
            Tree<String> decreases_right;
            if (decreases instanceof BinaryExpr) {
                decreases_right = parseBinExpression((BinaryExpr) decreases);
            } else {
                String val = ((NameExpr) decreases).getName().toString();
                decreases_right = new Tree<>(vars.get(val).getCurrent());
            }

            Tree<String> tree = new Tree<>("assert");
            tree.addRightNode(new Tree<>("&&"));
            tree.getRight().addLeftNode(new Tree<>(vars.get(PATH_LETTER).getCurrent()));

            Tree<String> decreasesTree = new Tree<>(">");
            decreasesTree.addLeftNode(decreases_left);
            decreasesTree.addRightNode(decreases_right);

            tree.getRight().addRightNode(new Tree<>("!"));
            tree.getRight().getRight().addRightNode(decreasesTree);

            lines.add(new Tree<>("push"));
            lines.add(tree);
            lines.add(new Tree<>("check-sat"));
            lines.add(new Tree<>("pop"));
        }
        // create a new path variable for after the while
        {
            // the variable is always the same as the variable before the while loop.
            Tree<String> condition_path = new Tree<>("=");
            condition_path.addRightNode(new Tree<>(vars.get(PATH_LETTER).getPrevious()));
            condition_path.addLeftNode(new Tree<>(vars.get(PATH_LETTER).getNext()));

            lines.add(condition_path);
        }
        //After the loop is complete the invariant still holds, and the loop condition does not hold anymore
        {
            vars.get(modifies).getNext();

            Tree<String> tree = new Tree<>("&&");
            tree.addLeftNode(new Tree<>(vars.get(PATH_LETTER).getCurrent()));

            Tree<String> invariant = parseBinExpression((BinaryExpr) parsed_inv);
            Tree<String> condition = parseBinExpression((BinaryExpr) node.getCondition());
            Tree<String> after = new Tree<>("&&");
            after.addLeftNode(invariant);

            tree.addRightNode(after);

            Tree<String> negation = new Tree<>("!");
            negation.addRightNode(condition);
            after.addRightNode(negation);

            lines.add(tree);
        }
    }

    /**
     * Parse a statement in the form of 'assert ... '
     * @param node The node containing the assert statement
     */
    private static void parseAssert(AssertStmt node) {
        lines.add(new Tree<>("push"));
        Tree<String> a = new Tree<>("assert");
        Tree<String> and = new Tree<>("=>");
        a.addRightNode(and);
        and.addLeftNode(new Tree<>(vars.get(PATH_LETTER).getCurrent()));
        and.addRightNode(new Tree<>("!"));
        and.getRight().addRightNode(parseBinExpression((BinaryExpr) node.getCheck()));
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
        boolean variableExists = vars.containsKey(name);
        if (!variableExists) {
            vars.put(name, new Variable(name, decl.getTypeAsString()));
        }

        Tree<String> tree = new Tree<>("=");

        Tree<String> op = new Tree<>("?");
        tree.addRightNode(op);
        op.addLeftNode(new Tree<>(vars.get(PATH_LETTER).getCurrent()));

        Tree<String> thenElse = new Tree<>(":");
        op.addRightNode(thenElse);

        if (decl.getInitializer().get() instanceof BinaryExpr) {
            thenElse.addLeftNode(parseBinExpression((BinaryExpr) decl.getInitializer().get()));
        } else {
            Variable var = vars.get(decl.getInitializer().get().toString());
            if (var != null) {
                thenElse.addLeftNode(new Tree<>(var.getCurrent()));
            } else {
                thenElse.addLeftNode(new Tree<>(decl.getInitializer().get().toString()));
            }
        }

        // the variable isn't introduced in this instance of this method so we need the next instance of the variable.
        if (variableExists) {
            vars.get(name).getNext();
        }

        tree.addLeftNode(new Tree<>(vars.get(name).getCurrent()));
        thenElse.addRightNode(new Tree<>(vars.get(name).getPrevious()));
        return tree;
    }

    /**
     * Creates trees for ITE statements (one tree for the if, one for every line in the body, one for the else,
     *          one for every line in the else body, one for every modified variable and one for the close of the if)
     * This method will save the state of the variables which will be modified at the beginning of the if statement.
     * After the if body is executed it will save the state of these variables again in the variable ifState and the vars get a reset till the beginning state of the if statement.
     * After the else body is executed the same will happen, the variables will be saved in the variable elseState and the vars get a reset.
     *
     * To be able to use the variables again outside the if statement another line gets added.
     * In which the next SSA value of the variable get's the value of the ifState if the if statement was executed (according to the path condition)
     * Otherwise the variable get's the value of the elseState.
     * resulting in i_3 = c_1 ? i_2 : i_1 for example
     *
     * @param node the node containing the ite statement
     */
    private static void parseITE(IfStmt node) {
        // If condition
        String oldPath = vars.get(PATH_LETTER).getCurrent();
        String ifPath = vars.get(PATH_LETTER).getNext();

        List<Integer> current = new ArrayList<>();
        List<String> modifies = new ArrayList<>();

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
            lines.add(tree);
        }

        //If body
        {
            List<Tree<String>> added = parseBody(node.getThenStmt().getChildNodes());
            modifiesList(current, modifies, added);
        }

        String elsePath = vars.get(PATH_LETTER).getNext();
        //Else condition path value
        {
            Tree<String> elseTree = new Tree<>("=");
            elseTree.addLeftNode(new Tree<>(elsePath));

            Tree<String> conjunction = new Tree<>("&&");
            elseTree.addRightNode(conjunction);
            conjunction.addLeftNode(new Tree<>(oldPath));

            Tree<String> negation = new Tree<>("!");
            conjunction.addRightNode(negation);
            negation.addRightNode(new Tree<>(ifPath));

            lines.add(elseTree);
        }

        List<Integer> ifState = new ArrayList<>();
        List<Integer> elseState = new ArrayList<>();
        //Only printInOrder an if body if the if body is present
        if (node.getElseStmt().isPresent()) {
            // save the state of the vars of the if state before resetting everything.
            ifState = getVarsStateAndReset(modifies, current);

            // parse the else body
            List<Tree<String>> added = parseBody(node.getElseStmt().get().getChildNodes());
            modifiesList(current, modifies, added);

            // save the state of the vars of the else state
            elseState = getVarsStateAndReset(modifies, current);
        }

        //End-if tree path condition
        {
            Tree<String> endIf = new Tree<>("=");
            endIf.addLeftNode(new Tree<>(vars.get(PATH_LETTER).getNext()));
            Tree<String> disjunction = new Tree<>("||");
            endIf.addRightNode(disjunction);
            disjunction.addLeftNode(new Tree<>(ifPath));
            disjunction.addRightNode(new Tree<>(elsePath));
            lines.add(endIf);
        }

        if (node.getElseStmt().isPresent()) {
            for (int i = 0; i < elseState.size(); i++) {
                Variable var = vars.get(modifies.get(i));

                int ifVal;
                if (i >= ifState.size()) {
                    ifVal = current.get(i);
                } else {
                    ifVal = ifState.get(i);
                }
                int elseVal = elseState.get(i);

                if (ifVal > elseVal) {
                    var.createTill(ifVal);
                } else {
                    var.createTill(elseVal);
                }

                // determine which value is set during the ITE
                Tree<String> setValue = new Tree<>("=");
                Tree<String> pathCon = new Tree<>("?");
                pathCon.addLeftNode(new Tree<>(ifPath));
                setValue.addRightNode(pathCon);

                Tree<String> ifElseTree = new Tree<>(":");
                pathCon.addRightNode(ifElseTree);

                ifElseTree.addLeftNode(new Tree<>(var.getVariables().get(ifVal)));
                ifElseTree.addRightNode(new Tree<>(var.getVariables().get(elseVal)));

                setValue.addLeftNode(new Tree<>(var.getNext()));
                lines.add(setValue);
            }
        }
    }

    private static void modifiesList(List<Integer> current, List<String> modifies, List<Tree<String>> added) {
        for (Tree<String> tree : added) {
            if (tree.getData().equals("=")) {
                String varname = truncateUnderscore(tree.getLeft().getData());
                if (!modifies.contains(varname)) {
                    modifies.add(varname);
                    current.add(vars.get(varname).getPreviousInt());
                }
            }
        }
    }

    public static List<Integer> getVarsStateAndReset(List<String> modifies, List<Integer> current) {
        List<Integer> collect = modifies.stream().map(s -> vars.get(s).getVariables().size() - 1).collect(Collectors.toList());
        for (int i = 0; i < modifies.size(); i++) {
            vars.get(modifies.get(i)).resetTo(current.get(i));
        }
        return collect;
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
                root.addLeftNode(new Tree<>(vars.get(node.getLeft().toString()).getCurrent()));
            }
        }

        //Check right node
        if (node.getRight() instanceof BinaryExpr) {
            root.addRightNode(parseBinExpression((BinaryExpr) node.getRight()));
        } else {
            if (node.getRight() instanceof IntegerLiteralExpr) {
                root.addRightNode(new Tree<>(node.getRight().toString()));
            } else {
                root.addRightNode(new Tree<>(vars.get(node.getRight().toString()).getCurrent()));
            }
        }
        return root;
    }

    /**
     * Parses the body of an if/else/while statement
     * @return
     */
    private static List<Tree<String>> parseBody(List<Node> nodes) {
        List<Tree<String>> added = new ArrayList<>();
        for (Node node : nodes) {
            Tree<String> expTree = parseExpression(node);
            lines.add(expTree);
            added.add(expTree);
        }
        return added;
    }

    /**
     * Creates smt2 format and prints it to the command line
     */
    private static void parseToZ3 () {
        Context ctx = new Context();

        // first declare all (path)variables
        vars.forEach((k, v) -> {
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
                return parseSSATree(tree.getRight(),ctx);
            case "assertinv":
                return ctx.mkNot((BoolExpr) parseSSATree(tree.getRight(), ctx));
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
                return ctx.mkNot((BoolExpr) parseSSATree(tree.getRight(), ctx));
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

    /**
     * printInOrder all trees in as SSA
     */
    private static void printSSA() {
        System.out.println("------- SSA ---------");
        for (Tree<String> line : lines) {
            if (!(line.getData().equals("push") || line.getData().equals("pop") || line.getData().equals("check-sat"))) {
                Tree.printInOrder(line);
                System.out.println();
            }
        }
        System.out.println("------- END SSA ---------");
    }


    private static String truncateUnderscore(String input) {
        String[] split = input.split("_");
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < split.length-1; i++) {
            res.append("_").append(split[i]);
        }
        res.deleteCharAt(0);
        return res.toString();
    }

}
