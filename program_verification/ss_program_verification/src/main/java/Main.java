import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.microsoft.z3.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class Main {

    private static final List<String> pathVars = new LinkedList<>();
    private static final Map<String, String> vars = new HashMap<>();
    private static int path = 0;

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


    private static void parseMethodToSSA(Node node) {
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
        System.out.println("assert (" + "c_" + path + " && " + node.getCheck().toString() + ")");
    }

    private static void parseExpression(Node node) {
        //System.out.println("Expression .... + " + node.toString());
        //Expression Check if the path condition holds,
        VariableDeclarator decl = (VariableDeclarator) node.getChildNodes().get(0).getChildNodes().get(0);
        String name = decl.getNameAsString();
        String pathName = "c_" + path;
        vars.put(name, decl.getTypeAsString());

        // This has the form of var_name = path_con ? decl : var_name
        System.out.println(name + " = " + pathName + " ? " + decl.getInitializer().get().toString() + " : " + name);
    }

    private static void parseITE(IfStmt node) {
        // If statements
        String ifPath = "c_" + (path + 1);
        String elsePath = "c_" + (path + 2);
        String oldPath = "c_" + path;
        //If condition
        System.out.println(ifPath + " = " + oldPath + " && (" + ((node).getCondition().toString() + ")"));
        //If body
        System.out.print("\t");
        path++;
        parseExpression((node).getThenStmt().getChildNodes().get(0));
        //Else statement
        System.out.println(elsePath + " = " + oldPath + " && ! " + ifPath);
        path++;
        System.out.print("\t");
        //Only print an if body if the if body is present
        if (node.getElseStmt().isPresent()) {
            parseExpression((node).getElseStmt().get().getChildNodes().get(0));
        }

        path++;
        System.out.println("c_" + path + " = " + ifPath + " || " + elsePath);
    }

    static void parseToZ3 () {
        Context context = new Context();
        context.close();
    }
}
