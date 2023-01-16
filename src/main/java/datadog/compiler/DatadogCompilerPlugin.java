package datadog.compiler;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import datadog.compiler.annotations.SourcePath;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.tools.JavaFileObject;

public class DatadogCompilerPlugin implements Plugin {

    static final String NAME = "DatadogCompilerPlugin";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... strings) {
        if (task instanceof BasicJavacTask) {
            BasicJavacTask basicJavacTask = (BasicJavacTask) task;
            Context context = basicJavacTask.getContext();

            JCTree.JCExpression annotationType = readAnnotationType(context);
            task.addTaskListener(new DatadogCompilerPluginTaskListener(context, annotationType));
            Log.instance(context).printRawLines(Log.WriterKind.NOTICE, NAME + " initialized");
        }
    }

    private static JCTree.JCExpression readAnnotationType(Context context) {
        Names names = Names.instance(context);
        Class<SourcePath> annotationClass = SourcePath.class;
        Name annotationClassName = names.fromString(annotationClass.getName());

        ClassReader classReader = ClassReader.instance(context);
        Symbol.ClassSymbol annotationSymbol = classReader.enterClass(annotationClassName);

        TreeMaker maker = TreeMaker.instance(context);
        return maker.Type(annotationSymbol.type);
    }

    private static final class DatadogCompilerPluginTaskListener implements TaskListener {
        private final Context context;
        private final JCTree.JCExpression annotationType;

        private DatadogCompilerPluginTaskListener(Context context, JCTree.JCExpression annotationType) {
            this.context = context;
            this.annotationType = annotationType;
        }

        @Override
        public void started(TaskEvent e) {
        }

        @Override
        public void finished(TaskEvent e) {
            if (e.getKind() != TaskEvent.Kind.PARSE) {
                return;
            }

            try {
                JavaFileObject sourceFile = e.getSourceFile();
                URI sourceUri = sourceFile.toUri();
                Path sourcePath = Paths.get(sourceUri).toAbsolutePath();

                TreeMaker maker = TreeMaker.instance(context);
                JCTree.JCLiteral annotationValue = maker.Literal(sourcePath.toString());
                JCTree.JCAnnotation annotation = maker.Annotation(annotationType, List.of(annotationValue));

                SourcePathInjectingClassVisitor treeVisitor = new SourcePathInjectingClassVisitor(annotation);
                e.getCompilationUnit().accept(treeVisitor, null);

            } catch (Throwable t) {
                Log log = Log.instance(context);
                log.printRawLines(
                        Log.WriterKind.WARNING,
                        "Could not inject source path field into "
                                + log.currentSourceFile().toUri()
                                + ": "
                                + t.getMessage());

                PrintWriter logWriter = log.getWriter(Log.WriterKind.WARNING);
                t.printStackTrace(logWriter);
            }
        }
    }

    private static final class SourcePathInjectingClassVisitor extends TreeScanner<Void, Void> {
        private final JCTree.JCAnnotation annotation;

        private SourcePathInjectingClassVisitor(JCTree.JCAnnotation annotation) {
            this.annotation = annotation;
        }

        @Override
        public Void visitClass(ClassTree node, Void aVoid) {
            JCTree.JCClassDecl classDeclaration = (JCTree.JCClassDecl) node;
            classDeclaration.mods.annotations = classDeclaration.mods.annotations.prepend(annotation);
            return super.visitClass(node, aVoid);
        }
    }
}
