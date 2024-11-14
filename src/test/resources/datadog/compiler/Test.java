package datadog.compiler;

public abstract class Test {
    public void regularMethod() {
        // no op
    }

    public void oneLineMethod() {}

    public
    void
    splitDefinitionMethod() {
        // no op
    }

    public void argsLineBreakMethod(int argOne,
                                    int argTwo,
                                    int argThree) {
        // no op
    }

    private void privateMethod() {
        // no op
    }

    void defaultMethod() {
        // no op
    }

    public static final void staticFinalMethod() {
        // no op
    }

    public abstract void abstractMethod();

    @Deprecated
    @SuppressWarnings({ "arg1", "arg2" })
    public void annotatedMethod() {
        // no op
    }

    public void anonymousClassMethod() {
        Runnable r = new Runnable() {
            public void run() {
                // no op
            }
        };
    }

    public void lambdaMethod() {
        Runnable r = () -> {};
    }

    /**
     * This is
     * a multi-line comment
     */
    public void commentedMethod() {
        // no op
    }

    public static final class InnerClass {}

    @Deprecated
    public Test() {
        // no op constructor
    }

    public
    class
    SplitDefinitionClass {
        // empty class
    }

    private class PrivateClass {
        // empty class
    }

    class DefaultClass {
        // empty class
    }

    public static final class StaticFinalClass {
        // empty class
    }

    @Deprecated
    public class AnnotatedClass {
        // empty class
    }

    /**
     * Multi line
     * comment
     */
    public class CommentedClass {
        // empty class
    }

    public interface TestInterface {
        // empty interface
    }

    public enum TestEnum {
        // empty enum
    }
}
