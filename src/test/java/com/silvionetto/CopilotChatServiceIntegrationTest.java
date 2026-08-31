package com.silvionetto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "COPILOT_INTEGRATION_TEST", matches = "true")
class CopilotChatServiceIntegrationTest {

    @Test
    void sendsPromptToAuthenticatedCopilotCli() {
        String response = new CopilotChatService().send(
                "Reply with exactly COPILOT_INTEGRATION_OK and nothing else."
        );

        assertEquals("COPILOT_INTEGRATION_OK", response.trim());
    }
}
