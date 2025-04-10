package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.DefaultButtonModel;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class DrumSequencerGridButton extends JToggleButton {
    
    private int stepIndex;
    private boolean toggled = false;
    private boolean highlighted = false;
    private boolean isHighlighted = false;
    private int drumPadIndex = -1; // Default to -1 for unassigned index
    private boolean isTemporary = false;
    private Color normalColor;
    private Color temporaryColor = new Color(200, 150, 40); // Amber highlight
    private boolean inPattern = true;

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
     * Create a new trigger button with step index
     * 
     * @param stepIndex The step index
     */
    public DrumSequencerGridButton(int stepIndex) {
        super();
        this.stepIndex = stepIndex;
        setPreferredSize(new Dimension(30, 30));
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(true);
        updateAppearance();
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
        this.highlighted = highlighted;
        this.isHighlighted = highlighted;
        updateAppearance();
    }
    
    /**
     * Set the toggled state of this button
     */
    public void setToggled(boolean toggled) {
        this.toggled = toggled;
        // Always make sure the button is set to be visible and opaque first
        setVisible(true);
        setOpaque(true);
        
        // Set the selected state
        setSelected(toggled);
        
        // Update background color based on state
        updateAppearance();
        
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
        return highlighted;
    }

    /**
     * Check if the button is toggled on
     * 
     * @return true if toggled on, false otherwise
     */
    public boolean isToggled() {
        return toggled;
    }

    /**
     * Get the step index
     * 
     * @return The step index
     */
    public int getStepIndex() {
        return stepIndex;
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
            setBackground(ColorUtils.darkGray);
            return;
        }
        
        if (!inPattern) {
            // Subdued appearance for steps outside the pattern length
            setBackground(ColorUtils.charcoalGray);
            setBorder(BorderFactory.createLineBorder(ColorUtils.slateGray, 1));
            return;
        }
        
        // Regular appearance for steps in the pattern
        if (highlighted) {
            setBackground(toggled ? ColorUtils.fadedOrange : ColorUtils.slateGray);
        } else {
            setBackground(toggled ? ColorUtils.dustyAmber : ColorUtils.charcoalGray);
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
}
