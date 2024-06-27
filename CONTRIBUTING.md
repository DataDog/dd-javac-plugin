# Contributing

Pull requests for bug fixes are welcome, but before submitting new features or changes to current functionality [open an issue](https://github.com/DataDog/dd-javac-plugin/issues/new)
and discuss your ideas or propose the changes you wish to make. After a resolution is reached a PR can be submitted for review.

## Requirements

Building the project requires Java 8 and Java 11 to be installed.

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

`java11` module needs JDK 11 set as its module SDK.
