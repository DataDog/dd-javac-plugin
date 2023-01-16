package datadog.compiler.utils;

import datadog.compiler.annotations.SourcePath;

public class CompilerUtils {

    /**
     * Returns path to class source file (injected by Datadog Java compiler plugin)
     */
    public static String getSourcePath(Class<?> c) {
        SourcePath sourcePathAnnotation = c.getAnnotation(SourcePath.class);
        return sourcePathAnnotation != null ? sourcePathAnnotation.value() : null;
    }
}
