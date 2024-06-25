package datadog.compiler;

import java.util.concurrent.Executor;
import org.burningwave.core.assembler.StaticComponentContainer;
import org.burningwave.core.function.ThrowingRunnable;

public class CompilerModuleOpener {

    /**
     * Exports {@code jdk.compiler} module to unnamed modules.
     * <p>
     * The code of the plugin lives in an unnamed module.
     * It needs to access the API from the {@code jdk.compiler} module.
     * <p>
     * There are a few ways of ensuring this:
     * <ul>
     *     <li>Make the plugin users declare <a href="https://openjdk.org/jeps/396">declare --add-exports</a>.</li>
     *     <li>Move the plugin classes to a named module and specify that the module requires access to JDK compiler (it looks like some of the APIs we need to access are not exported, so this solution is unlikely to work).</li>
     *     <li>Use the hack implemented in this method (it modifies an internal field in a core JDK class with the use of reflection, to add the necessary {@code --add-exports} as if they were provided in the javac command).</li>
     * </ul>
     */
    public static void setup() {
        try {
            if (StaticComponentContainer.JVMInfo.getVersion() >= 16) {
                StaticComponentContainer.Modules.exportToAllUnnamed("jdk.compiler");
            }
            // force classes to be loaded: https://github.com/burningwave/core/discussions/15
            ThrowingRunnable.class.getClassLoader();
            Executor.class.getClassLoader();
        } catch (Throwable e) {
            // ignore
        }
    }
}
