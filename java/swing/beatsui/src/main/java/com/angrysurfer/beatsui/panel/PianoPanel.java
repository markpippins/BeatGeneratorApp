package com.angrysurfer.beatsui.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import com.angrysurfer.beatsui.api.StatusConsumer;

public class PianoPanel extends StatusProviderPanel {
    public PianoPanel() {
        this(null);
    }

    public PianoPanel(StatusConsumer statusConsumer) {
        super(null, statusConsumer);
        setPreferredSize(new Dimension(500, 80));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        // setBackground(new Color(40, 40, 40));

        // Dimensions for keys
        int whiteKeyWidth = 30;
        int whiteKeyHeight = 60;
        int blackKeyWidth = 17;
        int blackKeyHeight = 30;

        // Create white keys

        String[] whiteNotes = { "C", "D", "E", "F", "G", "A", "B" };
        for (int i = 0; i < 7; i++) {
            JButton whiteKey = createPianoKey(true, whiteNotes[i]);
            whiteKey.setBounds(i * whiteKeyWidth + 10, 10, whiteKeyWidth - 1, whiteKeyHeight);
            add(whiteKey);
        }

        // Create black keys
        String[] blackNotes = { "C#", "D#", "", "F#", "G#", "A#", "" };
        for (int i = 0; i < 7; i++) {
            if (!blackNotes[i].isEmpty()) {
                JButton blackKey = createPianoKey(false, blackNotes[i]);
                blackKey.setBounds(i * whiteKeyWidth + whiteKeyWidth / 2 + 10, 10, blackKeyWidth, blackKeyHeight);
                add(blackKey, 0); // Add black keys first so they appear on top
            }
        }
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

                // Check if button is pressed
                boolean isPressed = ((JButton) c).getModel().isPressed();

                // Base color with pressed effect
                if (isWhite) {
                    if (isPressed) {
                        g2d.setColor(new Color(230, 230, 230));
                        g2d.fillRect(0, 2, w, h); // Slight offset when pressed
                    } else {
                        g2d.setColor(Color.WHITE);
                        g2d.fillRect(0, 0, w, h);
                        // Shadow effect
                        g2d.setColor(new Color(200, 200, 200));
                        g2d.fillRect(0, h - 10, w, 10);
                    }
                } else {
                    if (isPressed) {
                        g2d.setColor(new Color(20, 20, 20));
                        g2d.fillRect(0, 2, w, h); // Slight offset when pressed
                    } else {
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(0, 0, w, h);
                        // Highlight effect
                        g2d.setColor(new Color(60, 60, 60));
                        g2d.fillRect(0, 0, w, 5);
                    }
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

        // Add press effect
        key.setPressedIcon(null);
        key.setContentAreaFilled(false);
        key.setBorderPainted(false);
        key.setFocusPainted(false);
        key.setToolTipText(note);

        return key;
    }
}
