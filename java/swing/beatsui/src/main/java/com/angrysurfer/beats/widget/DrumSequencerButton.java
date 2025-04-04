package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.DrumPadSelectionEvent;
import com.angrysurfer.core.sequencer.DrumSequencer;

import lombok.Getter;
import lombok.Setter;

/**
 * A specialized DrumButton for the drum sequencer that handles selection state
 */
@Getter
@Setter
public class DrumSequencerButton extends DrumButton implements IBusListener {
    
    private int drumPadIndex;
    private DrumSequencer sequencer;
    private boolean isSelected = false;
    private boolean isPressed = false;
    private Color selectedColor = new Color(255, 140, 0); // Orange selected color
    private Color normalColor = new Color(50, 130, 200);  // Default blue color
    private Color pressedColor = new Color(120, 180, 240); // Lighter blue when pressed
    
    /**
     * Create a new drum sequencer button
     * 
     * @param drumPadIndex The index of the drum pad (0-15)
     * @param sequencer The drum sequencer instance
     */
    public DrumSequencerButton(int drumPadIndex, DrumSequencer sequencer) {
        super();
        this.drumPadIndex = drumPadIndex;
        this.sequencer = sequencer;
        
        // Override toggle behavior from parent class
        setToggle(false); // Important: disable toggle behavior inherited from parent
        
        // Set default color
        setDefaultColor(normalColor);
        
        // Register for command bus events to track selection changes
        CommandBus.getInstance().register(this);
        
        // Replace existing action listeners to prevent toggle behavior
        for (java.awt.event.ActionListener al : getActionListeners()) {
            removeActionListener(al);
        }
        
        // Add mouse listener for visual feedback and selection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
                
                // Only handle selection if mouse is still over the button
                if (contains(e.getPoint())) {
                    System.out.println("DrumSequencerButton: selecting pad " + drumPadIndex); // Debug
                    
                    // Call the sequencer's selectDrumPad method
                    if (sequencer != null) {
                        sequencer.selectDrumPad(drumPadIndex);
                    } else {
                        System.err.println("Error: sequencer is null!");
                    }
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                // Reset pressed state if mouse exits
                isPressed = false;
                repaint();
            }
        });
        
        // Add right-click listener for previewing sounds
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    // Right-click to preview sound
                    Strike strike = sequencer.getStrike(drumPadIndex);
                    if (strike != null && sequencer.getNoteEventListener() != null) {
                        sequencer.getNoteEventListener().accept(
                            new com.angrysurfer.core.sequencer.NoteEvent(
                                strike.getRootNote(), 
                                127, // Full velocity 
                                250  // 250ms duration
                            )
                        );
                    }
                }
            }
        });
    }
    
    /**
     * Override the paint method to handle the various visual states
     */
    @Override
    public void paint(Graphics g) {
        // Get the current UI state
        boolean currentPressed = isPressed;
        boolean currentSelected = isSelected;
        
        // Determine the color based on button state
        Color buttonColor;
        if (currentPressed) {
            buttonColor = pressedColor;
        } else if (currentSelected) {
            buttonColor = selectedColor;
        } else {
            buttonColor = normalColor;
        }
        
        // Set the current color for painting
        setDefaultColor(buttonColor);
        
        // Call the parent paint method
        super.paint(g);
        
        // Add a selection indicator if selected
        if (currentSelected) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(255, 255, 255, 100)); // White, semi-transparent
            
            int w = getWidth();
            int h = getHeight();
            int thickness = 3;
            
            g2d.fillRect(0, 0, w, thickness); // Top
            g2d.fillRect(0, h - thickness, w, thickness); // Bottom
            g2d.fillRect(0, 0, thickness, h); // Left
            g2d.fillRect(w - thickness, 0, thickness, h); // Right
            
            g2d.dispose();
        }
    }
    
    /**
     * Set the drum name and tooltip
     * 
     * @param name The name of the drum
     */
    public void setDrumName(String name) {
        setText(name);
        setToolTipText(name + " (Pad " + (drumPadIndex + 1) + ")");
    }
    
    /**
     * Set the selected state of this button
     * 
     * @param selected Whether this button is selected
     */
    @Override
    public void setSelected(boolean selected) {
        if (this.isSelected != selected) {
            this.isSelected = selected;
            repaint();
        }
    }

    /**
     * Handle command bus events, particularly drum pad selection
     */
    @Override
    public void onAction(Command action) {
        if (Commands.DRUM_PAD_SELECTED.equals(action.getCommand())) {
            if (action.getData() instanceof DrumPadSelectionEvent event) {
                // Update our appearance if this is the newly selected pad
                boolean isNowSelected = (event.getNewSelection() == drumPadIndex);
                if (isSelected != isNowSelected) {
                    setSelected(isNowSelected);
                }
            }
        }
    }
}