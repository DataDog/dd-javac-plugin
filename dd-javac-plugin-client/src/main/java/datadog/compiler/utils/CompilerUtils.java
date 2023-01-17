package datadog.compiler.utils;

import datadog.compiler.annotations.SourcePath;

public class CompilerUtils {

    /**
     * Returns path to class source file (injected by Datadog Java compiler plugin)
     *
     * @param clazz The class to get the source file path for
     * @return The absolute path to the source code of the provided class
     */
    public static String getSourcePath(Class<?> clazz) {
        SourcePath sourcePathAnnotation = clazz.getAnnotation(SourcePath.class);
        return sourcePathAnnotation != null ? sourcePathAnnotation.value() : null;
    }
}
