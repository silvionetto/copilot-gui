package com.silvionetto;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().showWindow());
    }

    private void showWindow() {
        JFrame frame = new JFrame("JGoodies Hello World");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(createContent());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JComponent createContent() {
        FormLayout layout = new FormLayout("right:pref, 8dlu, pref:grow", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.border(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        builder.appendSeparator("Welcome");
        builder.append("Message", new JLabel("Hello from JGoodies and Swing!"));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener((ActionEvent event) -> System.exit(0));
        builder.append("Action", closeButton);

        JPanel panel = builder.getPanel();
        panel.setOpaque(true);
        return panel;
    }
}
