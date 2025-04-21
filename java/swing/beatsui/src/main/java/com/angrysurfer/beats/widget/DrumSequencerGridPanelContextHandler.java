package com.angrysurfer.beats.widget;

import java.awt.Component;
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
     * @param sequencer The drum sequencer
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
     * @param x The x position to show the menu
     * @param y The y position to show the menu
     * @param drumIndex The index of the drum for this step
     * @param step The step index
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
        menu.add(fillItem);

        JMenuItem clearRowItem = new JMenuItem("Clear Row");
        clearRowItem.addActionListener(e -> parentPanel.clearRow(drumIndex));
        menu.add(clearRowItem);
        
        // Add Set Max Length option
        JMenuItem setMaxLengthItem = new JMenuItem("Set Max Length...");
        setMaxLengthItem.addActionListener(e -> {
            // Use CommandBus for dialog creation
            CommandBus.getInstance().publish(Commands.SHOW_MAX_LENGTH_DIALOG, this, sequencer);
        });
        menu.add(setMaxLengthItem);

        // Add divider
        menu.addSeparator();

        // Add pattern generation items
        JMenuItem patternItem = new JMenuItem("Apply Pattern...");
        patternItem.addActionListener(e -> showPatternDialog(drumIndex));
        menu.add(patternItem);

        // Add Euclidean Pattern option
        JMenuItem euclideanItem = new JMenuItem("Euclidean Pattern...");
        euclideanItem.addActionListener(e -> {
            // Use CommandBus for dialog creation
            Object[] params = new Object[] { sequencer, drumIndex };
            CommandBus.getInstance().publish(Commands.SHOW_EUCLIDEAN_DIALOG, this, params);
        });
        menu.add(euclideanItem);

        // Show the menu
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
     * @param n The step interval
     */
    private void applyPatternEveryN(int drumIndex, int n) {
        boolean success = DrumSequenceModifier.applyPatternEveryN(sequencer, drumIndex, n);
        if (success) {
            parentPanel.updateStepButtonsForDrum(drumIndex);
        }
    }
}