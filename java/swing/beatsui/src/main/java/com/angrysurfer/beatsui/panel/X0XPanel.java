package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

public class X0XPanel extends JPanel {

    public X0XPanel() {
        super(new BorderLayout());
        setup();
    }

    private void setup() {
        setLayout(new BorderLayout());
        add(createX0XPanel());
    }

    private JPanel createX0XPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel pianoPanel = createPianoPanel();
        mainPanel.add(pianoPanel, BorderLayout.NORTH);

        // Create panel for the 16 columns
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 5, 0));
        sequencePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            sequencePanel.add(columnPanel);
        }

        // Wrap in scroll pane in case window gets too small
        JScrollPane scrollPane = new JScrollPane(sequencePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createPianoPanel() {
        JPanel panel = new JPanel(null); // Using null layout for precise key positioning
        panel.setPreferredSize(new Dimension(500, 80));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(40, 40, 40));

        // Dimensions for keys
        int whiteKeyWidth = 30;
        int whiteKeyHeight = 60;
        int blackKeyWidth = 17;
        int blackKeyHeight = 30;

        // Create white keys
        String[] whiteNotes = { "C", "D", "E", "F", "G", "A", "B", "C" };
        for (int i = 0; i < 8; i++) {
            JButton whiteKey = createPianoKey(true, whiteNotes[i]);
            whiteKey.setBounds(i * whiteKeyWidth + 10, 10, whiteKeyWidth - 1, whiteKeyHeight);
            panel.add(whiteKey);
        }

        // Create black keys
        String[] blackNotes = { "C#", "D#", "", "F#", "G#", "A#", "" };
        for (int i = 0; i < 7; i++) {
            if (!blackNotes[i].isEmpty()) {
                JButton blackKey = createPianoKey(false, blackNotes[i]);
                blackKey.setBounds(i * whiteKeyWidth + whiteKeyWidth / 2 + 10, 10, blackKeyWidth, blackKeyHeight);
                panel.add(blackKey, 0); // Add black keys first so they appear on top
            }
        }

        return panel;
    }

    private JButton createPianoKey(boolean isWhite, String note) {
        JButton key = new JButton();
        key.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Base color
                if (isWhite) {
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, w, h);
                    // Add shadow effect
                    g2d.setColor(new Color(200, 200, 200));
                    g2d.fillRect(0, h - 10, w, 10);
                } else {
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(0, 0, w, h);
                    // Add highlight effect
                    g2d.setColor(new Color(60, 60, 60));
                    g2d.fillRect(0, 0, w, 5);
                }

                // Draw border
                g2d.setColor(Color.BLACK);
                g2d.drawRect(0, 0, w - 1, h - 1);

                // Draw note label at bottom of white keys
                if (isWhite) {
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    FontMetrics fm = g2d.getFontMetrics();
                    int noteWidth = fm.stringWidth(note);
                    g2d.drawString(note, (w - noteWidth) / 2, h - 15);
                }

                g2d.dispose();
            }
        });

        key.setContentAreaFilled(false);
        key.setBorderPainted(false);
        key.setFocusPainted(false);
        key.setToolTipText(note);

        return key;
    }

    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

        // Add 4 knobs
        for (int i = 0; i < 5; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setForeground(Color.GRAY);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            labelPanel.add(label);
            // Add label to the column
            column.add(labelPanel);

            JDial dial = createKnob();
            dial.setToolTipText(String.format("Step %d Knob %d", index + 1, i + 1));
            dial.setName("JDial-" + index + "-" + i);
            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);

            // Add small spacing between knobs
            column.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        // Add the trigger button
        JButton triggerButton = createTriggerButton(index);
        triggerButton.setName("TriggerButton-" + index);

        // Center the button horizontally
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton);
        column.add(buttonPanel1);

        // Add the pad button
        JButton padButton = createPadButton(index);
        padButton.setName("PadButton-" + index);
        padButton.setToolTipText("Pad " + (index + 1));
        padButton.setText(Integer.toString(index + 1));

        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel2.add(padButton);
        column.add(buttonPanel2);

        return column;
    }

    private String getKnobLabel(int i) {
        return i == 0 ? "Note" : i == 1 ? "Vel." : i == 2 ? "Gate" : "Prob.";
    }

    private JDial createKnob() {
        JDial dial = new JDial();
        // Increase size by 30%
        dial.setPreferredSize(new Dimension(40, 40));
        dial.setMinimumSize(new Dimension(40, 40));
        dial.setMaximumSize(new Dimension(40, 40));
        return dial;
    }

    // Simple JDial implementation for knobs
    private class JDial extends JComponent {
        private int value = 64;
        private boolean isDragging = false;
        private int lastY;

        public JDial() {
            setPreferredSize(new Dimension(40, 40));

            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mousePressed(java.awt.event.MouseEvent e) {
                    isDragging = true;
                    lastY = e.getY();
                }

                public void mouseReleased(java.awt.event.MouseEvent e) {
                    isDragging = false;
                }
            });

            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                public void mouseDragged(java.awt.event.MouseEvent e) {
                    if (isDragging) {
                        int delta = lastY - e.getY();
                        value = Math.min(127, Math.max(0, value + delta));
                        lastY = e.getY();
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int min = Math.min(w, h);

            // Draw knob body with blue color
            g2d.setColor(new Color(30, 100, 255));
            g2d.fillOval(0, 0, min - 1, min - 1);

            // Add a subtle gradient for 3D effect
            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(60, 130, 255),
                    0, h, new Color(20, 80, 200));
            g2d.setPaint(gp);
            g2d.fillOval(2, 2, min - 4, min - 4);

            // Draw white indicator line
            g2d.setColor(Color.WHITE);
            // g2d.setStroke(new BasicStroke(2.0f));
            double angle = Math.PI * 0.75 + (Math.PI * 1.5 * value / 127.0);
            int centerX = min / 2;
            int centerY = min / 2;
            int radius = min / 2 - 6;

            g2d.drawLine(centerX, centerY,
                    centerX + (int) (Math.cos(angle) * radius),
                    centerY + (int) (Math.sin(angle) * radius));

            // Add highlight for 3D effect
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.fillOval(5, 3, min / 2 - 5, min / 2 - 5);

            g2d.dispose();
        }

        public void setValue(int newValue) {
            value = Math.min(127, Math.max(0, newValue));
            repaint();
        }

        public int getValue() {
            return value;
        }
    }

    private JButton createPadButton(int index) {
        JButton button = new JButton();
        Color baseColor = new Color(60, 60, 60); // Dark grey base
        Color flashColor = new Color(160, 160, 160); // Lighter grey for flash
        final boolean[] isFlashing = { false };

        button.addActionListener(e -> {
            isFlashing[0] = true;
            button.repaint();

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
                    g2d.setColor(flashColor);
                } else {
                    g2d.setColor(baseColor);
                }

                g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add border
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add highlight
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                g2d.dispose();
            }
        });

        button.setPreferredSize(new Dimension(40, 40));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);

        return button;
    }

    private JButton createTriggerButton(int index) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(30, 20));
        button.setMinimumSize(new Dimension(30, 20));
        button.setMaximumSize(new Dimension(30, 20));

        final boolean[] isActive = { false };

        // Orange for inactive state
        Color topColorInactive = new Color(255, 140, 0); // Bright orange
        Color bottomColorInactive = new Color(200, 110, 0); // Darker orange

        // Green for active state
        Color topColorActive = new Color(50, 255, 50); // Bright green
        Color bottomColorActive = new Color(40, 200, 40); // Darker green

        button.addActionListener(e -> {
            isActive[0] = !isActive[0];
            button.repaint();
        });

        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Choose colors based on state
                Color topColor = isActive[0] ? topColorActive : topColorInactive;
                Color bottomColor = isActive[0] ? bottomColorActive : bottomColorInactive;

                GradientPaint gp = new GradientPaint(
                        0, 0, topColor,
                        0, h, bottomColor);

                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add border
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add highlight
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                g2d.dispose();
            }
        });

        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setToolTipText("Step " + (index + 1));

        return button;
    }
}
