package datadog.compiler;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.fail;

import datadog.compiler.utils.CompilerUtils;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DatadogCompilerPluginTest {

    @Test
    public void testSourcePathInjection() throws Exception {
        String testClassName = "datadog.compiler.Test";
        String testClassSource = "package datadog.compiler; public class Test {}";
        try (InMemoryFileManager fileManager = compile(testClassName, testClassSource)) {
            Class<?> clazz = fileManager.loadCompiledClass(testClassName);
            String sourcePath = CompilerUtils.getSourcePath(clazz);
            Assertions.assertEquals(InMemorySourceFile.sourcePath(testClassName), sourcePath);
        }
    }

    @Test
    public void testInnerClassSourcePathInjection() throws Exception {
        String testClassName = "datadog.compiler.Test";
        String testClassSource =
                "package datadog.compiler; public class Test { public static final class Inner {} }";
        try (InMemoryFileManager fileManager = compile(testClassName, testClassSource)) {
            Class<?> innerClazz = fileManager.loadCompiledClass(testClassName + "$Inner");
            String sourcePath = CompilerUtils.getSourcePath(innerClazz);
            Assertions.assertEquals(InMemorySourceFile.sourcePath(testClassName), sourcePath);
        }
    }

    @Test
    public void testAnonymousClassSourcePathInjection() throws Exception {
        String testClassName = "datadog.compiler.Test";
        String testClassSource =
                "package datadog.compiler; public class Test { " +
                        "    public static void main() { " +
                        "        Runnable r = new Runnable() { public void run() {} }; " +
                        "    } " +
                        "}";
        try (InMemoryFileManager fileManager = compile(testClassName, testClassSource)) {
            Class<?> anonymousClazz = fileManager.loadCompiledClass(testClassName + "$1");
            String sourcePath = CompilerUtils.getSourcePath(anonymousClazz);
            Assertions.assertEquals(InMemorySourceFile.sourcePath(testClassName), sourcePath);
        }
    }

    @Test
    public void testSourcePathInjectionPreservesAnnotations() throws Exception {
        String testClassName = "datadog.compiler.Test";
        String testClassSource = "package datadog.compiler; @Deprecated public class Test {}";
        try (InMemoryFileManager fileManager = compile(testClassName, testClassSource)) {
            Class<?> clazz = fileManager.loadCompiledClass(testClassName);

            String sourcePath = CompilerUtils.getSourcePath(clazz);
            Assertions.assertEquals(InMemorySourceFile.sourcePath(testClassName), sourcePath,
                    "source path was not injected");

            // existing annotation was preserved
            Assertions.assertNotNull(clazz.getAnnotation(Deprecated.class),
                    "existing annotation was not preserved");
        }
    }

    @Test
    public void testAnonymousClassCapturedFieldReference() {
        String testClassName = "datadog.compiler.Test";
        String testClassSource =
                "package datadog.compiler; public class Test { " +
                        "    public static void main() { " +
                        "        Object o = new Object(); " +
                        "        Runnable r = new Runnable() { public void run() { System.out.println(o); } }; " +
                        "        r.run();" +
                        "    } " +
                        "}";
        try (InMemoryFileManager fileManager = compile(testClassName, testClassSource)) {
            Class<?> testClass = fileManager.loadCompiledClass(testClassName);
            Method main = testClass.getMethod("main");
            main.invoke(testClass);
        } catch (Throwable t) {
            fail("Compilation interferes with captured fields usage", t);
        }
    }

    @Test
    public void testAnnotatingAnnotation() throws Exception {
        String testClassName = "datadog.compiler.Test";
        String testClassSource =
                "package datadog.compiler;\n" +
                        "import java.lang.annotation.ElementType;\n" +
                        "import java.lang.annotation.Retention;\n" +
                        "import java.lang.annotation.RetentionPolicy;\n" +
                        "import java.lang.annotation.Target;\n" +
                        "@Retention(RetentionPolicy.CLASS)\n" +
                        "@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})\n" +
                        "public @interface Test {\n" +
                        "    String reason();\n" +
                        "}";
        try (InMemoryFileManager fileManager = compile(testClassName, testClassSource)) {
            Class<?> clazz = fileManager.loadCompiledClass(testClassName);
            String sourcePath = CompilerUtils.getSourcePath(clazz);
            Assertions.assertEquals(InMemorySourceFile.sourcePath(testClassName), sourcePath);
        }
    }

    @Test
    public void testSkipAnnotatingAnnotation() throws Exception {
        String testClassName = "datadog.compiler.Test";
        String testClassSource =
                "package datadog.compiler;\n" +
                        "import java.lang.annotation.ElementType;\n" +
                        "import java.lang.annotation.Retention;\n" +
                        "import java.lang.annotation.RetentionPolicy;\n" +
                        "import java.lang.annotation.Target;\n" +
                        "@Retention(RetentionPolicy.CLASS)\n" +
                        "@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})\n" +
                        "public @interface Test {\n" +
                        "    String reason();\n" +
                        "}";
        try (InMemoryFileManager fileManager = compile(testClassName, testClassSource, DatadogCompilerPlugin.SKIP_ANNOTATIONS_ARGUMENT)) {
            Class<?> clazz = fileManager.loadCompiledClass(testClassName);
            String sourcePath = CompilerUtils.getSourcePath(clazz);
            Assertions.assertEquals(null, sourcePath);
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

        List<InMemorySourceFile> compilationUnits =
                singletonList(new InMemorySourceFile(className, classSource));

        JavaCompiler.CompilationTask task =
                compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);
        task.call();

        return fileManager;
    }
}
