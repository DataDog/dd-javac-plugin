package datadog.compiler;

import datadog.compiler.annotations.SourceLines;
import datadog.compiler.annotations.SourcePath;

@SourcePath("the-source-path")
@SourceLines(start = 1, end = 2)
public class TestAnnotated {

    @SourceLines(start = 1, end = 2)
    public void annotatedMethod() {
        // no op
    }
}
