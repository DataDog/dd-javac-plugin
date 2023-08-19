# Datadog Java Compiler Plugin

This is a Java compiler (`javac`) [plugin](https://openjdk.org/groups/compiler/processing-code.html#plugin) that
augments compiled classes with some additional data used by Datadog products.

List of things that the plugin does:

- Annotate every class with a `@SourcePath("...")` annotation that has the path to the class' source code
- Annotate every public method with a `@MethodLines("...")` annotation that has method start and end lines (taking into account method modifiers and annotations)

## Configuration

The following conditions need to be satisfied in order for the plugin to work:

- the plugin JAR needs to be added to the compiler's annotation processor path
- the plugin-client JAR needs to be added to the project's classpath
- `-Xplugin:DatadogCompilerPlugin` argument needs to be provided to the compiler

If the configuration is successful, you should see the line `DatadogCompilerPlugin initialized` in your compiler's
output

### Maven

Add plugin-client JAR to the project's classpath:

```xml

<dependencies>
    <dependency>
        <groupId>com.datadoghq</groupId>
        <artifactId>dd-javac-plugin-client</artifactId>
        <version>0.1.6</version>
    </dependency>
</dependencies>
```

Add plugin JAR to the compiler's annotation processor path and pass the plugin argument:

```xml 

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
                        <version>0.1.6</version>
                    </annotationProcessorPath>
                </annotationProcessorPaths>
                <compilerArgs>
                    <arg>-Xplugin:DatadogCompilerPlugin</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

> Maven compiler plugin supports
> [annotationProcessorPaths](https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#annotationProcessorPaths)
> property starting with version 3.5.
> If you absolutely must use an older version, declare Datadog compiler plugin as a regular dependency in your project.

### Gradle

Add plugin-client JAR to the project's classpath, add plugin JAR to the compiler's annotation processor path and pass
the plugin argument:

```groovy
dependencies {
    implementation 'com.datadoghq:dd-javac-plugin-client:0.1.6'
    annotationProcessor 'com.datadoghq:dd-javac-plugin:0.1.6'
    testAnnotationProcessor 'com.datadoghq:dd-javac-plugin:0.1.6'
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs.add('-Xplugin:DatadogCompilerPlugin')
}
```

### Other

If you're using any other build system, just make sure to add the plugin 
(plus the plugin's transitive dependencies, there are not many)
and client JARs to the compiler's classpath, and pass the plugin argument.
Below is an example for direct compiler invocation:

```shell
javac \
    -classpath dd-javac-plugin-client-0.1.6.jar:dd-javac-plugin-0.1.6.jar:/org/burningwave/core/12.62.7/core-12.62.7.jar:/io/github/toolfactory/jvm-driver/9.4.3/jvm-driver-9.4.3.jar \
    -Xplugin:DatadogCompilerPlugin \
    <PATH_TO_SOURCES>
```

## Accessing additional compilation data in runtime

To access the injected information, use `CompilerUtils` class from the `dd-javac-plugin-client`:

```java
String sourcePath = CompilerUtils.getSourcePath(MyClass.class);

int startLine = CompilerUtils.getStartLine(method);
int endLine = CompilerUtils.getEndLine(method);
```

## Additional configuration

Specify `methodAnnotationDisabled` plugin argument if you want to disable annotating public methods.
The argument can be specified in `javac` command line after the `-Xplugin` clause.

## Limitations

- Support is limited to `javac` (or any other compiler that knows how to work
  with [com.sun.source.util.Plugin](https://docs.oracle.com/javase/8/docs/jdk/api/javac/tree/com/sun/source/util/Plugin.html)).
  Eclipse JDT compiler support is [pending](https://bugs.eclipse.org/bugs/show_bug.cgi?id=574899).

- The plugin requires `javac` that comes with java 1.8 or above.

### Using the plugin with Lombok
Additional steps need to be taken if the compiled code uses [Lombok](https://projectlombok.org/) annotations. 
Lombok Jar needs to be registered as an annotation processor, alongside the compiler plugin.

When using Maven:
```xml
<annotationProcessorPath>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <version>${lombokVersion}</version>
</annotationProcessorPath>
```

When using Gradle: 
```groovy
  annotationProcessor 'org.projectlombok:lombok:${lombokVersion}'
  testAnnotationProcessor 'org.projectlombok:lombok:${lombokVersion}'
```
