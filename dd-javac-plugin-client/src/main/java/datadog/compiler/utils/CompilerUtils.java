package datadog.compiler.utils;

import datadog.compiler.annotations.MethodLines;
import datadog.compiler.annotations.SourcePath;
import java.lang.reflect.Executable;

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
     * Returns start line of the provided method or constructor (method name, modifiers and annotations are taken into account).
     *
     * @param executable Method or constructor
     * @return Start line of the method or {@link CompilerUtils#LINE_UNKNOWN} if the line cannot be determined
     */
    public static int getStartLine(Executable executable) {
        MethodLines methodLines = executable.getAnnotation(MethodLines.class);
        return methodLines != null ? methodLines.start() : LINE_UNKNOWN;
    }

    /**
     * Returns end line of the provided method or constructor.
     *
     * @param executable Method or constructor
     * @return End line of the method or {@link CompilerUtils#LINE_UNKNOWN} if the line cannot be determined
     */
    public static int getEndLine(Executable executable) {
        MethodLines methodLines = executable.getAnnotation(MethodLines.class);
        return methodLines != null ? methodLines.end() : LINE_UNKNOWN;
    }
}
