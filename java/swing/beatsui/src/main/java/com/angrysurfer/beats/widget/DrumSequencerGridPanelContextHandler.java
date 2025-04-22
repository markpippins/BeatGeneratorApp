package com.angrysurfer.beats.widget;

import java.awt.Component;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.panel.sequencer.poly.DrumSequencerPanel;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.sequencer.DrumSequenceModifier;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Handler for context menu operations on the drum sequencer grid
 */
public class DrumSequencerGridPanelContextHandler {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerGridPanelContextHandler.class);

    // References to required components
    private final DrumSequencer sequencer;
    private final DrumSequencerPanel parentPanel;

    /**
     * Create a new context menu handler
     * 
     * @param sequencer   The drum sequencer
     * @param parentPanel The parent panel for callbacks
     */
    public DrumSequencerGridPanelContextHandler(DrumSequencer sequencer, DrumSequencerPanel parentPanel) {
        this.sequencer = sequencer;
        this.parentPanel = parentPanel;
    }

    /**
     * Display a context menu for a step button
     * 
     * @param component The component that triggered the context menu
     * @param x         The x position to show the menu
     * @param y         The y position to show the menu
     * @param drumIndex The index of the drum for this step
     * @param step      The step index
     */
    public void showContextMenu(Component component, int x, int y, int drumIndex, int step) {
        JPopupMenu menu = new JPopupMenu();

        // Add menu items for step operations
        JMenuItem fillItem = new JMenuItem("Fill From Here...");
        fillItem.addActionListener(e -> {
            // Use CommandBus for dialog creation
            Object[] params = new Object[] { sequencer, drumIndex, step };
            CommandBus.getInstance().publish(Commands.SHOW_FILL_DIALOG, this, params);
        });

        JMenuItem clearRowItem = new JMenuItem("Clear Row");
        clearRowItem.addActionListener(e -> parentPanel.clearRow(drumIndex));

        // Add Copy to New sequence option
        JMenuItem copyToNewItem = new JMenuItem("Copy Sequence to New...");
        copyToNewItem.addActionListener(e -> copyToNewSequence(drumIndex));

        // Add Double Pattern option
        JMenuItem doublePatternItem = new JMenuItem("Double Pattern");
        // Only enable if doubling is possible
        int currentLength = sequencer.getPatternLength(drumIndex);
        int maxLength = sequencer.getMaxPatternLength();
        boolean canDouble = currentLength * 2 <= maxLength;
        doublePatternItem.setEnabled(canDouble);
        doublePatternItem.addActionListener(e -> doublePattern(drumIndex));

        // Add Set Max Length option
        JMenuItem setMaxLengthItem = new JMenuItem("Set Max Length...");
        setMaxLengthItem.addActionListener(e -> {
            // Use CommandBus for dialog creation
            CommandBus.getInstance().publish(Commands.SHOW_MAX_LENGTH_DIALOG, this, sequencer);
        });

        // Add pattern generation items
        JMenuItem patternItem = new JMenuItem("Apply Pattern...");
        patternItem.addActionListener(e -> showPatternDialog(drumIndex));

        // Add Euclidean Pattern option
        JMenuItem euclideanItem = new JMenuItem("Euclidean Pattern...");
        euclideanItem.addActionListener(e -> {
            // Use CommandBus for dialog creation
            Object[] params = new Object[] { sequencer, drumIndex };
            CommandBus.getInstance().publish(Commands.SHOW_EUCLIDEAN_DIALOG, this, params);
        });

        menu.add(clearRowItem);
        menu.add(copyToNewItem);
        menu.addSeparator();
        menu.add(doublePatternItem);
        menu.addSeparator();

        JMenu fillMenu = new JMenu("Fill..");
        fillMenu.add(fillItem);
        fillMenu.add(patternItem);
        fillMenu.add(euclideanItem);
        menu.add(fillMenu);

        menu.addSeparator();
        menu.add(setMaxLengthItem);

        menu.show(component, x, y);
    }

    /**
     * Shows a dialog to choose pattern type
     * 
     * @param drumIndex The drum index to apply the pattern to
     */
    private void showPatternDialog(int drumIndex) {
        Object[] options = { "Every 2nd Step", "Every 3rd Step", "Every 4th Step" };
        int choice = JOptionPane.showOptionDialog(
                parentPanel,
                "Choose pattern type:",
                "Pattern Generator",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice >= 0) {
            applyPatternEveryN(drumIndex, 2 + choice);
        }
    }

    /**
     * Apply a pattern that activates every Nth step
     * 
     * @param drumIndex The drum index to apply the pattern to
     * @param n         The step interval
     */
    private void applyPatternEveryN(int drumIndex, int n) {
        boolean success = DrumSequenceModifier.applyPatternEveryN(sequencer, drumIndex, n);
        if (success) {
            parentPanel.updateStepButtonsForDrum(drumIndex);
        }
    }

    /**
     * Copy the current sequence to a new sequence
     * 
     * @param drumIndex The drum index to copy
     */
    private void copyToNewSequence(int drumIndex) {
        // Implementation for copying sequence to a new one
        logger.info("Copying sequence for drum index {} to a new sequence.", drumIndex);
    }

    /**
     * Double the current pattern for the given drum index
     * 
     * @param drumIndex The drum index to double the pattern for
     */
    private void doublePattern(int drumIndex) {
        try {
            int currentLength = sequencer.getPatternLength(drumIndex);
            int maxLength = sequencer.getMaxPatternLength();
            
            // Safety check - make sure doubling is possible
            if (currentLength * 2 > maxLength) {
                JOptionPane.showMessageDialog(parentPanel, 
                    "Cannot double pattern - maximum length would be exceeded.", 
                    "Double Pattern", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Copy the pattern
            boolean[] activeSteps = new boolean[currentLength];
            int[] velocities = new int[currentLength];
            int[] decays = new int[currentLength];
            int[] probabilities = new int[currentLength];
            int[] nudges = new int[currentLength];
            
            // Get current values
            for (int i = 0; i < currentLength; i++) {
                activeSteps[i] = sequencer.isStepActive(drumIndex, i);
                velocities[i] = sequencer.getStepVelocity(drumIndex, i);
                decays[i] = sequencer.getStepDecay(drumIndex, i);
                probabilities[i] = sequencer.getStepProbability(drumIndex, i);
                nudges[i] = sequencer.getStepNudge(drumIndex, i);
            }
            
            // Double the pattern length
            int newLength = currentLength * 2;
            sequencer.setPatternLength(drumIndex, newLength);
            
            // Copy the pattern to the second half
            for (int i = 0; i < currentLength; i++) {
                // Set first half (should already be set, but to be safe)
                sequencer.isStepActive(drumIndex, i);
                sequencer.setStepVelocity(drumIndex, i, velocities[i]);
                sequencer.setStepDecay(drumIndex, i, decays[i]);
                sequencer.setStepProbability(drumIndex, i, probabilities[i]);
                sequencer.setStepNudge(drumIndex, i, nudges[i]);
                
                // Copy to second half
                int destIndex = i + currentLength;
                if (activeSteps[i]) {
                    sequencer.toggleStep(drumIndex, destIndex); // Set active if original was active
                }
                sequencer.setStepVelocity(drumIndex, destIndex, velocities[i]);
                sequencer.setStepDecay(drumIndex, destIndex, decays[i]);
                sequencer.setStepProbability(drumIndex, destIndex, probabilities[i]);
                sequencer.setStepNudge(drumIndex, destIndex, nudges[i]);
            }
            
            // Update UI
            CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_UPDATED, 
                this,
                null
            );
            
            // Log the action
            logger.info("Doubled pattern for drum {} from length {} to {}", 
                       drumIndex, currentLength, newLength);
        } catch (Exception ex) {
            logger.error("Error doubling pattern: {}", ex.getMessage(), ex);
            JOptionPane.showMessageDialog(parentPanel, 
                "Error doubling pattern: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}