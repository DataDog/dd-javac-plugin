package datadog.compiler;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;
import datadog.compiler.annotations.MethodLines;
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

    static final String UNNAMED_MODULE_OPEN_DISABLED = "unnabledModuleOpenDisabled";
    static final String METHOD_ANNOTATION_DISABLED = "methodAnnotationDisabled";

    static {
        try {
            // to free users from having to declare --add-exports: https://openjdk.org/jeps/396
            if (StaticComponentContainer.JVMInfo.getVersion() >= 16) {
                StaticComponentContainer.Modules.exportToAllUnnamed("jdk.compiler");
            }
            // force classes to be loaded: https://github.com/burningwave/core/discussions/15
            ThrowingRunnable.class.getClassLoader();
            Executor.class.getClassLoader();
        } catch (Throwable e) {
            // ignore
        }
    }

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

            Collection<String> arguments = Arrays.asList(strings);
            boolean unnamedModuleOpenDisabled = arguments.contains(UNNAMED_MODULE_OPEN_DISABLED);
            if (!unnamedModuleOpenDisabled) {
                UnnamedModuleOpener.open(context);
            }

            boolean methodAnnotationDisabled = arguments.contains(METHOD_ANNOTATION_DISABLED);
            JCTree.JCExpression sourcePathAnnotationType = TypeLoader.loadType(context, SourcePath.class);
            JCTree.JCExpression methodLinesAnnotationType = TypeLoader.loadType(context, MethodLines.class);
            task.addTaskListener(new DatadogCompilerPluginTaskListener(basicJavacTask, sourcePathAnnotationType, methodLinesAnnotationType, methodAnnotationDisabled));
            Log.instance(context).printRawLines(Log.WriterKind.NOTICE, NAME + " initialized");
        }
    }

    private static final class DatadogCompilerPluginTaskListener implements TaskListener {
        private final BasicJavacTask basicJavacTask;
        private final JCTree.JCExpression sourcePathAnnotationType;
        private final JCTree.JCExpression methodLinesAnnotationType;
        private final boolean methodAnnotationDisabled;

        private DatadogCompilerPluginTaskListener(BasicJavacTask basicJavacTask,
                                                  JCTree.JCExpression sourcePathAnnotationType,
                                                  JCTree.JCExpression methodLinesAnnotationType,
                                                  boolean methodAnnotationDisabled) {
            this.basicJavacTask = basicJavacTask;
            this.sourcePathAnnotationType = sourcePathAnnotationType;
            this.methodLinesAnnotationType = methodLinesAnnotationType;
            this.methodAnnotationDisabled = methodAnnotationDisabled;
        }

        @Override
        public void started(TaskEvent e) {
        }

        @Override
        public void finished(TaskEvent e) {
            if (e.getKind() != TaskEvent.Kind.PARSE) {
                return;
            }

            Context context = basicJavacTask.getContext();
            try {
                JavaFileObject sourceFile = e.getSourceFile();
                URI sourceUri = sourceFile.toUri();
                Path sourcePath = Paths.get(sourceUri).toAbsolutePath();

                TreeMaker maker = TreeMaker.instance(context);
                JCTree.JCAnnotation sourcePathAnnotation = maker.Annotation(sourcePathAnnotationType, List.of(maker.Literal(sourcePath.toString())));

                CompilationUnitTree compilationUnit = e.getCompilationUnit();
                LineMap lineMap = compilationUnit.getLineMap();
                EndPosTable endPositions;
                if (compilationUnit instanceof JCTree.JCCompilationUnit) {
                    JCTree.JCCompilationUnit jcCompilationUnit = (JCTree.JCCompilationUnit) compilationUnit;
                    endPositions = jcCompilationUnit.endPositions;
                } else {
                    endPositions = null;
                }

                Names names = Names.instance(context);
                SourcePathInjectingClassVisitor treeVisitor = new SourcePathInjectingClassVisitor(
                        maker, names, sourcePathAnnotation, methodLinesAnnotationType, methodAnnotationDisabled, lineMap, endPositions);
                compilationUnit.accept(treeVisitor, null);

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
        private final TreeMaker maker;
        private final Names names;
        private final JCTree.JCAnnotation sourcePathAnnotation;
        private final JCTree.JCExpression methodLinesAnnotationType;
        private final boolean methodAnnotationDisabled;
        private final LineMap lineMap;
        private final EndPosTable endPositions;

        private SourcePathInjectingClassVisitor(TreeMaker maker,
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
            if (node.getSimpleName().length() > 0) {
                classDeclaration.mods.annotations = classDeclaration.mods.annotations.prepend(sourcePathAnnotation);
            }
            return super.visitClass(node, aVoid);
        }

        public Void visitMethod(MethodTree node, Void aVoid) {
            if (!methodAnnotationDisabled && (node instanceof JCTree.JCMethodDecl)) {
                JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) node;
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

                    JCTree.JCAnnotation methodLinesAnnotation = maker.Annotation(methodLinesAnnotationType, List.of(startAssign, endAssign));
                    methodDecl.mods.annotations = methodDecl.mods.annotations.prepend(methodLinesAnnotation);
                }
            }
            return super.visitMethod(node, aVoid);
        }
    }

}
