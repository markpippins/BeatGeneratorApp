package com.angrysurfer.beats.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.widget.DrumSequencerGridButton;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.sequencer.DrumSequenceModifier;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Panel containing the drum sequencing grid buttons
 */
public class DrumSequencerGridPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerGridPanel.class);
    
    // Core components
    private final List<DrumSequencerGridButton> triggerButtons = new ArrayList<>();
    private final DrumSequencerGridButton[][] gridButtons;
    private final DrumSequencer sequencer;
    private final DrumSequencerPanel parentPanel;
    
    // UI constants
    private static final int DRUM_PAD_COUNT = DrumSequencer.DRUM_PAD_COUNT;
    private static final int GRID_BUTTON_SIZE = 24;
    
    // UI state
    private boolean isPlaying = false;
    private boolean debugMode = false;
    
    /**
     * Create a new DrumSequencerGridPanel
     * @param sequencer The drum sequencer
     * @param parentPanel The parent panel for callbacks
     */
    public DrumSequencerGridPanel(DrumSequencer sequencer, DrumSequencerPanel parentPanel) {
        this.sequencer = sequencer;
        this.parentPanel = parentPanel;
        
        // Use GridLayout for perfect grid alignment
        setLayout(new GridLayout(DRUM_PAD_COUNT, sequencer.getDefaultPatternLength(), 2, 2));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Initialize grid buttons array
        gridButtons = new DrumSequencerGridButton[DRUM_PAD_COUNT][sequencer.getDefaultPatternLength()];
        
        // Create the grid buttons
        createGridButtons();
    }
    
    /**
     * Create all grid buttons
     */
    private void createGridButtons() {
        // Create grid buttons
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
                DrumSequencerGridButton button = createStepButton(drumIndex, step);

                // IMPORTANT: Set initial state based on sequencer
                boolean isInPattern = step < sequencer.getPatternLength(drumIndex);
                boolean isActive = sequencer.isStepActive(drumIndex, step);

                // Configure button
                button.setEnabled(isInPattern);
                button.setSelected(isActive);
                button.setVisible(true);

                // Add to panel and tracking list
                add(button);
                triggerButtons.add(button);

                // Also store in the 2D array for direct access by coordinates
                gridButtons[drumIndex][step] = button;
            }
        }
    }
    
    /**
     * Create step button with proper behavior
     */
    private DrumSequencerGridButton createStepButton(int drumIndex, int step) {
        DrumSequencerGridButton button = new DrumSequencerGridButton();

        // Make button square with constant size
        button.setPreferredSize(new Dimension(GRID_BUTTON_SIZE, GRID_BUTTON_SIZE));

        // Add debug info if needed
        if (debugMode) {
            button.setText(String.format("%d,%d", drumIndex, step));
            button.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 8));
        }

        // Add action listener to toggle step state
        button.addActionListener(e -> {
            sequencer.toggleStep(drumIndex, step);
            button.setSelected(sequencer.isStepActive(drumIndex, step));
        });

        // Add right-click context menu
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    showContextMenu(e.getComponent(), e.getX(), e.getY(), drumIndex, step);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    showContextMenu(e.getComponent(), e.getX(), e.getY(), drumIndex, step);
                }
            }
        });

        return button;
    }
    
    /**
     * Update appearance of an entire drum row
     */
    public void updateRowAppearance(int drumIndex, boolean isSelected) {
        int patternLength = sequencer.getPatternLength(drumIndex);

        for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
            int buttonIndex = (drumIndex * sequencer.getDefaultPatternLength()) + step;
            if (buttonIndex >= 0 && buttonIndex < triggerButtons.size()) {
                DrumSequencerGridButton button = triggerButtons.get(buttonIndex);

                // Keep all buttons visible
                button.setVisible(true);

                // Style based on whether step is active and in pattern
                boolean isInPattern = step < patternLength;
                boolean isActive = sequencer.isStepActive(drumIndex, step);

                // Update button appearance using the button's own functionality
                button.setEnabled(isInPattern);
                button.setSelected(isActive);

                // Add subtle highlighting to the selected row
                if (isSelected) {
                    // Highlight the selected row's border
                    button.setBorder(BorderFactory.createLineBorder(
                            UIUtils.dustyAmber, 1));
                } else {
                    // Normal border for other rows
                    button.setBorder(BorderFactory.createLineBorder(
                            Color.DARK_GRAY, 1));
                }
            }
        }
    }
    
    /**
     * Update all step buttons for a specific drum
     */
    public void updateStepButtonsForDrum(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT || triggerButtons.isEmpty()) {
            // Invalid drum index or buttons not initialized yet
            logger.warn("Cannot update step buttons: invalid drum index {} or buttons not initialized", drumIndex);
            return;
        }

        // Get pattern length for this drum
        int patternLength = sequencer.getPatternLength(drumIndex);
        logger.debug("Updating step buttons for drum {} with pattern length {}", drumIndex, patternLength);

        // Update all buttons for this row
        for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
            int buttonIndex = (drumIndex * sequencer.getDefaultPatternLength()) + step;

            // Safety check
            if (buttonIndex >= 0 && buttonIndex < triggerButtons.size()) {
                DrumSequencerGridButton button = triggerButtons.get(buttonIndex);

                // Make button visible regardless of pattern length - CRITICAL FIX
                button.setVisible(true);

                // Update button state based on pattern
                boolean isInPattern = step < patternLength;
                boolean isActive = isInPattern && sequencer.isStepActive(drumIndex, step);

                // Update button state
                button.setEnabled(isInPattern);
                button.setSelected(isActive);

                // Style based on whether step is active and in pattern
                if (!isInPattern) {
                    button.setBackground(UIUtils.charcoalGray);
                } else {
                    if (isActive) {
                        button.setBackground(UIUtils.deepOrange);
                    } else {
                        button.setBackground(UIUtils.slateGray);
                    }
                }

                // Always repaint
                button.repaint();
            }
        }
    }
    
    /**
     * Display a context menu for a step button
     */
    private void showContextMenu(Component component, int x, int y, int drumIndex, int step) {
        JPopupMenu menu = new JPopupMenu();

        // Add menu items for step operations
        JMenuItem fillItem = new JMenuItem("Fill From Here...");
        fillItem.addActionListener(e -> {
            // Use CommandBus instead of direct dialog creation
            Object[] params = new Object[] { sequencer, drumIndex, step };
            CommandBus.getInstance().publish(Commands.SHOW_FILL_DIALOG, this, params);
        });
        menu.add(fillItem);

        JMenuItem clearRowItem = new JMenuItem("Clear Row");
        clearRowItem.addActionListener(e -> parentPanel.clearRow(drumIndex));
        menu.add(clearRowItem);
        
        // Add Set Max Length option
        JMenuItem setMaxLengthItem = new JMenuItem("Set Max Length...");
        setMaxLengthItem.addActionListener(e -> {
            // Use CommandBus instead of direct dialog creation
            CommandBus.getInstance().publish(Commands.SHOW_MAX_LENGTH_DIALOG, this, sequencer);
        });
        menu.add(setMaxLengthItem);

        // Add divider
        menu.addSeparator();

        // Add pattern generation items
        JMenuItem patternItem = new JMenuItem("Apply Pattern...");
        patternItem.addActionListener(e -> {
            Object[] options = { "Every 2nd Step", "Every 3rd Step", "Every 4th Step" };
            int choice = javax.swing.JOptionPane.showOptionDialog(
                    this,
                    "Choose pattern type:",
                    "Pattern Generator",
                    javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice >= 0) {
                applyPatternEveryN(drumIndex, 2 + choice);
            }
        });
        menu.add(patternItem);

        // Add Euclidean Pattern option
        JMenuItem euclideanItem = new JMenuItem("Euclidean Pattern...");
        euclideanItem.addActionListener(e -> {
            // Use CommandBus instead of direct dialog creation
            Object[] params = new Object[] { sequencer, drumIndex };
            CommandBus.getInstance().publish(Commands.SHOW_EUCLIDEAN_DIALOG, this, params);
        });
        menu.add(euclideanItem);

        // Show the menu
        menu.show(component, x, y);
    }
    
    /**
     * Apply a pattern that activates every Nth step
     */
    private void applyPatternEveryN(int drumIndex, int n) {
        boolean success = DrumSequenceModifier.applyPatternEveryN(sequencer, drumIndex, n);
        if (success) {
            updateStepButtonsForDrum(drumIndex);
        }
    }
    
    /**
     * Update step highlighting during playback
     */
    public void updateStepHighlighting(int drumIndex, int oldStep, int newStep) {
        // Only update highlighting if we're actually playing
        if (!isPlaying) {
            return;
        }

        // Ensure we're on the EDT for UI updates
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateStepHighlighting(drumIndex, oldStep, newStep));
            return;
        }

        // Calculate button indices based on the drum and step
        int oldButtonIndex = drumIndex * sequencer.getDefaultPatternLength() + oldStep;
        int newButtonIndex = drumIndex * sequencer.getDefaultPatternLength() + newStep;

        // Ensure indices are valid
        if (oldButtonIndex >= 0 && oldButtonIndex < triggerButtons.size()) {
            DrumSequencerGridButton oldButton = triggerButtons.get(oldButtonIndex);
            if (oldButton != null) {
                oldButton.setHighlighted(false);
            }
        }

        if (newButtonIndex >= 0 && newButtonIndex < triggerButtons.size()) {
            DrumSequencerGridButton newButton = triggerButtons.get(newButtonIndex);
            if (newButton != null) {
                newButton.setHighlighted(true);
            }
        }
    }
    
    /**
     * Clear all step highlighting across all drum rows
     */
    public void clearAllStepHighlighting() {
        for (DrumSequencerGridButton button : triggerButtons) {
            button.clearTemporaryState();
            button.repaint();
        }
    }
    
    /**
     * Set playing state to control step highlighting
     */
    public void setPlayingState(boolean isPlaying) {
        this.isPlaying = isPlaying;
        
        if (!isPlaying) {
            clearAllStepHighlighting();
        }
    }
    
    /**
     * Refresh the entire grid UI to match the current sequencer state
     */
    public void refreshGridUI() {
        if (triggerButtons == null || triggerButtons.isEmpty()) {
            logger.warn("Cannot refresh grid UI - triggerButtons list is empty");
            return;
        }

        logger.info("Refreshing entire grid UI for sequence {}", sequencer.getDrumSequenceId());

        try {
            // Ensure we refresh ALL drums and ALL steps
            for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
                    int buttonIndex = drumIndex * sequencer.getDefaultPatternLength() + step;

                    if (buttonIndex < triggerButtons.size()) {
                        DrumSequencerGridButton button = triggerButtons.get(buttonIndex);

                        if (button != null) {
                            // Get the current state from the sequencer
                            boolean active = sequencer.isStepActive(drumIndex, step);

                            // Force update button state without triggering events
                            button.setToggled(active);
                            button.setHighlighted(false); // Clear any highlighting
                            button.repaint(); // Force immediate repaint
                        }
                    }
                }

                // Update the drum row's appearance
                updateRowAppearance(drumIndex, drumIndex == parentPanel.getSelectedPadIndex());
            }
        } finally {
            // Any cleanup code if needed
        }

        // Ensure proper visual refresh
        revalidate();
        repaint();
    }
    
    /**
     * Toggle debug mode to show grid indices
     */
    public void toggleDebugMode() {
        debugMode = !debugMode;

        // Show indices on buttons in debug mode
        if (triggerButtons != null) {
            for (int i = 0; i < triggerButtons.size(); i++) {
                DrumSequencerGridButton button = triggerButtons.get(i);
                int drumIndex = i / sequencer.getDefaultPatternLength();
                int stepIndex = i % sequencer.getDefaultPatternLength();

                if (debugMode) {
                    button.setText(drumIndex + "," + stepIndex);
                    button.setForeground(Color.YELLOW);
                } else {
                    button.setText("");
                }
            }
        }
    }
    
    /**
     * Get the list of trigger buttons
     */
    public List<DrumSequencerGridButton> getTriggerButtons() {
        return triggerButtons;
    }
    
    /**
     * Get the 2D array of grid buttons
     */
    public DrumSequencerGridButton[][] getGridButtons() {
        return gridButtons;
    }
}