package datadog.compiler;

import static java.util.Collections.singletonList;

import datadog.compiler.utils.CompilerUtils;
import java.io.StringWriter;
import java.util.Arrays;
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

    private InMemoryFileManager compile(String className, String classSource) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(null, null, null);
        InMemoryFileManager fileManager = new InMemoryFileManager(standardFileManager);

        StringWriter output = new StringWriter();
        List<String> arguments =
                Arrays.asList(
                        "-classpath",
                        System.getProperty("java.class.path"),
                        "-Xplugin:" + DatadogCompilerPlugin.NAME);
        List<InMemorySourceFile> compilationUnits =
                singletonList(new InMemorySourceFile(className, classSource));

        JavaCompiler.CompilationTask task =
                compiler.getTask(output, fileManager, null, arguments, null, compilationUnits);
        task.call();

        return fileManager;
    }
}
