package com.angrysurfer.beats.panel;

import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.service.DrumSequencerManager;

/**
 * Panel providing navigation controls for drum sequences
 */
public class DrumSequencerNavigationPanel extends JPanel {

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

        // First/Previous buttons - enabled if we're not at the first sequence
        boolean isFirst = !hasSequences || (firstId != null && currentId <= firstId);

        // Next button should ALWAYS be enabled - this allows creating new sequences
        // even when at the last saved sequence
        // Last button - only enabled if we're not at the last sequence
        boolean isLast = !hasSequences || (lastId != null && currentId >= lastId);

        firstButton.setEnabled(hasSequences && !isFirst);
        prevButton.setEnabled(hasSequences && !isFirst);
        nextButton.setEnabled(true);  // Always enable the next button
        lastButton.setEnabled(hasSequences && !isLast);

        logger.debug("Button states: currentId={}, firstId={}, lastId={}, isFirst={}, isLast={}",
                currentId, firstId, lastId, isFirst, isLast);
    }

    /**
     * Load the sequence with the given ID
     */
    private void loadSequence(Long sequenceId) {
        if (sequenceId != null) {
            // Load the sequence
            manager.loadSequence(sequenceId, sequencer);

            // Reset the sequencer to ensure proper step indicator state
            sequencer.reset();

            // Update UI
            updateSequenceIdDisplay();

            // Use consistent command for sequence loading notifications
            CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_LOADED, 
                this, 
                sequencer.getDrumSequenceId()
            );
        }
    }

    /**
     * Load the first available sequence
     */
    private void loadFirstSequence() {
        Long firstId = manager.getFirstSequenceId();
        if (firstId != null) {
            loadSequence(firstId);
        }
    }

    /**
     * Load the previous sequence
     */
    private void loadPreviousSequence() {
        Long prevId = manager.getPreviousSequenceId(sequencer.getDrumSequenceId());
        if (prevId != null) {
            loadSequence(prevId);
        }
    }

    /**
     * Load the next sequence, or create a new one if at the last sequence
     */
    private void loadNextSequence() {
        Long nextId = manager.getNextSequenceId(sequencer.getDrumSequenceId());

        if (nextId != null) {
            loadSequence(nextId);
        } else {
            // We're at the last saved sequence, so create a new blank one
            sequencer.setDrumSequenceId(0); // Set to 0 to indicate new unsaved sequence

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

            updateSequenceIdDisplay();
            CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_CREATED,
                this,
                sequencer.getDrumSequenceId()
            );
        }
    }

    /**
     * Load the last available sequence
     */
    private void loadLastSequence() {
        Long lastId = manager.getLastSequenceId();
        if (lastId != null) {
            loadSequence(lastId);
        }
    }

    /**
     * Save the current sequence
     */
    private void saveCurrentSequence() {
        // Save the sequence
        manager.saveSequence(sequencer);

        // After saving, get the latest sequence IDs from the database
        manager.refreshSequenceList();

        // Update display and button states
        updateSequenceIdDisplay();

        // Force update of button states
        updateButtonStates();

        CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_SAVED,
                this,
                sequencer.getDrumSequenceId()
        );

        logger.info("Saved drum sequence: {}", sequencer.getDrumSequenceId());
    }
}