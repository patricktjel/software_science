import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.util.Optional;

public class Msain {

    public static void main(String[] args) {
        CompilationUnit compilationUnit = JavaParser.parse("class A { }");
        Optional<ClassOrInterfaceDeclaration> classA = compilationUnit.getClassByName("A");
        System.out.println("hello");
    }
}
