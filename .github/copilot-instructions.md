# Copilot instructions for this repository

## Project overview

This repository is a small Java Gradle project rooted at the repository root. It is a single-module application with the package namespace `com.silvionetto`, and the current entry point is `src/main/java/com/silvionetto/Main.java`.

There is no multi-service or framework-heavy architecture in this repo today: the codebase is intentionally minimal, with Java sources under `src/main/java` and tests under `src/test/java` when they are added.

## Build, test, and lint commands

Use the Gradle wrapper from the repository root:

```bash
# Compile and package the app
./gradlew build

# Run the full test suite
./gradlew test

# Run a single test class (replace with your test class)
./gradlew test --tests "com.silvionetto.MainTest"

# Run a single test method (replace with your test method)
./gradlew test --tests "com.silvionetto.MainTest.testSomething"
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

There is no dedicated lint task configured in `build.gradle`; validation is primarily via Gradle `build` and `test` tasks.

## High-level architecture

- The project is a plain Java application, not a Spring Boot/app framework project.
- `build.gradle` defines a Java plugin, Maven Central dependencies, and JUnit Jupiter for tests.
- Gradle wrapper version is pinned to 9.6.0 in `gradle/wrapper/gradle-wrapper.properties`.
- The app entry point is a simple `main` method in `Main`, so feature work typically adds new classes in the `com.silvionetto` package and uses standard Java patterns rather than framework-specific conventions.
- The repo is organized around a single module, so changes usually stay local to `src/main/java` and their corresponding tests in `src/test/java`.

## Key conventions

- Keep package names under `com.silvionetto` unless a broader naming structure is intentionally introduced.
- Prefer standard Java naming conventions (`UpperCamelCase` classes, `lowerCamelCase` methods/fields).
- Use the Gradle wrapper instead of a system-installed Gradle when running builds or tests.
- The repository is minimal and dependency-light; avoid adding new frameworks or build systems unless the task clearly requires them.
- When adding tests, use JUnit Jupiter (`org.junit.jupiter`) with the existing Gradle setup in `build.gradle`.
- Treat `src/main/java` as the application source root and `src/test/java` as the test source root.

## Repository notes

- No project README, CONTRIBUTING guide, or AI instruction files were found in the repository root, so the guidance above reflects the actual Gradle and source layout in the codebase.
- Build artifacts and IDE metadata are intentionally ignored via `.gitignore` and should not be committed.
