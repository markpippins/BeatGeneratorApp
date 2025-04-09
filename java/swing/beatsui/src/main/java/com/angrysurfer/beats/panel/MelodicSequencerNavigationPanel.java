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
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.MelodicSequencer;

/**
 * Panel providing navigation controls for melodic sequences
 */
public class MelodicSequencerNavigationPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerNavigationPanel.class);

    private JLabel sequenceIdLabel;
    private JButton firstButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton lastButton;
    private JButton saveButton;

    private final MelodicSequencer sequencer;
    private final RedisService redisService;

    public MelodicSequencerNavigationPanel(MelodicSequencer sequencer) {
        super(new FlowLayout(FlowLayout.CENTER, 2, 0));
        this.sequencer = sequencer;
        this.redisService = RedisService.getInstance();
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
        Long id = sequencer.getMelodicSequenceId();
        return id == null || id <= 0 ? "New" : "Seq " + id;
    }

    /**
     * Enable/disable buttons based on current sequence position
     */
    private void updateButtonStates() {
        Long currentId = sequencer.getMelodicSequenceId();
        boolean hasCurrentId = currentId != null && currentId > 0;

        // Query the service for sequence availability
        Long minId = redisService.getMinimumMelodicSequenceId(sequencer.getId());
        Long maxId = redisService.getMaximumMelodicSequenceId(sequencer.getId());

        boolean hasPrevious = hasCurrentId && minId != null && currentId > minId;
        boolean hasNext = maxId != null && (!hasCurrentId || currentId < maxId);

        firstButton.setEnabled(hasPrevious);
        prevButton.setEnabled(hasPrevious);
        nextButton.setEnabled(hasNext);
        lastButton.setEnabled(hasNext);
    }

    /**
     * Load the sequence with the given ID
     */
    private void loadSequence(Long sequenceId) {
        if (sequenceId != null && sequenceId > 0) {
            redisService.findMelodicSequenceById(sequenceId, sequencer.getId());
            redisService.applyMelodicSequenceToSequencer(
                redisService.findMelodicSequenceById(sequenceId, sequencer.getId()),
                sequencer
            );
            updateSequenceIdDisplay();
            
            // Create sequencer event class
            class MelodicSequencerEvent {
                private final Integer sequencerId;
                private final Long sequenceId;
                
                public MelodicSequencerEvent(Integer sequencerId, Long sequenceId) {
                    this.sequencerId = sequencerId;
                    this.sequenceId = sequenceId;
                }
                
                public Integer getSequencerId() { return sequencerId; }
                public Long getSequenceId() { return sequenceId; }
            }
            
            // Notify UI of sequence change
            CommandBus.getInstance().publish(
                Commands.MELODIC_SEQUENCE_LOADED, 
                this, 
                new MelodicSequencerEvent(sequencer.getId(), sequenceId)
            );
        }
    }

    /**
     * Load the first available sequence
     */
    private void loadFirstSequence() {
        Long firstId = redisService.getMinimumMelodicSequenceId(sequencer.getId());
        if (firstId != null) {
            loadSequence(firstId);
        }
    }

    /**
     * Load the previous sequence
     */
    private void loadPreviousSequence() {
        Long currentId = sequencer.getMelodicSequenceId();
        if (currentId != null && currentId > 0) {
            Long prevId = redisService.getPreviousMelodicSequenceId(sequencer.getId(), currentId);
            if (prevId != null) {
                loadSequence(prevId);
            }
        }
    }

    /**
     * Load the next sequence, or create a new one if at the last sequence
     */
    private void loadNextSequence() {
        Long currentId = sequencer.getMelodicSequenceId();
        Long nextId = null;
        
        if (currentId != null && currentId > 0) {
            nextId = redisService.getNextMelodicSequenceId(sequencer.getId(), currentId);
        }
        
        if (nextId != null) {
            loadSequence(nextId);
        } else {
            // Create new sequence
            redisService.applyMelodicSequenceToSequencer(
                redisService.newMelodicSequence(sequencer.getId()),
                sequencer
            );
            updateSequenceIdDisplay();
            
            // Notify UI
            CommandBus.getInstance().publish(
                Commands.MELODIC_SEQUENCE_CREATED, 
                this, 
                sequencer.getMelodicSequenceId()
            );
        }
    }

    /**
     * Load the last available sequence
     */
    private void loadLastSequence() {
        Long lastId = redisService.getMaximumMelodicSequenceId(sequencer.getId());
        if (lastId != null) {
            loadSequence(lastId);
        }
    }

    /**
     * Save the current sequence
     */
    private void saveCurrentSequence() {
        redisService.saveMelodicSequence(sequencer);
        updateSequenceIdDisplay();
        
        // Create sequencer event class
        class MelodicSequencerEvent {
            private final Integer sequencerId;
            private final Long sequenceId;
            
            public MelodicSequencerEvent(Integer sequencerId, Long sequenceId) {
                this.sequencerId = sequencerId;
                this.sequenceId = sequenceId;
            }
            
            public Integer getSequencerId() { return sequencerId; }
            public Long getSequenceId() { return sequenceId; }
        }
        
        // Notify UI
        CommandBus.getInstance().publish(
            Commands.MELODIC_SEQUENCE_SAVED, 
            this, 
            new MelodicSequencerEvent(sequencer.getId(), sequencer.getMelodicSequenceId())
        );
    }
}