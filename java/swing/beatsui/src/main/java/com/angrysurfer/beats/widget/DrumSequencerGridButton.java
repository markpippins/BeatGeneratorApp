package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.DefaultButtonModel;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import com.angrysurfer.beats.UIUtils;

public class DrumSequencerGridButton extends JButton {
    
    private boolean isHighlighted = false;
    private int drumPadIndex = -1; // Default to -1 for unassigned index
    private boolean isTemporary = false;
    private Color normalColor;
    private Color temporaryColor = new Color(200, 150, 40); // Amber highlight
    private boolean inPattern = true;
    private Color highlightColor = UIUtils.fadedOrange; // Default highlight color

    /**
     * Create a new trigger button with label
     * 
     * @param text The button text
     */
    public DrumSequencerGridButton(String text) {
        super(text);
        initialize();
    }
    
    /**
     * Create a new trigger button without label
     */
    public DrumSequencerGridButton() {
        super();
        initialize();
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
    }
    
    /**
     * Set whether this button is highlighted
     * 
     * @param highlighted true to highlight, false to unhighlight
     */
    public void setHighlighted(boolean highlighted) {
        this.isHighlighted = highlighted;
        if (highlighted) {
            setBackground(Color.ORANGE);
        } else {
            setBackground(isSelected() ? new Color(100, 200, 100) : Color.DARK_GRAY);
        }
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
        
        // Update background color based on state
        if (isHighlighted()) {
            setBackground(Color.ORANGE);
        } else if (toggled) {
            setBackground(new Color(100, 200, 100)); // Green for active steps
        } else {
            setBackground(Color.DARK_GRAY);  // Dark gray for inactive steps
        }
        
        // Make sure the change is visible
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
     * Check if the button is toggled on
     * 
     * @return true if toggled on, false otherwise
     */
    public boolean isToggled() {
        return isSelected();
    }

    /**
     * Set a temporary state during pattern drawing
     */
    public void setTemporaryState(boolean state) {
        isTemporary = state;
        if (state) {
            normalColor = getBackground();
            setBackground(temporaryColor);
        } else {
            setBackground(isSelected() ? getActiveColor() : getInactiveColor());
        }
        repaint();
    }

    /**
     * Clear temporary state after pattern is applied
     */
    public void clearTemporaryState() {
        isTemporary = false;
        setBackground(isSelected() ? getActiveColor() : getInactiveColor());
        repaint();
    }

    /**
     * Set whether this button is in the pattern
     * 
     * @param inPattern true if in pattern, false otherwise
     */
    public void setInPattern(boolean inPattern) {
        this.inPattern = inPattern;
        updateAppearance();
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
     * Update the appearance of the button based on its state
     */
    private void updateAppearance() {
        if (!isEnabled()) {
            setBackground(UIUtils.darkGray);
            return;
        }
        
        if (!inPattern) {
            // Subdued appearance for steps outside the pattern length
            setBackground(UIUtils.charcoalGray);
            setBorder(BorderFactory.createLineBorder(UIUtils.slateGray, 1));
            return;
        }
        
        // Regular appearance for steps in the pattern
        if (isSelected()) {
            setBackground(isHighlighted() ? UIUtils.dustyAmber : UIUtils.deepOrange);
        } else {
            setBackground(isHighlighted() ? UIUtils.fadedOrange : UIUtils.slateGray);
        }
    }

    // Helper methods - implement if not already present
    private Color getActiveColor() {
        return new Color(60, 180, 120); // Green when active
    }

    private Color getInactiveColor() {
        return new Color(60, 60, 60);   // Dark gray when inactive
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
            // Update appearance without firing events
            updateAppearance();
        }
    }

    /**
     * Set custom highlight color for this step button
     */
    public void setHighlightColor(Color color) {
        this.highlightColor = color;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        if (isHighlighted()) {
            g2d.setColor(highlightColor);
        } else if (isSelected()) {
            g2d.setColor(getActiveColor());
        } else {
            g2d.setColor(getInactiveColor());
        }
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }
}
