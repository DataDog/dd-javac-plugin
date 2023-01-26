# Contributing

Pull requests for bug fixes are welcome, but before submitting new features or changes to current functionality [open an issue](https://github.com/DataDog/dd-javac-plugin/issues/new)
and discuss your ideas or propose the changes you wish to make. After a resolution is reached a PR can be submitted for review.

## Requirements

A JDK needs to be installed in order to run Gradle.
Minimum supported version is JDK 8.  

# Building

To build the project without running tests run:
```bash
./gradlew clean assemble
```

To build the entire project with tests run:
```bash
./gradlew clean build
```

## Intellij IDEA

In `Project Settings` -> `Modules` ensure that the following modules have JDK 8 set as their module SDK:
- `main`
- `test`
- `dd-javac-plugin-client/main`
- `dd-javac-plugin-client/test`

`java9` module needs JDK 9 set as their module SDK.
