import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static Map<String, String> vars = new HashMap<>();
    private static List<String> asserts = new ArrayList<>();
    private static int path = 0;
    private ArrayList<Tree<String>> lines = new ArrayList();


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
    }

    private static void parseExpression(Node node) {
        //System.out.println("Expression .... + " + node.toString());
        //Expression Check if the path condition holds,
        VariableDeclarator decl = (VariableDeclarator) node.getChildNodes().get(0).getChildNodes().get(0);
        String name = decl.getNameAsString();
        String pathName = "c_" + path;
        vars.put(name, decl.getTypeAsString());

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
        tree.addLeftNode(new Tree<>("ifPath"));

        Tree<String> exp  = new Tree<>(con.getOperator().asString());
        //tree.addLeftNode();
        //If body
        System.out.print("\t");
        path++;
        parseExpression((node).getThenStmt().getChildNodes().get(0));
        //Else statement
        addAssertion(elsePath + " = " + oldPath + " && ! " + ifPath);
        path++;
        System.out.print("\t");
        //Only print an if body if the if body is present
        if (node.getElseStmt().isPresent()) {
            parseExpression((node).getElseStmt().get().getChildNodes().get(0));
        }

        path++;
        addAssertion("c_" + path + " = " + ifPath + " || " + elsePath);
        vars.put("c_" + path, "Bool");
    }

    private static void addAssertion(String assertion) {
        System.out.println(assertion);
        asserts.add(assertion);
    }

    private static Tree<String> parseBinExpresion(BinaryExpr node, Tree tree) {
        //Tree<String> root =  node.getOperator().asString();
        return null;
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

        asserts.forEach(s -> {
            String[] assert_parts = s.split(" ");
            for (String part : assert_parts) {
//                System.out.println("help");
            }
        });

        ctx.close();
    }

    private static void printAssert(Expr smt2) {
        System.out.println("(assert " + smt2 + ")");
    }
}
