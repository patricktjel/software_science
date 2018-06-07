import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.io.IOException;

public class Main {

//https://tomassetti.me/parsing-in-java/#javaLibraries
    public static void main(String[] args) throws IOException {
        CompilationUnit compilationUnit = JavaParser.parseResource("Example.java");
        BlockStmt method = (BlockStmt) compilationUnit.getChildNodes().get(0).getChildNodes().get(1).getChildNodes().get(3);
        System.out.println("hello");
    }
}
