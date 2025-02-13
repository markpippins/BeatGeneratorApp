package com.angrysurfer.beatsui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

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
        this(null, null, null);
    }

    public Dialog(T data) {
        this(null, data, null);
    }

    public Dialog(T data, JPanel content) {
        this(null, data, content);
    }

    public Dialog(Frame owner, T data, JPanel content) {
        super(owner, true); // Make dialog modal with specified owner
        this.data = data;
        this.contentPanel = content;
        setupDialog();
    }

    private void setupDialog() {
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Add keyboard shortcuts
        setupKeyboardShortcuts();

        // Title panel
        setupTitlePanel();

        // Content area
        setupContentPanel();

        // Button panel
        setupButtonPanel();

        // Increase minimum size for additional controls
        setMinimumSize(getContentPanel().getMinimumSize());
        setMaximumSize(getContentPanel().getMaximumSize());
        setPreferredSize(getContentPanel().getPreferredSize());
        pack();
        setLocationRelativeTo(getOwner());
        setResizable(true);
    }

    private void setupKeyboardShortcuts() {
        // Handle Enter key

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "accept");
        getRootPane().getActionMap().put("accept", new javax.swing.AbstractAction() {

            final Dialog dialog = Dialog.this;

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.accept();
            }
        });

        // Handle Escape key
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getRootPane().getActionMap().put("cancel", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
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
        Frame frame = new Frame();
        frame.setVisible(true);

        // Create sample player
        Strike samplePlayer = new Strike();

        // Create test panel with sample player
        JPanel testPanel = new PlayerEditorPanel(samplePlayer);

        // Create and show dialog with frame as owner
        Dialog<Strike> dialog = new Dialog<>(frame, samplePlayer, testPanel);
        dialog.setTitle("Edit Player: " + samplePlayer.getName());
        boolean result = dialog.showDialog();

        System.out.println("Dialog result: " + result);
        if (result) {
            Strike updatedPlayer = ((PlayerEditorPanel) testPanel).getUpdatedPlayer();
            System.out.println("Updated player name: " + updatedPlayer.getName());
        }
    }
}
