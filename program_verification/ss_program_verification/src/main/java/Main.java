import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
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

    private static Map<String, Expr> z3Vars = new HashMap<>();

    //https://tomassetti.me/parsing-in-java/#javaLibraries
    public static void main(String[] args) throws IOException {
        CompilationUnit compilationUnit = JavaParser.parseResource("Example.java");
        MethodDeclaration method = (MethodDeclaration) compilationUnit.getChildNodes().get(0).getChildNodes().get(1);
        BlockStmt code = (BlockStmt) method.getChildNodes().get(method.getChildNodes().size() - 1);
        parseMethodToSSA(code);
    }

    static void parseMethodToSSA(Node node) {
        //Set up initial Path variables
        vars.put("c_0", "Bool");
        for (Node child : node.getChildNodes()) {
            if (child instanceof ExpressionStmt) {
                //Expression Check if the path condition holds,
                VariableDeclarator decl = (VariableDeclarator) child.getChildNodes().get(0).getChildNodes().get(0);
                String name = decl.getNameAsString();
                String pathName = "c_" + path;
                vars.put(name, decl.getTypeAsString());

                // This has the form of var_name = path_con ? decl : var_name
                String assertion = name + " = " + pathName + " ? " + decl.getInitializer().get().toString() + " : " + name;
                System.out.println(assertion);
                asserts.add(assertion);
            } else if (child instanceof IfStmt) {
                // If statements
                String pathName = "c_" + path + 1 ;
            }
        }
        parseToZ3();
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
                System.out.println("help");
            }
        });

        ctx.close();
    }

    private static void printAssert(Expr smt2) {
        System.out.println("(assert " + smt2 + ")");
    }
}
