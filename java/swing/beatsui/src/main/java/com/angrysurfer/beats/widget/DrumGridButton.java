package com.angrysurfer.beats.widget;

import com.angrysurfer.beats.util.UIHelper;

import javax.swing.*;
import java.awt.*;

public class DrumGridButton extends JButton {

    private final int drumPadIndex = -1; // Default to -1 for unassigned index
    private final Color temporaryColor = new Color(200, 150, 40); // Amber highlight
    private boolean isHighlighted = false;
    private boolean isTemporary = false;
    private Color normalColor;
    private boolean inPattern = true;
    private Color highlightColor = UIHelper.fadedOrange; // Default highlight color

    // Add properties to store parameter values
    private int velocity = 100;     // Default values
    private int decay = 250;
    private int probability = 100;
    private int nudge = 0;
    private int pan = 64;
    private int chorus = 0;
    private int reverb = 0;

    // Flags to indicate which parameters to visualize
    private boolean showVelocity = true;
    private boolean showDecay = true;
    private boolean showProbability = true;
    private boolean showNudge = true;
    private boolean showEffects = false;  // pan, chorus, reverb

    // Step index within pattern (for debugging)
    private int stepIndex = -1;

    // Accent property
    private boolean accented = false;

    /**
     * Create a new trigger button with label
     *
     * @param text The button text
     */
    public DrumGridButton(String text) {
        super(text);
        initialize();
    }

    /**
     * Create a new trigger button without label
     */
    public DrumGridButton() {
        super();
        initialize();
    }

    /**
     * Check if this step is accented
     *
     * @return true if accented
     */
    public boolean isAccented() {
        return accented;
    }

    /**
     * Set whether this step is accented
     *
     * @param accented true if accented, false otherwise
     */
    public void setAccented(boolean accented) {
        this.accented = accented;
        repaint();
    }

