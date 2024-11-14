package datadog.compiler.utils;

import datadog.compiler.annotations.SourceLines;
import datadog.compiler.annotations.SourcePath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;

public class CompilerUtilsTest {

    private static final String TEST_CLASS_SOURCE_PATH = "/repo/src/package/TestClass.java";
    private static final int TEST_CLASS_SOURCE_LINES_START = 19;
    private static final int TEST_CLASS_SOURCE_LINES_END = 24;
    private static final int TEST_METHOD_SOURCE_LINES_START = 21;
    private static final int TEST_METHOD_SOURCE_LINES_END = 23;

    @SourcePath(TEST_CLASS_SOURCE_PATH)
    @SourceLines(start = TEST_CLASS_SOURCE_LINES_START, end = TEST_CLASS_SOURCE_LINES_END)
    private static final class TestClass {
        @SourceLines(start = TEST_METHOD_SOURCE_LINES_START, end = TEST_METHOD_SOURCE_LINES_END)
        public static void testMethod() {
            // no op
        }
    }

    @Test
    public void testSourcePathExtraction() {
        String sourcePath = CompilerUtils.getSourcePath(TestClass.class);
        Assertions.assertEquals(TEST_CLASS_SOURCE_PATH, sourcePath);
    }

    @Test
    public void testMethodLinesExtraction() throws Exception {
        Method method = TestClass.class.getDeclaredMethod("testMethod");
        int startLine = CompilerUtils.getStartLine(method);
        int endLine = CompilerUtils.getEndLine(method);
        Assertions.assertEquals(TEST_METHOD_SOURCE_LINES_START, startLine);
        Assertions.assertEquals(TEST_METHOD_SOURCE_LINES_END, endLine);
    }

    @Test
    public void testClassLinesExtraction() {
        int startLine = CompilerUtils.getStartLine(TestClass.class);
        int endLine = CompilerUtils.getEndLine(TestClass.class);
        Assertions.assertEquals(TEST_CLASS_SOURCE_LINES_START, startLine);
        Assertions.assertEquals(TEST_CLASS_SOURCE_LINES_END, endLine);
    }
}