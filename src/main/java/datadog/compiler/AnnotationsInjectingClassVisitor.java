package datadog.compiler;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;

public class AnnotationsInjectingClassVisitor extends TreeScanner<Void, Void> {
    private final TreeMaker maker;
    private final Names names;
    private final JCTree.JCAnnotation sourcePathAnnotation;
    private final JCTree.JCExpression methodLinesAnnotationType;
    private final boolean methodAnnotationDisabled;
    private final LineMap lineMap;
    private final EndPosTable endPositions;

    AnnotationsInjectingClassVisitor(TreeMaker maker,
                                     Names names,
                                     JCTree.JCAnnotation sourcePathAnnotation,
                                     JCTree.JCExpression methodLinesAnnotationType,
                                     boolean methodAnnotationDisabled,
                                     LineMap lineMap,
                                     EndPosTable endPositions) {
        this.maker = maker;
        this.names = names;
        this.sourcePathAnnotation = sourcePathAnnotation;
        this.methodLinesAnnotationType = methodLinesAnnotationType;
        this.methodAnnotationDisabled = methodAnnotationDisabled;
        this.lineMap = lineMap;
        this.endPositions = endPositions;
    }

    @Override
    public Void visitClass(ClassTree node, Void aVoid) {
        JCTree.JCClassDecl classDeclaration = (JCTree.JCClassDecl) node;
        for (JCTree.JCAnnotation annotation : classDeclaration.mods.annotations) {
            if (annotation.annotationType.toString().endsWith("SourcePath")) {
                // The method is already annotated with @SourcePath.
                // This can happen, for instance, when code-generation tools are used
                // that copy annotations from interface to class
                return super.visitClass(node, aVoid);
            }
        }

        if (node.getSimpleName().length() > 0) {
            classDeclaration.mods.annotations = classDeclaration.mods.annotations.prepend(sourcePathAnnotation);
        }
        return super.visitClass(node, aVoid);
    }

    public Void visitMethod(MethodTree node, Void aVoid) {
        if (!methodAnnotationDisabled && (node instanceof JCTree.JCMethodDecl)) {
            JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) node;

            for (JCTree.JCAnnotation annotation : methodDecl.mods.annotations) {
                if (annotation.annotationType.toString().endsWith("MethodLines")) {
                    // The method is already annotated with @MethodLines.
                    // This can happen, for instance, when code-generation tools are used
                    // that copy annotations from interface methods to class methods
                    return super.visitMethod(node, aVoid);
                }
            }

            JCTree.JCModifiers modifiers = methodDecl.getModifiers();
            if ((modifiers.flags & Flags.PUBLIC) != 0) {

                int startPosition = modifiers.getStartPosition();
                if (startPosition == Position.NOPOS) {
                    startPosition = methodDecl.getStartPosition();
                }

                int endPosition = methodDecl.getEndPosition(endPositions);
                if (endPosition == Position.NOPOS) {
                    BlockTree methodBody = node.getBody();
                    if (methodBody != null) {
                        JCTree methodBodyTree = (JCTree) methodBody;
                        endPosition = methodBodyTree.getEndPosition(endPositions);
                    }
                }

                int startLine = (int) lineMap.getLineNumber(startPosition);
                int endLine = (int) lineMap.getLineNumber(endPosition);

                Name startName = names.fromString("start");
                JCTree.JCIdent startIdent = maker.Ident(startName);
                JCTree.JCLiteral startLiteral = maker.Literal(startLine);
                JCTree.JCAssign startAssign = maker.Assign(startIdent, startLiteral);

                Name endName = names.fromString("end");
                JCTree.JCIdent endIdent = maker.Ident(endName);
                JCTree.JCLiteral endLiteral = maker.Literal(endLine);
                JCTree.JCAssign endAssign = maker.Assign(endIdent, endLiteral);

                JCTree.JCAnnotation methodLinesAnnotation = annotation(maker, methodLinesAnnotationType, startAssign, endAssign);
                methodDecl.mods.annotations = methodDecl.mods.annotations.prepend(methodLinesAnnotation);
            }
        }
        return super.visitMethod(node, aVoid);
    }

    private static JCTree.JCAnnotation annotation(TreeMaker maker, JCTree type, JCTree.JCExpression... arguments) {
        return maker.Annotation(type, List.from(arguments));
    }
}
