package com.angrysurfer.beats;

import java.awt.BorderLayout;
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
        setResizable(false);

        setupKeyboardShortcuts();
        setupTitlePanel();
        setupButtonPanel();

        if (contentPanel != null) {
            // Ensure content is properly added
            add(contentPanel, BorderLayout.CENTER);
        }

        // Set reasonable initial size and center
        setSize(600, 400);
        setLocationRelativeTo(getOwner());
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

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
   }

    private void setupTitlePanel() {
        titlePanel = new JPanel();
        titlePanel.setLayout(new BorderLayout());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        titlePanel.setBackground(getBackground()); // Match dialog background

        // Create navigation buttons with same style as toolbar
        JButton prevButton = createNavigationButton("⏮", "Previous");
        JButton nextButton = createNavigationButton("⏭", "Next");

        titleLabel = new JLabel("Title", JLabel.CENTER);

        // Add buttons to title panel
        titlePanel.add(prevButton, BorderLayout.WEST);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(nextButton, BorderLayout.EAST);

        add(titlePanel, BorderLayout.NORTH);
    }

    private JButton createNavigationButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }

        // Match toolbar button styling but use dialog colors
        button.setForeground(getForeground());
        button.setBackground(getBackground());
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(32, 32));

        return button;
    }

    private void setupButtonPanel() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
        buttonPanel.setBackground(getBackground()); // Match dialog background

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

    public void setContent(JPanel content) {
        // Remove existing content if any
        if (contentPanel != null) {
            remove(contentPanel);
        }

        contentPanel = content;
        if (content != null) {
            add(content, BorderLayout.CENTER);

            // Force layout update
            revalidate();
            repaint();

            // Adjust size to fit content
            pack();
            setLocationRelativeTo(getOwner());
        }
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
    // public static void main(String[] args) {
    // Frame frame = new Frame();
    // frame.setVisible(true);

    // // Create sample player
    // ProxyStrike samplePlayer = new ProxyStrike();

    // // Create test panel with sample player
    // JPanel testPanel = new PlayerEditorPanel(samplePlayer);

    // // Create and show dialog with frame as owner
    // Dialog<ProxyStrike> dialog = new Dialog<>(frame, samplePlayer, testPanel);
    // dialog.setTitle("Edit Player: " + samplePlayer.getName());
    // boolean result = dialog.showDialog();

    // // System.out.println("Dialog result: " + result);
    // if (result) {
    // ProxyStrike updatedPlayer = ((PlayerEditorPanel)
    // testPanel).getUpdatedPlayer();
    // // System.out.println("Updated player name: " + updatedPlayer.getName());
    // }
    // }
}
