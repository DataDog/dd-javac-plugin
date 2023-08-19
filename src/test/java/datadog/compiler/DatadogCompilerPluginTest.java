package datadog.compiler;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.fail;

import datadog.compiler.utils.CompilerUtils;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DatadogCompilerPluginTest {

    @ParameterizedTest
    @MethodSource("sourcePathInjectionArguments")
    public void testSourcePathInjection(String resourceName,
                                        String className) throws Exception {
        String classSource;
        try (InputStream classStream = ClassLoader.getSystemResourceAsStream(resourceName)) {
            classSource = IOUtils.toString(classStream, Charset.defaultCharset());
        }

        String compiledClassName = resourceName.substring(0, resourceName.lastIndexOf('.')).replace('/', '.');
        try (InMemoryFileManager fileManager = compile(compiledClassName, classSource)) {
            Class<?> clazz = fileManager.loadCompiledClass(className);
            String sourcePath = CompilerUtils.getSourcePath(clazz);
            Assertions.assertEquals(InMemorySourceFile.sourcePath(compiledClassName), sourcePath);
        }
    }

    private static Stream<Arguments> sourcePathInjectionArguments() {
        return Stream.of(
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test"),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test$InnerClass"),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test$1"),
                Arguments.of("datadog/compiler/TestAnnotation.java", "datadog.compiler.TestAnnotation")
        );
    }

    @Test
    public void testSourcePathInjectionPreservesAnnotations() throws Exception {
        String resourceName = "datadog/compiler/DeprecatedClass.java";

        String classSource;
        try (InputStream classStream = ClassLoader.getSystemResourceAsStream(resourceName)) {
            classSource = IOUtils.toString(classStream, Charset.defaultCharset());
        }

        String compiledClassName = resourceName.substring(0, resourceName.lastIndexOf('.')).replace('/', '.');
        try (InMemoryFileManager fileManager = compile(compiledClassName, classSource)) {
            Class<?> clazz = fileManager.loadCompiledClass(compiledClassName);
            String sourcePath = CompilerUtils.getSourcePath(clazz);
            Assertions.assertEquals(InMemorySourceFile.sourcePath(compiledClassName), sourcePath, "source path was not injected");
            Assertions.assertNotNull(clazz.getAnnotation(Deprecated.class), "existing annotation was not preserved");
        }
    }

    @Test
    public void testAnonymousClassCapturedFieldReference() throws Exception {
        String resourceName = "datadog/compiler/TestCapturedFieldReference.java";

        String classSource;
        try (InputStream classStream = ClassLoader.getSystemResourceAsStream(resourceName)) {
            classSource = IOUtils.toString(classStream, Charset.defaultCharset());
        }

        String compiledClassName = resourceName.substring(0, resourceName.lastIndexOf('.')).replace('/', '.');
        try (InMemoryFileManager fileManager = compile(compiledClassName, classSource)) {
            Class<?> clazz = fileManager.loadCompiledClass(compiledClassName);
            Method main = clazz.getMethod("main");
            main.invoke(clazz);
        } catch (Throwable t) {
            fail("Compilation interferes with captured fields usage", t);
        }
    }

    @ParameterizedTest
    @MethodSource("linesInjectionArguments")
    public void testMethodLinesInjection(String resourceName,
                                         String className,
                                         String methodName,
                                         Class<?>[] methodParameterTypes,
                                         int expectedStart,
                                         int expectedEnd) throws Exception {
        String classSource;
        try (InputStream classStream = ClassLoader.getSystemResourceAsStream(resourceName)) {
            classSource = IOUtils.toString(classStream, Charset.defaultCharset());
        }

        String compiledClassName = resourceName.substring(0, resourceName.lastIndexOf('.')).replace('/', '.');
        try (InMemoryFileManager fileManager = compile(compiledClassName, classSource)) {
            Class<?> clazz = fileManager.loadCompiledClass(className);
            Method method = clazz.getDeclaredMethod(methodName, methodParameterTypes);
            int startLine = CompilerUtils.getStartLine(method);
            int endLine = CompilerUtils.getEndLine(method);
            Assertions.assertEquals(expectedStart, startLine);
            Assertions.assertEquals(expectedEnd, endLine);
        }
    }

    private static Stream<Arguments> linesInjectionArguments() {
        return Stream.of(
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "regularMethod", new Class[0], 4, 6),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "oneLineMethod", new Class[0], 8, 8),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "splitDefinitionMethod", new Class[0], 10, 14),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "argsLineBreakMethod", new Class[]{int.class, int.class, int.class}, 16, 20),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "privateMethod", new Class[0], CompilerUtils.LINE_UNKNOWN, CompilerUtils.LINE_UNKNOWN),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "defaultMethod", new Class[0], CompilerUtils.LINE_UNKNOWN, CompilerUtils.LINE_UNKNOWN),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "staticFinalMethod", new Class[0], 30, 32),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "abstractMethod", new Class[0], 34, 34),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "annotatedMethod", new Class[0], 36, 40),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test$1", "run", new Class[0], 44, 46),
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "lambda$lambdaMethod$0", new Class[0], CompilerUtils.LINE_UNKNOWN, CompilerUtils.LINE_UNKNOWN), // lines unknown since lambda method is private
                Arguments.of("datadog/compiler/Test.java", "datadog.compiler.Test",   "commentedMethod", new Class[0], 58, 60) // we cannot establish correspondence between the method and the comment, so only actual method lines are considered here
        );
    }

    @Test
    public void testMethodAnnotationDisabled() throws Exception {
        String resourceName = "datadog/compiler/Test.java";
        String methodName = "regularMethod";
        Class<?>[] methodParameterTypes = new Class[0];

        String classSource;
        try (InputStream classStream = ClassLoader.getSystemResourceAsStream(resourceName)) {
            classSource = IOUtils.toString(classStream, Charset.defaultCharset());
        }

        String compiledClassName = resourceName.substring(0, resourceName.lastIndexOf('.')).replace('/', '.');
        try (InMemoryFileManager fileManager = compile(compiledClassName, classSource, DatadogCompilerPlugin.DISABLE_METHOD_ANNOTATION)) {
            Class<?> clazz = fileManager.loadCompiledClass(compiledClassName);
            Method method = clazz.getDeclaredMethod(methodName, methodParameterTypes);
            int startLine = CompilerUtils.getStartLine(method);
            int endLine = CompilerUtils.getEndLine(method);
            Assertions.assertEquals(CompilerUtils.LINE_UNKNOWN, startLine);
            Assertions.assertEquals(CompilerUtils.LINE_UNKNOWN, endLine);
        }
    }

    private InMemoryFileManager compile(String className, String classSource, String... args) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(null, null, null);
        InMemoryFileManager fileManager = new InMemoryFileManager(standardFileManager);

        StringWriter output = new StringWriter();
        List<String> arguments = new ArrayList<>();
        arguments.add("-classpath");
        arguments.add(System.getProperty("java.class.path"));
        arguments.add("-Xplugin:" + DatadogCompilerPlugin.NAME + " " + String.join(" ", args));

        List<InMemorySourceFile> compilationUnits = singletonList(new InMemorySourceFile(className, classSource));
        JavaCompiler.CompilationTask task = compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);
        task.call();

        try {
            if (fileManager.loadCompiledClass(className) == null) {
                throw new IllegalStateException("Class " + className + " was not compiled. Compilation ouput:\n" + output);
            } else {
                return fileManager;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error while compiling class " + className + " . Compilation ouput:\n" + output);
        }
    }
}
