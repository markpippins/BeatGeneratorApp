package com.angrysurfer.beatsui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

public class TestFrame extends JFrame {

    public TestFrame() {
        super("Beats");
        setupFrame();
        setupMenuBar();
    }

    private void setupFrame() {
        setMinimumSize(new Dimension(1200, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setContentPane(createLaunchPanel());
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(new JMenuItem("New"));
        fileMenu.add(new JMenuItem("Open"));
        fileMenu.add(new JMenuItem("Save"));
        fileMenu.add(new JMenuItem("Save As..."));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem("Exit"));

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(new JMenuItem("Undo"));
        editMenu.add(new JMenuItem("Redo"));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem("Cut"));
        editMenu.add(new JMenuItem("Copy"));
        editMenu.add(new JMenuItem("Paste"));

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(new JMenuItem("About"));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private JPanel createLaunchPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        // Create the 8x8 grid panel
        JPanel gridPanel = new JPanel(new GridLayout(8, 8, 5, 5));
        gridPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Define colors for each quadrant
        Color[] quadrantColors = {
                new Color(255, 50, 50), // Top-left: bright red
                new Color(50, 255, 50), // Top-right: bright green
                new Color(50, 50, 255), // Bottom-left: bright blue
                new Color(205, 155, 50) // Bottom-right: bright yellow
        };

        // Create and add 64 drum pad buttons

        int[] count = { 1, 1, 1, 1 };
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // Determine which quadrant we're in (0-3)
                int quadrant = (row / 4) * 2 + (col / 4);

                // Create button with quadrant-specific color
                JButton padButton = createDrumPadButton(row * 8 + col, quadrantColors[quadrant]);
                padButton.setText(Integer.toString(boxConvert(count[quadrant]++)));

                gridPanel.add(padButton);
            }
        }

        // for (int i = 0; i < blue.length - 1; i++)
        // blue[i].setText(Integer.toString(16 - i));
        // Add the grid panel to the main panel with constraints to make it expand
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(gridPanel, gbc);

        return panel;
    }

    private JButton createDrumPadButton(int index, Color baseColor) {
        JButton button = new JButton();

        // Create flash color (lighter version of base color)
        Color flashColor = new Color(
                Math.min(baseColor.getRed() + 100, 255),
                Math.min(baseColor.getGreen() + 100, 255),
                Math.min(baseColor.getBlue() + 100, 255));

        // Track if we're in flash state
        final boolean[] isFlashing = { false };

        button.addActionListener(e -> {
            // Start flash
            isFlashing[0] = true;
            button.repaint();

            // Timer to end flash after 100ms
            Timer timer = new Timer(100, evt -> {
                isFlashing[0] = false;
                button.repaint();
                ((Timer) evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        });

        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                if (isFlashing[0]) {
                    // Draw flash state
                    g2d.setColor(flashColor);
                    g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                } else {
                    // Draw normal state
                    g2d.setColor(baseColor);
                    g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                }

                // Add border
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add highlight
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                // Draw the button text if it exists
                String text = ((JButton) c).getText();
                if (text != null && !text.isEmpty()) {
                    g2d.setFont(new Font("Arial", Font.BOLD, 16));
                    g2d.setColor(Color.WHITE);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getHeight();
                    g2d.drawString(text,
                            (w - textWidth) / 2,
                            ((h + textHeight) / 2) - fm.getDescent());
                }

                g2d.dispose();
            }
        });

        button.setPreferredSize(new Dimension(40, 40));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setToolTipText("Pad " + index);

        return button;
    }

    public int boxConvert(int input) {
        switch (input) {
            case 1:
                return 13;
            case 2:
                return 14;
            case 3:
                return 15;
            case 4:
                return 16;
            case 5:
                return 9;
            case 6:
                return 10;
            case 7:
                return 11;
            case 8:
                return 12;
            case 9:
                return 5;
            case 10:
                return 6;
            case 11:
                return 7;
            case 12:
                return 8;
            case 13:
                return 1;
            case 14:
                return 2;
            case 15:
                return 3;
            case 16:
                return 4;
        }

        return input; // Return unchanged if out of range
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                // UIManager.setLookAndFeel(
                // UIManager.getSystemLookAndFeelClassName());
                UIManager.setLookAndFeel(
                        UIManager.getCrossPlatformLookAndFeelClassName());
                // UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
                // UIManager.setLookAndFeel("com.sun.java.swing.plaf.metal.MetalLookAndFeel");
                // UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                // UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
                // UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");

            } catch (Exception e) {
                e.printStackTrace();
            }

            TestFrame app = new TestFrame();
            app.setLocationRelativeTo(null); // Center on screen
            app.setVisible(true);
        });
    }
}
