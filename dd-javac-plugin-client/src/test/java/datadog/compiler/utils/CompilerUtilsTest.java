package datadog.compiler.utils;

import datadog.compiler.annotations.SourceLines;
import datadog.compiler.annotations.SourcePath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CompilerUtilsTest {

    private static final String TEST_CLASS_SOURCE_PATH = "/repo/src/package/TestClass.java";
    private static final int TEST_CLASS_SOURCE_LINES_START = 16;
    private static final int TEST_CLASS_SOURCE_LINES_END = 18;

    @SourcePath(TEST_CLASS_SOURCE_PATH)
    @SourceLines(start = TEST_CLASS_SOURCE_LINES_START, end = TEST_CLASS_SOURCE_LINES_END)
    private static final class TestClass {
    }

    @Test
    public void testSourcePathExtraction() {
        String sourcePath = CompilerUtils.getSourcePath(TestClass.class);
        Assertions.assertEquals(TEST_CLASS_SOURCE_PATH, sourcePath);
    }

    @Test
    public void testSourceLinesExtraction() {
        int startLine = CompilerUtils.getStartLine(TestClass.class);
        int endLine = CompilerUtils.getEndLine(TestClass.class);
        Assertions.assertEquals(TEST_CLASS_SOURCE_LINES_START, startLine);
        Assertions.assertEquals(TEST_CLASS_SOURCE_LINES_END, endLine);
    }
}