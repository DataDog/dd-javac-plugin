package datadog.compiler.utils;

import datadog.compiler.annotations.MethodLines;
import datadog.compiler.annotations.SourcePath;
import java.lang.reflect.Method;

public class CompilerUtils {

    public static final int LINE_UNKNOWN = -1;

    /**
     * Returns path to class source file (injected by Datadog Java compiler plugin)
     *
     * @param clazz The class to get the source file path for
     * @return The absolute path to the source code of the provided class
     */
    public static String getSourcePath(Class<?> clazz) {
        SourcePath sourcePathAnnotation = clazz.getAnnotation(SourcePath.class);
        if (sourcePathAnnotation != null) {
            return sourcePathAnnotation.value();
        }

        if (clazz.isAnonymousClass()) {
            try {
                Class<?> enclosingClass = clazz.getEnclosingClass();
                if (enclosingClass != null) {
                    return getSourcePath(enclosingClass);
                }
            } catch (Exception e) {
                // ignored
            }
        }

        return null;
    }

    /**
     * Returns start line of the provided method (method name, modifiers and annotations are taken into account).
     *
     * @param method The method
     * @return Start line of the method or {@link CompilerUtils#LINE_UNKNOWN} if the line cannot be determined
     */
    public static int getStartLine(Method method) {
        MethodLines methodLines = method.getAnnotation(MethodLines.class);
        return methodLines != null ? methodLines.start() : LINE_UNKNOWN;
    }

    /**
     * Returns end line of the provided method.
     *
     * @param method The method
     * @return End line of the method or {@link CompilerUtils#LINE_UNKNOWN} if the line cannot be determined
     */
    public static int getEndLine(Method method) {
        MethodLines methodLines = method.getAnnotation(MethodLines.class);
        return methodLines != null ? methodLines.end() : LINE_UNKNOWN;
    }
}
