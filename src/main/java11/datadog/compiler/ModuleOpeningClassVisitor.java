package datadog.compiler;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.util.Context;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Opens unnamed module (which contains the injected source path annotation)
 * to the other non-system modules that constitute the compilation unit.
 * <p>
 * The module is opened by programmatically modifying the {@code --add-reads} option.
 * <p>
 * The goal is to avoid compiler warnings saying that source path annotation class
 * cannot be found when referenced by a class in a different module
 * (some projects are set up in a way that cause build failure whenever a compiler warning is encountered).
 */
public class ModuleOpeningClassVisitor extends TreeScanner<Void, Void> {

    private static final ConcurrentMap<String, Boolean> PROCESSED_MODULES = new ConcurrentHashMap<>();

    private final Modules modules;

    public ModuleOpeningClassVisitor(Context context) {
        modules = Modules.instance(context);
    }

    @Override
    public Void visitModule(ModuleTree node, Void unused) {
        ExpressionTree nodeName = node.getName();
        PROCESSED_MODULES.computeIfAbsent(nodeName.toString(), this::augmentAddReadsOption);
        return super.visitModule(node, unused);
    }

    private boolean augmentAddReadsOption(String moduleName) {
        try {
            Field addReadsOptField = Modules.class.getDeclaredField("addReadsOpt");
            addReadsOptField.setAccessible(true);

            String currentValue = (String) addReadsOptField.get(modules);
            String newValue = (currentValue != null ? currentValue + '\0' : "") + String.format("%s=ALL-UNNAMED", moduleName);

            addReadsOptField.set(modules, newValue);
        } catch (Exception e) {
            // ignore
        }
        return true;
    }
}
