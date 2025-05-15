package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.DrumGridButton;
import com.angrysurfer.beats.widget.DrumSequencerGridPanelContextHandler;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.DrumSequencerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel containing the drum sequencing grid buttons
 */
public class DrumSequencerGridPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerGridPanel.class);
    // UI constants
    private static final int DRUM_PAD_COUNT = SequencerConstants.DRUM_PAD_COUNT;
    private static final int GRID_BUTTON_SIZE = 24;
    // Core components
    private final List<DrumGridButton> triggerButtons = new ArrayList<>();
    private final DrumGridButton[][] gridButtons;
    private final DrumSequencer sequencer;
    private final DrumSequencerPanel parentPanel;
    private final DrumSequencerGridPanelContextHandler contextMenuHandler;
    // UI state
    private boolean isPlaying = false;
    private boolean debugMode = false;

    /**
     * Create a new DrumSequencerGridPanel
     *
     * @param sequencer   The drum sequencer
     * @param parentPanel The parent panel for callbacks
     */
    public DrumSequencerGridPanel(DrumSequencer sequencer, DrumSequencerPanel parentPanel) {
        this.sequencer = sequencer;
        this.parentPanel = parentPanel;

        // Create the context menu handler
        this.contextMenuHandler = new DrumSequencerGridPanelContextHandler(sequencer, parentPanel);

        // Use GridLayout for perfect grid alignment
        // REDUCED: from 2,2 to 1,1 - tighter grid spacing for more compact appearance
        setLayout(new GridLayout(DRUM_PAD_COUNT, sequencer.getDefaultPatternLength(), 1, 1));
        // REDUCED: from 5,5,5,5 to 2,2,2,2 - consistent with other panels
        // setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        // setBorder(BorderFactory.createLoweredBevelBorder());
        // Initialize grid buttons array
        gridButtons = new DrumGridButton[DRUM_PAD_COUNT][sequencer.getDefaultPatternLength()];
        // Create the grid buttons
        createGridButtons();
        // Visualizer gridSaver = new Visualizer(this, gridButtons);
    }

    /**
     * Create all grid buttons
     */
    private void createGridButtons() {
        // Create grid buttons
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
                DrumGridButton button = createStepButton(drumIndex, step);

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
    private DrumGridButton createStepButton(int drumIndex, int step) {
        DrumGridButton button = new DrumGridButton();

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
                    contextMenuHandler.showContextMenu(e.getComponent(), e.getX(), e.getY(), drumIndex, step);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    contextMenuHandler.showContextMenu(e.getComponent(), e.getX(), e.getY(), drumIndex, step);
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
                DrumGridButton button = triggerButtons.get(buttonIndex);

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
                            UIHelper.dustyAmber, 1));
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
                DrumGridButton button = triggerButtons.get(buttonIndex);

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
                    button.setBackground(UIHelper.charcoalGray);
                } else {
                    if (isActive) {
                        button.setBackground(UIHelper.agedOffWhite);
                    } else {
                        button.setBackground(UIHelper.slateGray);
                    }
                }

                // Always repaint
                button.repaint();
            }
        }
    }

    /**
     * Update step highlighting during playback with position-based colors
     */
    public void updateStepHighlighting(int drumIndex, int oldStep, int newStep) {
        // Ensure we're on the EDT for UI updates
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateStepHighlighting(drumIndex, oldStep, newStep));
            return;
        }

        // Calculate button indices based on the drum and step
        int oldButtonIndex = drumIndex * sequencer.getDefaultPatternLength() + oldStep;
        int newButtonIndex = drumIndex * sequencer.getDefaultPatternLength() + newStep;

        // Un-highlight old step
        if (oldButtonIndex >= 0 && oldButtonIndex < triggerButtons.size()) {
            DrumGridButton oldButton = triggerButtons.get(oldButtonIndex);
            if (oldButton != null) {
                oldButton.setHighlighted(false);
                oldButton.repaint();
            }
        }

        // Highlight new step if valid and we're playing
        if (isPlaying && newButtonIndex >= 0 && newButtonIndex < triggerButtons.size()) {
            DrumGridButton newButton = triggerButtons.get(newButtonIndex);
            if (newButton != null) {
                // Choose color based on step position in the pattern
                Color highlightColor;

                if (newStep < 16) {
                    // First 16 steps - orange
                    highlightColor = UIHelper.fadedOrange;
                } else if (newStep < 32) {
                    // Steps 17-32 - blue
                    highlightColor = UIHelper.coolBlue;
                } else if (newStep < 48) {
                    // Steps 33-48 - navy
                    highlightColor = UIHelper.deepNavy;
                } else {
                    // Steps 49-64 - olive
                    highlightColor = UIHelper.mutedOlive;
                }

                newButton.setHighlighted(true);
                newButton.setHighlightColor(highlightColor);
                newButton.repaint();
            }
        }
    }

    /**
     * Clear all step highlighting across all drum rows
     */
    public void clearAllStepHighlighting() {
        for (DrumGridButton button : triggerButtons) {
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

        logger.info("Refreshing entire grid UI for sequence {}", sequencer.getData().getId());

        // Ensure we refresh ALL drums and ALL steps
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
                int buttonIndex = drumIndex * sequencer.getDefaultPatternLength() + step;

                if (buttonIndex < triggerButtons.size()) {
                    DrumGridButton button = triggerButtons.get(buttonIndex);

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
            updateRowAppearance(drumIndex, drumIndex == DrumSequencerManager.getInstance().getSelectedPadIndex());
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
                DrumGridButton button = triggerButtons.get(i);
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
    public List<DrumGridButton> getTriggerButtons() {
        return triggerButtons;
    }

    /**
     * Get the 2D array of grid buttons
     */
    public DrumGridButton[][] getGridButtons() {
        return gridButtons;
    }
}