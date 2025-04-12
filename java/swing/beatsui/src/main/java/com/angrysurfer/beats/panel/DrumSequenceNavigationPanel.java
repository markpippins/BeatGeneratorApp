package com.angrysurfer.beats.panel;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

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
public class DrumSequenceNavigationPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequenceNavigationPanel.class);

    private JLabel sequenceIdLabel;
    private JButton firstButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton lastButton;
    private JButton saveButton;

    private final DrumSequencer sequencer;
    private final DrumSequencerManager manager;

    public DrumSequenceNavigationPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        this.manager = DrumSequencerManager.getInstance();

        initializeUI();
    }

    private void initializeUI() {
        // Set layout and border
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 2));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Sequence Navigation",
                TitledBorder.CENTER,
                TitledBorder.TOP
        ));

        // Create ID label
        sequenceIdLabel = new JLabel(getFormattedIdText());
        sequenceIdLabel.setPreferredSize(new Dimension(40, 25));

        // Create navigation buttons
        firstButton = createButton("⏮", "First sequence", e -> loadFirstSequence());
        prevButton = createButton("◀", "Previous sequence", e -> loadPreviousSequence());
        nextButton = createButton("▶", "Next sequence", e -> loadNextSequence());
        lastButton = createButton("⏭", "Last sequence", e -> loadLastSequence());

        // Create save button - make it stand out more
        saveButton = createButton("💾", "Save current sequence", e -> saveCurrentSequence());
        
        // Add components to panel
        add(sequenceIdLabel);
        add(firstButton);
        add(prevButton);
        add(nextButton);
        add(lastButton);
        add(saveButton);

        // Set initial button state
        updateButtonStates();
    }

    private JButton createButton(String text, String tooltip, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
        button.setPreferredSize(new Dimension(32, 32));
        
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
        return "Seq: " + (sequencer.getDrumSequenceId() > 0 ? sequencer.getDrumSequenceId() : "New");
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
        prevButton.setEnabled((hasSequences || sequencer.getDrumSequenceId() < 0) && !isFirst);
        nextButton.setEnabled(sequencer.getDrumSequenceId() > 0);  // Always enable the next button
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
            if (sequencer.isPlaying())
                sequencer.reset(true);
            else
                sequencer.reset(false);
                
            // Update UI
            updateSequenceIdDisplay();

            // Use consistent command for sequence loading notifications
            CommandBus.getInstance().publish(
                    Commands.PATTERN_LOADED,
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
        } else if (sequencer.getDrumSequenceId() != 0) {
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
                    Commands.DRUM_SEQUENCE_UPDATED,
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
