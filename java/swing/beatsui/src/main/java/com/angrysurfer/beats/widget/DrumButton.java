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
    private boolean toggle; // Toggle attribute
    private boolean isToggled; // Current toggle state
    private boolean exclusive; // Exclusive toggle attribute
    private Color defaultColor = new Color(50, 130, 200); // Default color
    private Color highlightColor = new Color(255, 100, 100); // Highlight color

    public DrumButton() {
        super();
        this.toggle = false; // Default to not toggled
        this.isToggled = false; // Initial state
        this.exclusive = false; // Default to not exclusive

        // Register this button to listen for PAD_TOGGLED messages
        CommandBus.getInstance().register(this);

        // Add action listener to handle toggle behavior
        addActionListener(e -> {
            if (toggle) {
                isToggled = !isToggled; // Toggle the state
                repaint(); // Repaint to reflect the change

                // Publish the PAD_TOGGLED command to the CommandBus
                CommandBus.getInstance().publish(Commands.PAD_TOGGLED, this);
                // CommandBus.getInstance().publish(new Command("PAD_TOGGLED", this));
            }
        });
        setup();
    }

    public void setPadNumber(int padNumber) {
        this.padNumber = padNumber;
        repaint();
    }

    public int getPadNumber() {
        return padNumber;
    }

    public void setIsMainBeat(boolean isMainBeat) {
        this.isMainBeat = isMainBeat;
        repaint();
    }

    public boolean isMainBeat() {
        return isMainBeat;
    }

    private void setup() {
        Color baseColor = new Color(50, 130, 200); // A vibrant, cool blue
        Color flashColor = new Color(160, 160, 160); // Lighter grey for flash
        final boolean[] isFlashing = { false };

        addActionListener(e -> {
            isFlashing[0] = true;
            repaint();

            Timer timer = new Timer(100, evt -> {
                isFlashing[0] = false;
                repaint();
                ((Timer) evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        });

        setUI(new BasicButtonUI() {
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
                    if (isToggled) {
                        g2d.setColor(highlightColor);
                    } else {
                        g2d.setColor(defaultColor);
                    }
                }

                g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add border
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add highlight
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                // Draw pad number if set
                if (padNumber > 0) {
                    // Use white text for better contrast
                    g2d.setColor(Color.WHITE);

                    // Use a bold font
                    g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 14f));

                    String padText = String.valueOf(padNumber);
                    FontMetrics fm = g2d.getFontMetrics();

                    // Center the text
                    int textX = (w - fm.stringWidth(padText)) / 2;
                    int textY = (h + fm.getAscent() - fm.getDescent()) / 2;

                    // Draw the text
                    g2d.drawString(padText, textX, textY);

                    // Draw underline for main beats (1, 5, 9, 13)
                    if (isMainBeat) {
                        int underlineY = textY + 2; // 2 pixels below text baseline
                        g2d.drawLine(textX, underlineY, textX + fm.stringWidth(padText), underlineY);
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

    // Method to set the toggle attribute
    public void setToggle(boolean toggle) {
        this.toggle = toggle;
    }

    // Method to set the exclusive attribute
    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Set the color based on the toggle state
        if (isToggled) {
            g.setColor(highlightColor); // Use highlight color if toggled
        } else {
            g.setColor(defaultColor); // Use default color if not toggled
        }
        super.paintComponent(g); // Call the superclass method to paint the button
    }

    @Override
    public void onAction(Command action) {
        if ("PAD_TOGGLED".equals(action.getCommand())) {
            // Check if the sender is not this button and exclusive is true
            if (exclusive && action.getData() != this) {
                isToggled = false; // Toggle off
                repaint(); // Repaint to reflect the change
            }
        }
    }

}
