package datadog.compiler.utils;

import datadog.compiler.annotations.SourcePath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CompilerUtilsTest {

    private static final String TEST_CLASS_SOURCE_PATH = "/repo/src/package/TestClass.java";

    @SourcePath(TEST_CLASS_SOURCE_PATH)
    private static final class TestClass {
    }

    @Test
    public void testSourcePathExtraction() {
        String sourcePath = CompilerUtils.getSourcePath(TestClass.class);
        Assertions.assertEquals(TEST_CLASS_SOURCE_PATH, sourcePath);
    }

}