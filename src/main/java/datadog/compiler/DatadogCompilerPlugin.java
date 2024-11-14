package datadog.compiler;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.util.Arrays;
import java.util.Collection;

public class DatadogCompilerPlugin implements Plugin {

    static final String DISABLE_SOURCE_LINES_ANNOTATION = "disableSourceLinesAnnotation";

    static {
        CompilerModuleOpener.setup();
    }

    static final String NAME = "DatadogCompilerPlugin";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... strings) {
        if (task instanceof BasicJavacTask) {
            BasicJavacTask basicJavacTask = (BasicJavacTask) task;
            Context context = basicJavacTask.getContext();

            Collection<String> arguments = Arrays.asList(strings);
            boolean sourceLinesAnnotationDisabled = arguments.contains(DISABLE_SOURCE_LINES_ANNOTATION);
            task.addTaskListener(new DatadogTaskListener(basicJavacTask, sourceLinesAnnotationDisabled));

            Log.instance(context).printRawLines(Log.WriterKind.NOTICE, NAME + " initialized");
        }
    }

}
