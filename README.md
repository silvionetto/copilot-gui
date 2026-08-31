# copilot-gui

`copilot-gui` is a small Java desktop application built with Gradle. It uses plain Swing (`BorderLayout`/`BoxLayout`, no third-party layout library) and the GitHub Copilot SDK for Java to provide a one-shot Copilot chat window.

## Requirements

- Java 21 or newer
- A terminal
- GitHub Copilot CLI 1.0.55-5 or newer installed and available on `PATH`
- An authenticated GitHub Copilot CLI session

Install the CLI on Windows with WinGet:

```powershell
winget install GitHub.Copilot
```

On first launch, enter `/login` in Copilot CLI and complete the authentication flow.

The Java SDK uses the CLI's existing authentication and its default model. The SDK is responsible for starting and stopping the CLI runtime for each request.

> **Note:** the SDK spawns `copilot` as a subprocess and expects it on the process `PATH`. A JVM launched from Gradle or an IDE run configuration does not always inherit the same `PATH` as your interactive shell (e.g. an npm-global install directory), which causes the CLI subprocess to exit immediately with a `The pipe is being closed` error. `CopilotChatService` works around this by checking, in order: a `COPILOT_CLI_PATH` environment variable override, well-known npm global install locations (`%APPDATA%\npm\copilot.cmd` on Windows, `~/.npm-global/bin/copilot` elsewhere), then falling back to plain `copilot` on `PATH`. If you hit this error, set `COPILOT_CLI_PATH` to the full path reported by `where copilot` / `which copilot`.

## Run

From the repository root:

```bash
./gradlew run
```

On Windows:

```powershell
gradlew.bat run
```

## Build

```bash
./gradlew build
```

## Copilot integration test

The regular test suite does not contact Copilot. To run the opt-in integration test, make sure Copilot CLI is installed, authenticated, and available on `PATH`, then use PowerShell:

```powershell
$env:COPILOT_INTEGRATION_TEST = "true"
.\gradlew.bat test --tests "com.silvionetto.CopilotChatServiceIntegrationTest"
```

The test sends a deterministic prompt through the real Java SDK and expects the exact response `COPILOT_INTEGRATION_OK`.

## Project layout

- `src/main/java/com/silvionetto/Main.java` — Swing UI (plain `BorderLayout`/`BoxLayout`, no layout library)
- `src/main/java/com/silvionetto/CopilotChatService.java` — wraps the Copilot Java SDK, including CLI path resolution
- `build.gradle` — Gradle build and dependencies
