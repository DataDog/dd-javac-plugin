package datadog.compiler;

import com.sun.tools.javac.code.ModuleFinder;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.lang.reflect.Field;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

public class UnnamedModuleOpener {

    /**
     * Opens unnamed module (which contains the injected source path annotation)
     * to the other non-system modules that constitute or are referenced by the compilation unit.
     * <p>
     * The module is opened by programmatically modifying the {@code --add-reads} option.
     * <p>
     * The goal is to avoid compiler warnings saying that source path annotation class
     * cannot be found when referenced by a class in a different module
     */
    public static void open(Context context) {
        StringBuilder addReads = new StringBuilder();

        ModuleFinder moduleFinder = ModuleFinder.instance(context);
        List<Symbol.ModuleSymbol> allModules = moduleFinder.findAllModules();
        for (Symbol.ModuleSymbol module : allModules) {
            String systemModulesLocation = StandardLocation.SYSTEM_MODULES.name();
            boolean systemModule = module.classLocation.toString().startsWith(systemModulesLocation);
            if (!systemModule) {
                String moduleName = module.getQualifiedName().toString();
                appendOpenUnnamedModuleArgument(addReads, moduleName);
            }
        }

        String currentModuleName = getCurrentModuleName(context);
        if (currentModuleName != null) {
            appendOpenUnnamedModuleArgument(addReads, currentModuleName);
        }

        augmentAddReadsOption(context, addReads.toString());
    }

    private static void appendOpenUnnamedModuleArgument(StringBuilder addReads, String moduleName) {
        addReads.append(moduleName).append('=').append("ALL-UNNAMED").append('\0');
    }

    private static String getCurrentModuleName(Context context) {
        JavaFileManager javaFileManager = context.get(JavaFileManager.class);
        if (javaFileManager.hasLocation(StandardLocation.SOURCE_PATH)) {
            Names names = Names.instance(context);
            try {
                JavaFileObject currentModuleInfo = javaFileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, names.module_info.toString(), JavaFileObject.Kind.SOURCE);
                ModuleFinder moduleFinder = ModuleFinder.instance(context);
                Name currentModuleName = moduleFinder.moduleNameFromSourceReader.readModuleName(currentModuleInfo);
                return currentModuleName.toString();
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    private static void augmentAddReadsOption(Context context, String addReads) {
        Modules modules = Modules.instance(context);
        try {
            Field addReadsOptField = Modules.class.getDeclaredField("addReadsOpt");
            addReadsOptField.setAccessible(true);

            String currentValue = (String) addReadsOptField.get(modules);
            String augmentedValue = (currentValue != null ? currentValue + '\0' : "") + addReads;

            addReadsOptField.set(modules, augmentedValue);
        } catch (Exception e) {
            // ignore
        }
    }
}
