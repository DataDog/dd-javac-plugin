package datadog.compiler;

import java.util.concurrent.Executor;
import org.burningwave.core.assembler.StaticComponentContainer;
import org.burningwave.core.function.ThrowingRunnable;

/**
 * Isolates all burningwave references so that the JVM only loads burningwave classes when this
 * class is explicitly accessed. This prevents {@code StaticComponentContainer.<clinit>} from
 * running on JDK versions where it fails (e.g. JDK 26+).
 */
class BurningwaveModuleOpener {

    static void open() {
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
