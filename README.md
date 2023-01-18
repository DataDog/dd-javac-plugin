# Datadog Java Compiler Plugin

This is a Java compiler (`javac`) [plugin](https://openjdk.org/groups/compiler/processing-code.html#plugin) that
augments compiled classes with some additional data used by Datadog products.

List of things that the plugin does:

- Annotate every class with a `@SourcePath("...")` annotation that has the path to the class' source code
  (this data is being used by [Datadog's CI Visibility](https://www.datadoghq.com/product/ci-cd-monitoring/)).

## Configuration

The following conditions need to be satisfied in order for the plugin to work:

- the plugin and plugin-client JARs need to be added to the compiler's classpath
- `-Xplugin:DatadogCompilerPlugin` argument needs to be provided to the compiler
- for JDK 16 and newer versions, additional `--add-exports` flags are required due
  to [JEP 396: Strongly Encapsulate JDK Internals by Default](https://openjdk.org/jeps/396) (see below sections for more
  details)

If the configuration is successful, you should see the line `DatadogCompilerPlugin initialized` in your compiler's
output

> For now the augmentation is only useful in test classes, so you can limit configuration to javac invocations that
> compile test sources.

### Maven

Below is an example of how to specify annotation processor path and compiler arguments for maven compiler plugin.

```xml

<dependencies>
    <dependency>
        <groupId>com.datadoghq</groupId>
        <artifactId>dd-javac-plugin-client</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
<build>
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.5</version>
        <configuration>
            <annotationProcessorPaths>
                <annotationProcessorPath>
                    <groupId>com.datadoghq</groupId>
                    <artifactId>dd-javac-plugin</artifactId>
                    <version>1.0.0</version>
                </annotationProcessorPath>
            </annotationProcessorPaths>
            <testCompilerArgument>
                -Xplugin:DatadogCompilerPlugin
            </testCompilerArgument>
        </configuration>
    </plugin>
</plugins>
</build>
```

> Maven compiler plugin supports
> [annotationProcessorPaths](https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#annotationProcessorPaths)
> property starting with version 3.5.
> If you absolutely must use an older version, declare Datadog compiler plugin as a regular dependency in your project.

Additionally, if you are using JDK 16 or newer, add the following flags
to [.mvn/jvm.config](https://maven.apache.org/configure.html#mvn-jvm-config-file) in your project base directory:

```
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

### Gradle

Below is an example of how to configure the plugin for compiling test classes:

```groovy
dependencies {
    implementation 'com.datadoghq:dd-javac-plugin-client:1.0.0'
    testAnnotationProcessor 'com.datadoghq:dd-javac-plugin:1.0.0'
}

compileTestJava {
    options.compilerArgs << '-Xplugin:DatadogCompilerPlugin'
}
```

Additionally, if you are using JDK 16 or newer, add the following flags to your
[gradle.properties](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties)
file:

```
org.gradle.jvmargs=\
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED  \
--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

### Other

If you're using any other build system, just make sure to add the plugin and client JARs to the compiler's classpath,
and pass the plugin argument.
Below is an example for direct compiler invocation:

```shell
javac \
    -classpath dd-javac-plugin-client-1.0.0.jar:dd-javac-plugin-1.0.0.jar \
    -Xplugin:DatadogCompilerPlugin \
    <PATH_TO_SOURCES>
```

If you are using JDK 16 or newer, additional `--add-exports` flags should be provided:

```shell
javac \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED  \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
  -classpath dd-javac-plugin-client-1.0.0.jar:dd-javac-plugin-1.0.0.jar \
  -Xplugin:DatadogCompilerPlugin \
  <PATH_TO_SOURCES>
```

## Limitations

- Support is limited to `javac` (or any other compiler that knows how to work
  with [com.sun.source.util.Plugin](https://docs.oracle.com/javase/8/docs/jdk/api/javac/tree/com/sun/source/util/Plugin.html)).
  Eclipse JDT compiler support is [pending](https://bugs.eclipse.org/bugs/show_bug.cgi?id=574899).

- The plugin requires `javac` that comes with java 1.8 or above.