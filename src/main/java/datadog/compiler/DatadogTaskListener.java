package datadog.compiler;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.tools.JavaFileObject;

final class DatadogTaskListener implements TaskListener {
    private final BasicJavacTask basicJavacTask;
    private final boolean methodAnnotationDisabled;

    DatadogTaskListener(BasicJavacTask basicJavacTask, boolean methodAnnotationDisabled) {
        this.basicJavacTask = basicJavacTask;
        this.methodAnnotationDisabled = methodAnnotationDisabled;
    }

    @Override
    public void started(TaskEvent e) {
    }

    @Override
    public void finished(TaskEvent e) {
        TaskEvent.Kind taskKind = e.getKind();
        if (taskKind != TaskEvent.Kind.PARSE) {
            return;
        }

        Context context = basicJavacTask.getContext();
        try {
            Path sourcePath = getSourcePath(e);
            if (sourcePath.endsWith("module-info.java")) {
                ModuleOpeningClassVisitor moduleOpeningClassVisitor = new ModuleOpeningClassVisitor(context);
                CompilationUnitTree compilationUnit = e.getCompilationUnit();
                compilationUnit.accept(moduleOpeningClassVisitor, null);
                return;
            }

            TreeMaker maker = TreeMaker.instance(context);
            Names names = Names.instance(context);

            JCTree.JCExpression sourcePathAnnotationType = select(maker, names, "datadog", "compiler", "annotations", "SourcePath");
            JCTree.JCAnnotation sourcePathAnnotation = annotation(maker, sourcePathAnnotationType, maker.Literal(sourcePath.toString()));

            JCTree.JCExpression methodLinesAnnotationType = select(maker, names, "datadog", "compiler", "annotations", "MethodLines");

            CompilationUnitTree compilationUnit = e.getCompilationUnit();
            LineMap lineMap = compilationUnit.getLineMap();
            EndPosTable endPositions;
            if (compilationUnit instanceof JCTree.JCCompilationUnit) {
                JCTree.JCCompilationUnit jcCompilationUnit = (JCTree.JCCompilationUnit) compilationUnit;
                endPositions = jcCompilationUnit.endPositions;
            } else {
                endPositions = null;
            }

            AnnotationsInjectingClassVisitor treeVisitor = new AnnotationsInjectingClassVisitor(
                    maker, names, sourcePathAnnotation, methodLinesAnnotationType, methodAnnotationDisabled, lineMap, endPositions);
            compilationUnit.accept(treeVisitor, null);

        } catch (Throwable t) {
            Log log = Log.instance(context);
            log.printRawLines(
                    Log.WriterKind.WARNING,
                    "Could not process "
                            + log.currentSourceFile().toUri()
                            + ": "
                            + t.getMessage());

            PrintWriter logWriter = log.getWriter(Log.WriterKind.WARNING);
            t.printStackTrace(logWriter);
        }
    }

    private static Path getSourcePath(TaskEvent e) {
        JavaFileObject sourceFile = e.getSourceFile();
        URI sourceUri = sourceFile.toUri();
        return Paths.get(sourceUri).toAbsolutePath();
    }

    private static JCTree.JCExpression select(TreeMaker maker, Names names, String... parts) {
        JCTree.JCExpression id = maker.Ident(names.fromString(parts[0]));
        for (int i = 1; i < parts.length; i++) {
            id = maker.Select(id, names.fromString(parts[i]));
        }
        return id;
    }

    private static JCTree.JCAnnotation annotation(TreeMaker maker, JCTree type, JCTree.JCExpression... arguments) {
        return maker.Annotation(type, List.from(arguments));
    }
}
