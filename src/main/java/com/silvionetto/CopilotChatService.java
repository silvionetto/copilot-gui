package com.silvionetto;

import com.github.copilot.CopilotClient;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.ModelInfo;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public final class CopilotChatService {

    private volatile String selectedModel;

    public String send(String prompt) {
        var options = new CopilotClientOptions().setCliPath(resolveCliPath());
        try (var client = new CopilotClient(options)) {
            client.start().get();

            var sessionConfig = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
            if (selectedModel != null && !selectedModel.isBlank()) {
                sessionConfig.setModel(selectedModel);
            }

            try (var session = client.createSession(sessionConfig).get()) {
                var response = new AtomicReference<String>();
                session.on(AssistantMessageEvent.class, message -> response.set(message.getData().content()));
                session.sendAndWait(new MessageOptions().setPrompt(prompt)).get();

                var content = response.get();
                if (content == null || content.isBlank()) {
                    throw new IllegalStateException("Copilot returned an empty response.");
                }
                return content;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The Copilot request was interrupted.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Copilot could not complete the request.", exception.getCause());
        }
    }

    /**
     * Fetches the models available to the authenticated Copilot account,
     * mirroring the CLI's {@code /model} slash command.
     */
    public List<ModelInfo> listModels() {
        var options = new CopilotClientOptions().setCliPath(resolveCliPath());
        try (var client = new CopilotClient(options)) {
            client.start().get();
            return client.listModels().get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The model list request was interrupted.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Copilot could not list available models.", exception.getCause());
        }
    }

    /**
     * Returns the model id used for subsequent requests, or {@code null} if
     * the CLI's default model applies.
     */
    public String getSelectedModel() {
        return selectedModel;
    }

    /**
     * Sets the model id to use for subsequent requests. Pass {@code null} or
     * blank to fall back to the CLI's default model.
     */
    public void setSelectedModel(String selectedModel) {
        this.selectedModel = (selectedModel == null || selectedModel.isBlank()) ? null : selectedModel;
    }

    /**
     * Resolves the path to the Copilot CLI executable. The SDK defaults to
     * launching "copilot" and relying on the process's PATH, but a JVM started
     * from Gradle/an IDE often does not inherit the PATH entries that a user's
     * shell has (e.g. the npm global bin directory), which causes the CLI
     * process to fail immediately with a closed-pipe error. This checks an
     * optional override and well-known npm install locations before falling
     * back to relying on PATH.
     */
    private static String resolveCliPath() {
        String override = System.getenv("COPILOT_CLI_PATH");
        if (override != null && !override.isBlank()) {
            return override;
        }

        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (isWindows) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                Path candidate = Path.of(appData, "npm", "copilot.cmd");
                if (Files.isRegularFile(candidate)) {
                    return candidate.toString();
                }
            }
        } else {
            String home = System.getProperty("user.home");
            if (home != null) {
                Path candidate = Path.of(home, ".npm-global", "bin", "copilot");
                if (Files.isRegularFile(candidate)) {
                    return candidate.toString();
                }
            }
        }

        return "copilot";
    }
}
