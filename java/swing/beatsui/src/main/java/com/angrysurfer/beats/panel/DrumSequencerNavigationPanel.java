package com.angrysurfer.beats.panel;

import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.service.DrumSequencerManager;

/**
 * Panel providing navigation controls for drum sequences
 */
public class DrumSequencerNavigationPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerNavigationPanel.class);

    private JLabel sequenceIdLabel;
    private JButton firstButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton lastButton;
    private JButton saveButton;

    private final DrumSequencer sequencer;
    private final DrumSequencerManager manager;

    public DrumSequencerNavigationPanel(DrumSequencer sequencer) {
        // Match the MelodicSequencerNavigationPanel layout
        super(new FlowLayout(FlowLayout.CENTER, 2, 0));
        
        this.sequencer = sequencer;
        this.manager = DrumSequencerManager.getInstance();
        
        // Add titled border to match MelodicSequencerNavigationPanel
        setBorder(BorderFactory.createTitledBorder("Sequence"));
        
        initializeUI();
        
        // Register with command bus to receive sequence updates
        CommandBus.getInstance().register(this);
    }

    private void initializeUI() {
        // First sequence button
        firstButton = createButton("⏮", "First Sequence", e -> loadFirstSequence());

        // Previous sequence button
        prevButton = createButton("◀", "Previous Sequence", e -> loadPreviousSequence());

        // Sequence ID display
        sequenceIdLabel = new JLabel(getFormattedIdText());
        sequenceIdLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        // Next sequence button
        nextButton = createButton("▶", "Next Sequence", e -> loadNextSequence());

        // Last sequence button
        lastButton = createButton("⏭", "Last Sequence", e -> loadLastSequence());

        // Save button
        saveButton = createButton("💾", "Save Sequence", e -> saveCurrentSequence());

        // Add components to panel
        add(firstButton);
        add(prevButton);
        add(sequenceIdLabel);
        add(nextButton);
        add(lastButton);
        add(saveButton);

        // Initialize button states
        updateButtonStates();
    }

    private JButton createButton(String text, String tooltip, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.addActionListener(listener);
        return button;
    }

    /**
     * Update the ID display with current sequence ID
     */
    public void updateSequenceIdDisplay() {
        sequenceIdLabel.setText(getFormattedIdText());
        updateButtonStates();
    }

    private String getFormattedIdText() {
        long id = sequencer.getDrumSequenceId();
        return id <= 0 ? "New" : "Seq " + id;
    }

    /**
     * Enable/disable buttons based on current sequence position
     */
    private void updateButtonStates() {
        long currentId = sequencer.getDrumSequenceId();
        boolean hasSequences = manager.hasSequences();

        // Get first/last sequence IDs
        Long firstId = manager.getFirstSequenceId();
        Long lastId = manager.getLastSequenceId();

        // Special case for new sequences (ID <= 0)
        boolean isNewSequence = currentId <= 0;

        // First/Previous buttons - enabled if we're not at the first sequence
        // AND we have sequences in the database AND we're not editing a new sequence
        boolean isFirst = isNewSequence ? false : (firstId != null && currentId <= firstId);

        // Last button - only enabled if we're not at the last sequence
        boolean isLast = isNewSequence ? true : (lastId != null && currentId >= lastId);

        // Enable previous/first buttons if we're on a new sequence AND we have sequences in DB
        if (isNewSequence && hasSequences) {
            firstButton.setEnabled(true);
            prevButton.setEnabled(true);
        } else {
            firstButton.setEnabled(hasSequences && !isFirst);
            prevButton.setEnabled(hasSequences && !isFirst);
        }

        nextButton.setEnabled(true);  // Always enable the next button
        lastButton.setEnabled(hasSequences && !isLast);

        logger.info("Button states: currentId={}, firstId={}, lastId={}, isFirst={}, isLast={}, hasSequences={}, isNew={}",
                currentId, firstId, lastId, isFirst, isLast, hasSequences, isNewSequence);
    }

    /**
     * Load the sequence with the given ID
     */
    private void loadSequence(Long sequenceId) {
        if (sequenceId != null) {
            // Use the manager directly to load the sequence
            manager.loadSequence(sequenceId);

            // Reset the sequencer to ensure proper step indicator state
            sequencer.reset();

            // Update UI
            updateSequenceIdDisplay();

            CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_LOADED, 
                this, 
                sequencer.getDrumSequenceId()
            );
        }
    }
    
    // Load first/previous/next/last sequence methods...
    
    private void loadFirstSequence() {
        Long firstId = manager.getFirstSequenceId();
        if (firstId != null) {
            loadSequence(firstId);
        }
    }

    private void loadPreviousSequence() {
        Long prevId = manager.getPreviousSequenceId(sequencer.getDrumSequenceId());
        if (prevId != null) {
            loadSequence(prevId);
        }
    }

    private void loadNextSequence() {
        Long nextId = manager.getNextSequenceId(sequencer.getDrumSequenceId());
        if (nextId != null) {
            loadSequence(nextId);
        } else {
            // We're at the last saved sequence, so create a new blank one
            sequencer.setDrumSequenceId(0); // Set to 0 to indicate new unsaved sequence
            logger.info("Creating new sequence, ID set to 0");

            // Reset the sequencer
            sequencer.reset();

            // Clear all patterns
            for (int drumIndex = 0; drumIndex < DrumSequencer.DRUM_PAD_COUNT; drumIndex++) {
                for (int step = 0; step < 64; step++) { // Clear all steps up to max
                    if (sequencer.isStepActive(drumIndex, step)) {
                        sequencer.toggleStep(drumIndex, step); // Turn off any active steps
                    }
                }
            }

            // Reset to default parameters
            for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                sequencer.setLooping(i, true);
                sequencer.setDirection(i, Direction.FORWARD);
                sequencer.setPatternLength(i, 16);
                sequencer.setTimingDivision(i, TimingDivision.NORMAL);
            }

            // Update UI AFTER all changes
            SwingUtilities.invokeLater(() -> {
                updateSequenceIdDisplay();
                updateButtonStates();
            });
            
            CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_CREATED,
                this,
                sequencer.getDrumSequenceId()
            );
        }
    }

    private void loadLastSequence() {
        Long lastId = manager.getLastSequenceId();
        if (lastId != null) {
            loadSequence(lastId);
        }
    }

    private void saveCurrentSequence() {
        // Save the sequence
        manager.saveSequence();
        
        // After saving, get the latest sequence IDs from the database
        manager.refreshSequenceList();
        
        // Update display and button states
        updateSequenceIdDisplay();
        
        CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_SAVED,
                this,
                sequencer.getDrumSequenceId()
        );
        
        logger.info("Saved drum sequence: {}", sequencer.getDrumSequenceId());
    }
    
    /**
     * Implement IBusListener to respond to sequence-related events
     */
    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;
        
        switch (action.getCommand()) {
            case Commands.DRUM_SEQUENCE_LOADED, 
                 Commands.DRUM_SEQUENCE_CREATED, 
                 Commands.DRUM_SEQUENCE_SAVED,
                 Commands.DRUM_SEQUENCE_DELETED -> {
                // Update UI on EDT for thread safety
                SwingUtilities.invokeLater(() -> {
                    updateSequenceIdDisplay();
                    updateButtonStates();  // Explicitly update button states
                    logger.info("Updated UI after command: {}", action.getCommand());
                });
            }
        }
    }
}