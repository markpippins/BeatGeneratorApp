package com.angrysurfer.beatsui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.angrysurfer.beatsui.mock.Strike;
import com.angrysurfer.beatsui.panel.PlayerEditorPanel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Dialog<T> extends JDialog {
    private T data;
    private JPanel contentPanel;
    private boolean accepted = false;
    private JPanel buttonPanel;
    private JPanel titlePanel;
    private JLabel titleLabel;

    public Dialog() {
        this(null, null);
    }

    public Dialog(T data) {
        this(data, null);
    }

    public Dialog(T data, JPanel content) {
        super((Frame) null, true); // Make dialog modal
        this.data = data;
        this.contentPanel = content;
        setupDialog();
    }

    private void setupDialog() {
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Title panel
        setupTitlePanel();

        // Content area
        setupContentPanel();

        // Button panel
        setupButtonPanel();

        // Increase minimum size for additional controls
        setMinimumSize(new Dimension(900, 700));
        pack();
        setLocationRelativeTo(null);
        setResizable(true);
    }

    private void setupTitlePanel() {
        titlePanel = new JPanel();
        titlePanel.setLayout(new BorderLayout());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        titlePanel.setBackground(Utils.charcoalGray);

        titleLabel = new JLabel("Dialog");
        titleLabel.setForeground(Utils.warmOffWhite);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        titlePanel.add(titleLabel, BorderLayout.WEST);
        add(titlePanel, BorderLayout.NORTH);
    }

    private void setupContentPanel() {
        if (contentPanel == null) {
            // Create default black panel if no content provided
            contentPanel = new JPanel();
            contentPanel.setBackground(Color.BLACK);
        }

        // Add content directly without scroll pane
        add(contentPanel, BorderLayout.CENTER);
    }

    private void setupButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        buttonPanel.setBackground(Utils.charcoalGray);

        JButton okButton = createButton("OK", e -> accept());
        JButton cancelButton = createButton("Cancel", e -> cancel());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JButton createButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.addActionListener(listener);
        return button;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }

    public void setContent(JPanel content) {
        if (contentPanel != null) {
            remove(contentPanel);
        }
        contentPanel = content;
        setupContentPanel();
        revalidate();
        repaint();
    }

    private void accept() {
        accepted = true;
        dispose();
    }

    private void cancel() {
        accepted = false;
        dispose();
    }

    public boolean showDialog() {
        setVisible(true);
        return accepted;
    }

    // Test method
    public static void main(String[] args) {
        // Create sample player
        Strike samplePlayer = new Strike();

        // Create test panel with sample player
        JPanel testPanel = new PlayerEditorPanel(samplePlayer);

        // Create and show dialog
        Dialog<Strike> dialog = new Dialog<>(samplePlayer, testPanel);
        dialog.setTitle("Edit Player: " + samplePlayer.getName());
        boolean result = dialog.showDialog();

        System.out.println("Dialog result: " + result);
        if (result) {
            Strike updatedPlayer = ((PlayerEditorPanel) testPanel).getUpdatedPlayer();
            System.out.println("Updated player name: " + updatedPlayer.getName());
        }
    }
}
