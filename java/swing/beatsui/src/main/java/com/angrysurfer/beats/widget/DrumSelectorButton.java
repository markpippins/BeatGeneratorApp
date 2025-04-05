package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.DefaultButtonModel;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrumSelectorButton extends JToggleButton {

    private boolean isHighlighted = false;
    private int drumPadIndex = -1;
    private Color[] colors = {
        ColorUtils.charcoalGray,
        ColorUtils.slateGray,
        ColorUtils.deepNavy,
        ColorUtils.mutedOlive,
        ColorUtils.fadedLime,
        ColorUtils.dustyAmber,
        ColorUtils.warmMustard,
        ColorUtils.deepOrange,
        ColorUtils.agedOffWhite,
        ColorUtils.deepTeal,
        ColorUtils.darkGray,
        ColorUtils.warmGray,
        ColorUtils.mutedRed,
        ColorUtils.fadedOrange,
        ColorUtils.coolBlue,
        ColorUtils.warmOffWhite
    };

    /**
     * Create a new trigger button with label
     *
     * @param text The button text
     */
    public DrumSelectorButton(String text) {
        super(text);
        initialize();
    }

    /**
     * Create a new trigger button without label
     */
    public DrumSelectorButton() {
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
            setBackground(isSelected() ? drumPadIndex < 0 ? new Color(100, 200, 100) : colors[drumPadIndex] : Color.DARK_GRAY);
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
            setBackground(drumPadIndex < 0 ? new Color(100, 200, 100) : colors[drumPadIndex]); // Green for active steps
        } else {
            setBackground(Color.DARK_GRAY);  // Dark gray for inactive steps
        }

        // Make sure the change is visible
        repaint();
    }

    /**
     * Set whether this button is toggleable
     *
     * @param toggleable If true, the button will maintain its selected state
     * when clicked
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
}
