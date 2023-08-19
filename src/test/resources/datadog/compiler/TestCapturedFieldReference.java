package datadog.compiler;

public class TestCapturedFieldReference {
    public static void main() {
        Object o = new Object();
        Runnable r = new Runnable() { public void run() { System.out.println(o); } };
        r.run();
    }
}
