package datadog.compiler;

import datadog.compiler.annotations.MethodLines;
import datadog.compiler.annotations.SourcePath;

@SourcePath("the-source-path")
public class TestAnnotated {

    @MethodLines(start = 1, end = 2)
    public void annotatedMethod() {
        // no op
    }
}
