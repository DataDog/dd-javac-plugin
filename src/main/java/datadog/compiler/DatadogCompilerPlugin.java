package datadog.compiler;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import datadog.compiler.annotations.SourcePath;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import javax.tools.JavaFileObject;
import org.burningwave.core.assembler.StaticComponentContainer;
import org.burningwave.core.function.Executor;
import org.burningwave.core.function.ThrowingRunnable;

public class DatadogCompilerPlugin implements Plugin {

    static final String SKIP_ANNOTATIONS_ARGUMENT = "skipAnnotations";

    static {
        // to free users from having to declare --add-exports: https://openjdk.org/jeps/396
        if (StaticComponentContainer.JVMInfo.getVersion() >= 16) {
            StaticComponentContainer.Modules.exportToAllUnnamed("jdk.compiler");
        }
        // force classes to be loaded: https://github.com/burningwave/core/discussions/15
        ThrowingRunnable.class.getClassLoader();
        Executor.class.getClassLoader();
    }

    static final String NAME = "DatadogCompilerPlugin";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... strings) {
        if (task instanceof BasicJavacTask) {
            Collection<String> arguments = Arrays.asList(strings);
            boolean skipAnnotations = arguments.contains(SKIP_ANNOTATIONS_ARGUMENT);

            BasicJavacTask basicJavacTask = (BasicJavacTask) task;
            Context context = basicJavacTask.getContext();

            JCTree.JCExpression annotationType = TypeLoader.loadType(context, SourcePath.class);
            task.addTaskListener(new DatadogCompilerPluginTaskListener(skipAnnotations, context, annotationType));
            Log.instance(context).printRawLines(Log.WriterKind.NOTICE, NAME + " initialized");
        }
    }

    private static final class DatadogCompilerPluginTaskListener implements TaskListener {
        private final Context context;
        private final JCTree.JCExpression annotationType;
        private final boolean skipAnnotations;

        private DatadogCompilerPluginTaskListener(boolean skipAnnotations, Context context, JCTree.JCExpression annotationType) {
            this.skipAnnotations = skipAnnotations;
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

                SourcePathInjectingClassVisitor treeVisitor = new SourcePathInjectingClassVisitor(skipAnnotations, annotation);
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
        private final boolean skipAnnotations;
        private final JCTree.JCAnnotation annotation;

        private SourcePathInjectingClassVisitor(boolean skipAnnotations, JCTree.JCAnnotation annotation) {
            this.skipAnnotations = skipAnnotations;
            this.annotation = annotation;
        }

        @Override
        public Void visitClass(ClassTree node, Void aVoid) {
            if (skipAnnotations && node.getKind() == Tree.Kind.ANNOTATION_TYPE) {
                return super.visitClass(node, aVoid);
            }

            JCTree.JCClassDecl classDeclaration = (JCTree.JCClassDecl) node;
            if (node.getSimpleName().length() > 0) {
                classDeclaration.mods.annotations = classDeclaration.mods.annotations.prepend(annotation);
            }
            return super.visitClass(node, aVoid);
        }
    }

}
