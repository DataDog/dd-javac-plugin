package datadog.compiler;

import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.util.Context;

public class ModuleOpeningClassVisitor extends TreeScanner<Void, Void> {

    public ModuleOpeningClassVisitor(Context context) {
        // this is a stub used only for JDK 8. It does nothing. See corresponding class in JDK 9+ sources.
    }

}
