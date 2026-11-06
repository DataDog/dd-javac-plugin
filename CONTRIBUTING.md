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

## Releasing

The release version comes from the Git tag. `version` in `gradle.properties` is only used for local builds.

1. Make sure the `master` commit you want to release has passed CI.
2. Create a signed tag on that commit and push it. Pushing tags requires bypass permission on the organization tag ruleset.
   ```bash
   git tag -s 0.2.5 -m 0.2.5 <commit>
   git push origin 0.2.5
   ```
3. The `Create Release` GitHub workflow creates the GitHub release and attaches the all-in-one JAR.
4. In the GitLab pipeline for the tag, run the manual `deploy_to_sonatype` job. It builds, signs and uploads the artifacts to the Maven Central staging area.
5. Publish the deployment in the [Maven Central portal](https://central.sonatype.com/publishing/deployments). The artifacts can take a few hours to appear on Maven Central.

## Intellij IDEA

In `Project Settings` -> `Modules` ensure that the following modules have JDK 8 set as their module SDK:
- `main`
- `test`
- `dd-javac-plugin-client/main`
- `dd-javac-plugin-client/test`

`java11` module needs JDK 11 set as its module SDK.
