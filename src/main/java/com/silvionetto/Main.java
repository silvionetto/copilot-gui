package com.silvionetto;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.DefaultEditorKit;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import com.github.copilot.rpc.ModelInfo;

public class Main {

    private static final Color APP_BACKGROUND = new Color(0xF3, 0xF6, 0xFB);
    private static final Color SURFACE_BACKGROUND = Color.WHITE;
    private static final Color HEADER_FOREGROUND = new Color(0x1F, 0x29, 0x37);
    private static final Color SECONDARY_FOREGROUND = new Color(0x6B, 0x72, 0x80);
    private static final Color CONTROL_FOREGROUND = new Color(0x1F, 0x29, 0x37);
    private static final Color BUTTON_BACKGROUND = new Color(0xE5, 0xED, 0xF8);
    private static final Color USER_BUBBLE = new Color(0xD9, 0xEC, 0xFF);
    private static final Color ASSISTANT_BUBBLE = new Color(0xE9, 0xF7, 0xEF);
    private static final Color SYSTEM_BUBBLE = new Color(0xF3, 0xF0, 0xFA);
    private static final Color COMPOSER_BACKGROUND = new Color(0xF9, 0xFA, 0xFB);
    private static final String HELP_TEXT = """
            Supported commands:
            /help - show this message
            /model [id] - show the current model, list available models, or switch models
            /clear - clear the conversation
            /login, /feedback, /experimental - not available in the GUI yet; run `copilot` in a terminal to use them""";
    private static final Border CARD_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xD8, 0xE0, 0xEA)),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
    );
    private static final int BUBBLE_MAX_WIDTH_PX = 420;
    private static final int BUBBLE_MIN_WIDTH_PX = 60;

    private final CopilotChatService copilot = new CopilotChatService();
    private JPanel messages;
    private JScrollPane messagesScrollPane;
    private JTextArea input;
    private JButton sendButton;
    private JLabel hint;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().showWindow());
    }

    private void showWindow() {
        JFrame frame = new JFrame("Copilot Chat UI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(createContent());
        frame.setPreferredSize(new Dimension(720, 540));
        frame.pack();
        frame.setMinimumSize(new Dimension(680, 480));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        setRequestInProgress(false);
    }

    private JComponent createContent() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 12));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rootPanel.setOpaque(true);
        rootPanel.setBackground(APP_BACKGROUND);

        rootPanel.add(createHeader(), BorderLayout.NORTH);
        rootPanel.add(createConversationCard(), BorderLayout.CENTER);
        rootPanel.add(createComposer(), BorderLayout.SOUTH);

        return rootPanel;
    }

    private JComponent createHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Copilot");
        title.setFont(title.getFont().deriveFont(24f));
        title.setForeground(HEADER_FOREGROUND);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Ask Copilot a question from your desktop");
        subtitle.setForeground(SECONDARY_FOREGROUND);
        subtitle.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(subtitle);
        return header;
    }

    private JComponent createConversationCard() {
        messages = new JPanel();
        messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
        messages.setOpaque(true);
        messages.setBackground(SURFACE_BACKGROUND);

        messages.add(Box.createVerticalGlue());

        messagesScrollPane = new JScrollPane(messages);
        messagesScrollPane.setBorder(BorderFactory.createEmptyBorder());
        messagesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        messagesScrollPane.getViewport().setBackground(SURFACE_BACKGROUND);

        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(SURFACE_BACKGROUND);
        card.setBorder(CARD_BORDER);
        card.add(messagesScrollPane, BorderLayout.CENTER);
        return card;
    }

    private JComponent createMessageRow(String sender, String message, boolean fromUser) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent bubble = createBubble(sender, message, fromUser);
        row.add(bubble, fromUser ? BorderLayout.EAST : BorderLayout.WEST);
        return row;
    }

    /**
     * Renders slash-command output (e.g. {@code /help}, {@code /model}) as a
     * full-width, neutral-colored row instead of a user/assistant chat bubble.
     */
    private JComponent createSystemRow(String message) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setOpaque(true);
        bubble.setBackground(SYSTEM_BUBBLE);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDD, 0xD6, 0xEE)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setOpaque(false);
        messageArea.setForeground(SECONDARY_FOREGROUND);
        messageArea.setFont(messageArea.getFont().deriveFont(Font.PLAIN, messageArea.getFont().getSize() - 1f));
        sizeMessageArea(messageArea, message);

        bubble.add(messageArea, BorderLayout.CENTER);
        row.add(bubble, BorderLayout.CENTER);
        return row;
    }

    private JComponent createBubble(String sender, String message, boolean fromUser) {
        // Cap the bubble's width so long messages wrap instead of stretching to
        // fill the conversation card, while short messages shrink to fit their content.
        JPanel bubble = new JPanel(new BorderLayout(0, 4));
        bubble.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel senderLabel = new JLabel(sender);
        senderLabel.setForeground(SECONDARY_FOREGROUND);
        senderLabel.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));

        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setOpaque(false);
        messageArea.setForeground(HEADER_FOREGROUND);
        messageArea.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        sizeMessageArea(messageArea, message);

        bubble.add(senderLabel, BorderLayout.NORTH);
        bubble.add(messageArea, BorderLayout.CENTER);

        bubble.setOpaque(true);
        bubble.setBackground(fromUser ? USER_BUBBLE : ASSISTANT_BUBBLE);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fromUser ? new Color(0xB7, 0xD7, 0xFA) : new Color(0xB8, 0xE3, 0xC7)),
                bubble.getBorder()
        ));
        return bubble;
    }

    /**
     * Sizes a word-wrapped {@link JTextArea} so short messages shrink to fit
     * their content and long messages wrap at a fixed max width with a height
     * that matches how many lines they actually occupy once wrapped.
     * <p>
     * JTextArea's row/column API measures in character units and can't express
     * "wrap at N pixels, then size to however many lines that produces", so
     * this uses the standard two-pass technique: set a fixed width, ask Swing
     * for the preferred size at that width (which accounts for real wrapping),
     * then lock the preferred size to the measured (width, height).
     */
    private void sizeMessageArea(JTextArea messageArea, String message) {
        FontMetrics metrics = messageArea.getFontMetrics(messageArea.getFont());
        int naturalWidth = 0;
        for (String line : message.split("\n", -1)) {
            naturalWidth = Math.max(naturalWidth, metrics.stringWidth(line));
        }

        int width = Math.min(BUBBLE_MAX_WIDTH_PX, Math.max(BUBBLE_MIN_WIDTH_PX, naturalWidth + 4));
        messageArea.setSize(new Dimension(width, Integer.MAX_VALUE / 2));
        Dimension preferred = messageArea.getPreferredSize();
        messageArea.setPreferredSize(new Dimension(width, preferred.height));
    }

    private JComponent createComposer() {
        JPanel composer = new JPanel(new BorderLayout(0, 4));
        composer.setBorder(CARD_BORDER);
        composer.setOpaque(true);
        composer.setBackground(SURFACE_BACKGROUND);

        input = new JTextArea(2, 0);
        input.setToolTipText("Enter a message for Copilot (Enter to send, Shift+Enter for a new line). "
                + "Start with / for commands, e.g. /help");
        input.setBackground(COMPOSER_BACKGROUND);
        input.setForeground(CONTROL_FOREGROUND);
        input.setCaretColor(CONTROL_FOREGROUND);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        // Enter sends the prompt; Shift+Enter inserts a newline for multi-line prompts.
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send-prompt");
        input.getActionMap().put("send-prompt", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                sendPrompt();
            }
        });
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-break");
        input.getActionMap().put("insert-break", new DefaultEditorKit.InsertBreakAction());

        JScrollPane inputScrollPane = new JScrollPane(input,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xD8, 0xE0, 0xEA)));
        inputScrollPane.setPreferredSize(new Dimension(0, 64));

        sendButton = new JButton("Send");
        sendButton.setFocusable(false);
        sendButton.setMargin(new Insets(6, 12, 6, 12));
        sendButton.setForeground(CONTROL_FOREGROUND);
        sendButton.setBackground(BUTTON_BACKGROUND);
        sendButton.setOpaque(true);
        sendButton.addActionListener(event -> sendPrompt());

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.add(inputScrollPane, BorderLayout.CENTER);
        inputRow.add(sendButton, BorderLayout.EAST);

        hint = new JLabel("", SwingConstants.LEFT);
        hint.setForeground(SECONDARY_FOREGROUND);

        composer.add(inputRow, BorderLayout.NORTH);
        composer.add(hint, BorderLayout.SOUTH);
        return composer;
    }

    private void sendPrompt() {
        String text = input.getText().trim();
        if (text.isEmpty() || !sendButton.isEnabled()) {
            return;
        }

        input.setText("");

        if (text.startsWith("/")) {
            handleSlashCommand(text);
            return;
        }

        appendMessage("You", text, true);
        setRequestInProgress(true);

        CompletableFuture
                .supplyAsync(() -> copilot.send(text))
                .whenComplete((response, error) -> SwingUtilities.invokeLater(() -> {
                    setRequestInProgress(false);
                    if (error != null) {
                        appendMessage("Copilot", formatError(error), false);
                        return;
                    }
                    appendMessage("Copilot", response, false);
                }));
    }

    /**
     * Routes a {@code /command [args]} line entered in the composer, mirroring
     * the CLI's slash commands. Unlike regular prompts, slash commands are
     * handled locally in the GUI and never sent to Copilot as a chat message.
     */
    private void handleSlashCommand(String rawCommand) {
        int spaceIndex = rawCommand.indexOf(' ');
        String name = (spaceIndex == -1 ? rawCommand : rawCommand.substring(0, spaceIndex)).toLowerCase(Locale.ROOT);
        String args = spaceIndex == -1 ? "" : rawCommand.substring(spaceIndex + 1).trim();

        switch (name) {
            case "/help" -> appendSystemMessage(HELP_TEXT);
            case "/clear" -> clearConversation();
            case "/model" -> handleModelCommand(args);
            case "/login", "/feedback", "/experimental" -> appendSystemMessage(
                    "\"" + name + "\" isn't available in the GUI yet. Run `copilot` in a terminal and use "
                            + name + " there.");
            default -> appendSystemMessage("Unknown command \"" + name + "\". Type /help to see supported commands.");
        }
    }

    private void handleModelCommand(String modelId) {
        if (!modelId.isEmpty()) {
            copilot.setSelectedModel(modelId);
            appendSystemMessage("Model set to \"" + modelId + "\" for new requests.");
            setRequestInProgress(false);
            return;
        }

        setRequestInProgress(true);
        CompletableFuture
                .supplyAsync(copilot::listModels)
                .whenComplete((models, error) -> SwingUtilities.invokeLater(() -> {
                    setRequestInProgress(false);
                    if (error != null) {
                        appendSystemMessage("Unable to list models: " + formatError(error));
                        return;
                    }
                    appendSystemMessage(formatModelList(models));
                }));
    }

    private String formatModelList(List<ModelInfo> models) {
        if (models.isEmpty()) {
            return "No models are currently available.";
        }

        String current = copilot.getSelectedModel();
        StringBuilder text = new StringBuilder("Available models (use /model <id> to switch):");
        for (ModelInfo model : models) {
            text.append("\n- ").append(model.getId());
            if (model.getName() != null && !model.getName().isBlank()) {
                text.append(" \u2014 ").append(model.getName());
            }
            if (model.getId() != null && model.getId().equals(current)) {
                text.append(" (current)");
            }
        }
        return text.toString();
    }

    private void clearConversation() {
        messages.removeAll();
        messages.add(Box.createVerticalGlue());
        messages.revalidate();
        messages.repaint();
        setRequestInProgress(false);
    }

    private void appendMessage(String sender, String message, boolean fromUser) {
        appendRow(createMessageRow(sender, message, fromUser));
    }

    private void appendSystemMessage(String message) {
        appendRow(createSystemRow(message));
    }

    private void appendRow(JComponent row) {
        int glueIndex = messages.getComponentCount() - 1;
        messages.remove(glueIndex);
        if (messages.getComponentCount() > 0) {
            messages.add(Box.createVerticalStrut(12));
        }
        messages.add(row);
        messages.add(Box.createVerticalGlue());
        messages.revalidate();
        messages.repaint();
        scrollToBottom();
    }


    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            var verticalScrollBar = messagesScrollPane.getVerticalScrollBar();
            verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        });
    }

    private void setRequestInProgress(boolean inProgress) {
        sendButton.setEnabled(!inProgress);
        input.setEnabled(!inProgress);
        String model = copilot.getSelectedModel();
        String idleHint = "Uses your authenticated Copilot CLI account \u00b7 Model: "
                + (model == null ? "default" : model) + " \u00b7 Type /help for commands";
        hint.setText(inProgress ? "Copilot is thinking..." : idleHint);
    }

    private String formatError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return "Unable to contact Copilot: " + cause.getMessage();
    }
}
