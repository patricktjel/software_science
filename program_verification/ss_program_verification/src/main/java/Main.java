import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.microsoft.z3.*;

import java.io.IOException;

public class Main {

//https://tomassetti.me/parsing-in-java/#javaLibraries
    public static void main(String[] args) throws IOException {
        CompilationUnit compilationUnit = JavaParser.parseResource("Example.java");
        MethodDeclaration method = (MethodDeclaration) compilationUnit.getChildNodes().get(0).getChildNodes().get(1);
        BlockStmt code = (BlockStmt) method.getChildNodes().get(method.getChildNodes().size() - 1);
        parseMethodToSSA(code);
    }

    static void parseMethodToSSA(Node node) {
        parseToZ3();
    }

    static void parseToZ3 () {
        Context context = new Context();
        context.close();
    }
}