    /**
     * Initialize common properties
     */
    private void initialize() {
        setPreferredSize(new Dimension(20, 20));
        setBackground(Color.DARK_GRAY);
        setForeground(Color.WHITE);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(true);
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    public void setStepIndex(int index) {
        this.stepIndex = index;
    }

    // Add parameter setters
    public void setStepParameters(boolean active, boolean isAccented, int velocity, int decay, int probability, int nudge) {
        setActiveQuietly(active);
        this.velocity = velocity;
        this.decay = decay;
        this.probability = probability;
        this.nudge = nudge;
        this.accented = isAccented;
        invalidate();
        repaint();
    }

    // Add effects parameter setters
    public void setEffectsParameters(int pan, int chorus, int reverb) {
        this.pan = pan;
        this.chorus = chorus;
        this.reverb = reverb;
        repaint(); // Request repaint to show new parameter values
    }

    // Individual parameter setters
    public void setVelocity(int velocity) {
        this.velocity = velocity;
        repaint();
    }

    public void setDecay(int decay) {
        this.decay = decay;
        repaint();
    }

    public void setProbability(int probability) {
        this.probability = probability;
        repaint();
    }

    public void setNudge(int nudge) {
        this.nudge = nudge;
        repaint();
    }

    public void setPan(int pan) {
        this.pan = pan;
        repaint();
    }

    public void setChorus(int chorus) {
        this.chorus = chorus;
        repaint();
    }

    public void setReverb(int reverb) {
        this.reverb = reverb;
        repaint();
    }

    // Parameter visualization toggles
    public void setShowVelocity(boolean show) {
        this.showVelocity = show;
        repaint();
    }

    public void setShowDecay(boolean show) {
        this.showDecay = show;
        repaint();
    }

    public void setShowProbability(boolean show) {
        this.showProbability = show;
        repaint();
    }

    public void setShowNudge(boolean show) {
        this.showNudge = show;
        repaint();
    }

    public void setShowEffects(boolean show) {
        this.showEffects = show;
        repaint();
    }

    /**
     * Set whether this button is toggleable
     *
     * @param toggleable If true, the button will maintain its selected state when clicked
     */
    public void setToggleable(boolean toggleable) {
        // JToggleButton is already toggleable by default
        // This method is provided for API compatibility with other button classes

        // If we want to disable toggling:
        if (!toggleable) {
            // Override the model to prevent toggling
            setModel(new DefaultButtonModel() {
                @Override
                public void setSelected(boolean b) {
                    // Only allow selection, not toggling
                    if (b) {
                        super.setSelected(b);
                        // Automatically revert after a short delay
                        Timer timer = new Timer(100, e -> super.setSelected(false));
                        timer.setRepeats(false);
                        timer.start();
                    }
                }
            });
        } else {
            // Restore default toggle button behavior
            setModel(new DefaultButtonModel());
        }
    }

    /**
     * Get the highlighted state
     *
     * @return true if highlighted, false otherwise
     */
    public boolean isHighlighted() {
        return isHighlighted;
    }

    /**
     * Set whether this button is highlighted
     *
     * @param highlighted true to highlight, false to unhighlight
     */
    public void setHighlighted(boolean highlighted) {
        this.isHighlighted = highlighted;
        repaint(); // Just repaint, the actual color is handled in paintComponent
    }

    /**
     * Check if the button is toggled on
     *
     * @return true if toggled on, false otherwise
     */
    public boolean isToggled() {
        return isSelected();
    }

    /**
     * Set the toggled state of this button
     */
    public void setToggled(boolean toggled) {
        // Always make sure the button is set to be visible and opaque first
        setVisible(true);
        setOpaque(true);

        // Set the selected state
        setSelected(toggled);

        // Make sure the change is visible
        repaint();
    }

    /**
     * Set a temporary state during pattern drawing
     */
    public void setTemporaryState(boolean state) {
        isTemporary = state;
        repaint();
    }

    /**
     * Clear temporary state after pattern is applied
     */
    public void clearTemporaryState() {
        isTemporary = false;
        repaint();
    }

    /**
     * Get whether this button is in the pattern
     *
     * @return true if in pattern, false otherwise
     */
    public boolean isInPattern() {
        return inPattern;
    }

    /**
     * Set whether this button is in the pattern
     *
     * @param inPattern true if in pattern, false otherwise
     */
    public void setInPattern(boolean inPattern) {
        this.inPattern = inPattern;
        repaint();
    }

    /**
     * Set custom highlight color for this step button
     */
    public void setHighlightColor(Color color) {
        this.highlightColor = color;
        repaint();
    }

    /**
     * Set active state without triggering events
     * This is used during grid refreshes to avoid event loops
     *
     * @param active Whether the step is active
     */
    public void setActiveQuietly(boolean active) {
        boolean oldValue = isSelected();

        if (oldValue != active) {
            // Use direct model access to avoid firing events
            getModel().setSelected(active);
            // Update appearance 
            repaint();
        }
    }

    /**
     * Get the color to use for displaying velocity
     */
    private Color getVelocityColor() {
        // Normalize velocity (0-127) to (0-255)
        int value = (int) (velocity * 2.0);
        return new Color(value, value, 0); // Yellow with intensity based on velocity
    }

    /**
     * Get alpha value for probability display
     */
    private int getProbabilityAlpha() {
        return (int) (probability * 2.55); // Convert 0-100 to 0-255
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Base button color
        if (isHighlighted()) {
            g2d.setColor(highlightColor);
        } else if (isSelected()) {
            if (!inPattern) {
                g2d.setColor(UIHelper.charcoalGray);
            } else {
                // Base color for selected (inactive) buttons
                g2d.setColor(new Color(60, 180, 120));
            }
        } else {
            if (!inPattern) {
                g2d.setColor(UIHelper.darkGray);
            } else {
                // Base color for inactive buttons
                g2d.setColor(new Color(60, 60, 60));
            }
        }

        // Fill the base button
        g2d.fillRect(0, 0, width, height);

        // Only draw parameter visualization if the button is selected and in pattern
        if (isSelected() && inPattern) {
            // Draw probability as overall opacity
            if (showProbability && probability < 100) {
                // Create a semi-transparent overlay
                Color overlayColor = new Color(0, 0, 0, 255 - getProbabilityAlpha());
                g2d.setColor(overlayColor);
                g2d.fillRect(0, 0, width, height);
            }

            // Draw velocity as a vertical bar
            if (showVelocity) {
                // Calculate height based on velocity (0-127)
                int velocityHeight = (int) (height * (velocity / 127.0));
                g2d.setColor(new Color(255, 255, 0, 100)); // Semi-transparent yellow
                g2d.fillRect(0, height - velocityHeight, width / 4, velocityHeight);
            }

            // Draw decay as a horizontal bar
            if (showDecay) {
                // Calculate width based on decay (normalized to reasonable range)
                int decayWidth = Math.min(width - 2, (int) (width * (decay / 500.0)));
                g2d.setColor(Color.BLUE); // Semi-transparent green
                // g2d.setColor(new Color(0, 200, 0, 100)); // Semi-transparent green
                g2d.fillRect(width / 4, height / 2, decayWidth, height / 4);
            }

            // Draw nudge as a position marker
            if (showNudge && nudge != 0) {
                int centerX = width / 2;
                int nudgeOffset = (int) (width * (nudge / 100.0)); // Normalize to reasonable range

                g2d.setColor(new Color(255, 0, 0, 150)); // Semi-transparent red
                g2d.fillRect(centerX + nudgeOffset - 1, 0, 3, height / 4);
            }

            // Draw effects indicators if enabled
            if (showEffects) {
                // Draw pan as position (left to right)
                int panX = (int) (width * (pan / 127.0));
                g2d.setColor(new Color(0, 0, 255, 150)); // Semi-transparent blue
                g2d.fillRect(panX - 1, height - 4, 3, 3);

                // Draw reverb and chorus as small indicators in corners if above threshold
                if (reverb > 20) {
                    g2d.setColor(new Color(128, 0, 128, 150)); // Semi-transparent purple
                    g2d.fillRect(0, 0, 4, 4);
                }

                if (chorus > 20) {
                    g2d.setColor(new Color(0, 128, 128, 150)); // Semi-transparent teal
                    g2d.fillRect(width - 4, 0, 4, 4);
                }
            }

            // Debug output - draw step number
            if (stepIndex >= 0) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g2d.drawString(String.valueOf(stepIndex + 1), width / 2 - 3, height / 2 + 3);
            }
        }

        // If accented, draw a small red square in the top right corner
        if (accented) {
            g2d.setColor(Color.RED);
            int squareSize = Math.max(4, Math.min(width, height) / 5);
            g2d.fillRect(width - squareSize - 1, 1, squareSize, squareSize);
        }

        // If using temporary state, draw a border
        if (isTemporary) {
            g2d.setColor(temporaryColor);
            g2d.drawRect(0, 0, width - 1, height - 1);
        }
    }
}
