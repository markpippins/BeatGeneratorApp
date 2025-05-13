package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicButtonUI;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrumButton extends JButton implements IBusListener {

    private int padNumber;
    private boolean isMainBeat;
    
    // Just one boolean for exclusive group behavior
    private boolean exclusive; 
    
    // Visual state (separate from selection)
    private boolean highlighted = false;
    
    // Animation state
    private boolean flashing = false;
    
    // Colors
    private Color defaultColor = new Color(50, 130, 200);
    private Color selectedColor = new Color(255, 100, 100);
    private Color flashColor = new Color(160, 160, 160);
    private Color highlightColor = new Color(200, 200, 80);

    public DrumButton() {
        super();
        
        // Register this button to listen for PAD_SELECTED messages
        CommandBus.getInstance().register(this);

        // Add action listener to handle selection behavior
        addActionListener(e -> {
            if (exclusive) {
                // Always set to selected when exclusive and prevent toggling off
                setSelected(true);
                // Notify other buttons in group
                CommandBus.getInstance().publish(Commands.DRUM_BUTTON_SELECTED, this);
            } else {
                // Toggle selection if not exclusive
                setSelected(!isSelected());
            }
        });
        
        setup();
    }

    @Override
    public void onAction(Command action) {
        if (Commands.DRUM_BUTTON_SELECTED.equals(action.getCommand())) {
            // If we're in exclusive mode and not the sender, deselect
            if (exclusive && action.getData() != this) {
                setSelected(false);
                repaint();
            }
        }
    }

    /**
     * Set up the button appearance and behavior
     */
    private void setup() {
        setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Choose color based on state - with clear priority order
                if (flashing) {
                    g2d.setColor(flashColor);
                } else if (isSelected()) {
                    g2d.setColor(selectedColor);
                } else if (highlighted) {
                    g2d.setColor(highlightColor);
                } else {
                    g2d.setColor(defaultColor);
                }

                g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add border
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add highlight
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                // Draw pad number or text
                String displayText = getText();
                if (displayText == null || displayText.isEmpty()) {
                    displayText = (padNumber > 0) ? String.valueOf(padNumber) : "";
                }
                
                if (!displayText.isEmpty()) {
                    // Use white text for better contrast
                    g2d.setColor(Color.WHITE);

                    // Use a bold font
                    g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 14f));

                    FontMetrics fm = g2d.getFontMetrics();

                    // Center the text
                    int textX = (w - fm.stringWidth(displayText)) / 2;
                    int textY = (h + fm.getAscent() - fm.getDescent()) / 2;

                    // Draw the text
                    g2d.drawString(displayText, textX, textY);

                    // Draw underline for main beats (1, 5, 9, 13)
                    if (isMainBeat) {
                        int underlineY = textY + 2;
                        g2d.drawLine(textX, underlineY, textX + fm.stringWidth(displayText), underlineY);
                    }
                }

                g2d.dispose();
            }
        });

        setPreferredSize(new Dimension(50, 50));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
    }

    /**
     * Flash the button briefly (for playback indication)
     */
    public void flash() {
        flashing = true;
        repaint();

        Timer timer = new Timer(100, evt -> {
            flashing = false;
            repaint();
            ((Timer) evt.getSource()).stop();
        });
        timer.setRepeats(false);
        timer.start();
    }
}
