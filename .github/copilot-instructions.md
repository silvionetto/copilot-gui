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
- The Swing UI (`Main.java`) uses plain AWT/Swing layout managers only (`BorderLayout`, `BoxLayout`, `GridBagLayout`). The JGoodies Forms dependency was removed; do not reintroduce it or other third-party layout libraries without an explicit request.

## Lessons learned (Copilot Java SDK + Swing chat UI)

- **CLI subprocess PATH resolution**: `CopilotClient` (from `com.github:copilot-sdk-java`) spawns the `copilot` executable as a child process and, on Windows, wraps it with `cmd /c copilot ...`. If the JVM's process environment doesn't include the directory containing `copilot`/`copilot.cmd` (common when launching via Gradle or an IDE run configuration, since it may not inherit the interactive shell's `PATH`), the subprocess exits almost instantly and JSON-RPC calls fail with `java.io.IOException: The pipe is being closed`. Fix: resolve an explicit CLI path via `CopilotClientOptions().setCliPath(...)` — `CopilotChatService.resolveCliPath()` checks a `COPILOT_CLI_PATH` env var override, then well-known npm global install locations (`%APPDATA%\npm\copilot.cmd` on Windows, `~/.npm-global/bin/copilot` on Unix), before falling back to bare `"copilot"`.
- **Sizing a word-wrapped `JTextArea` to its content**: `JTextArea`'s `rows`/`columns` constructor args are measured in character units and estimating row count from `message.length() / columns` does not reliably match real word-wrap line breaks — it can under-count and clip text to what looks like a single line. The reliable approach is the standard two-pass Swing technique: call `setSize(new Dimension(fixedWidthPx, Integer.MAX_VALUE / 2))` on the text area, then call `getPreferredSize()` (which now accounts for real wrapping at that width), and lock in `setPreferredSize(new Dimension(fixedWidthPx, measuredHeight))`. See `Main.sizeMessageArea()`.
- **Chat bubble width**: to make short messages shrink-to-fit and long messages wrap at a max width (instead of always stretching to the full container width), measure the natural single-line width with `FontMetrics.stringWidth(...)` (per explicit `\n`-separated line) and clamp it between a min/max pixel bound before applying the two-pass sizing above.
- **Auto-scroll a `JScrollPane` chat log**: after appending a new message component and calling `revalidate()`/`repaint()`, scroll to the bottom via `SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()))` — doing it synchronously before layout has recalculated the new preferred size does not scroll far enough.
- **Multi-line composer with Enter-to-send**: bind `KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)` to a custom "send" `Action` in the `JTextArea`'s input/action maps, and bind `Shift+Enter` (`InputEvent.SHIFT_DOWN_MASK`) to `DefaultEditorKit.InsertBreakAction` so it inserts a newline instead of triggering send.
- **Verifying Swing sizing logic without a full GUI run**: a throwaway `public static void main` class (temporarily swapped in as the `application.mainClass` in `build.gradle`, then reverted) run via `gradlew run` can print computed `Dimension`/layout values headlessly — useful for validating text-sizing math without needing to visually inspect the window.


## Repository notes

- No project README, CONTRIBUTING guide, or AI instruction files were found in the repository root, so the guidance above reflects the actual Gradle and source layout in the codebase.
- Build artifacts and IDE metadata are intentionally ignored via `.gitignore` and should not be committed.
