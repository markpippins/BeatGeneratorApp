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
import com.angrysurfer.core.redis.MelodicSequencerHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;

/**
 * Panel providing navigation controls for melodic sequences
 */
public class MelodicSequenceNavigationPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceNavigationPanel.class);

    private JLabel sequenceIdLabel;
    private JButton firstButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton lastButton;
    private JButton saveButton;

    private final MelodicSequencer sequencer;
    private final RedisService redisService;

    public MelodicSequenceNavigationPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        this.redisService = RedisService.getInstance();

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
        sequenceIdLabel.setPreferredSize(new Dimension(120, 25));

        // Create navigation buttons
        firstButton = createButton("|<", "First sequence", e -> loadFirstSequence());
        prevButton = createButton("<", "Previous sequence", e -> loadPreviousSequence());
        nextButton = createButton(">", "Next sequence", e -> loadNextSequence());
        lastButton = createButton(">|", "Last sequence", e -> loadLastSequence());

        // Create save button
        saveButton = createButton("Save", "Save current sequence", e -> saveCurrentSequence());
        saveButton.setBackground(new java.awt.Color(220, 240, 255));

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
        button.setFocusable(false);
        return button;
    }

    /**
     * Update the ID display with current sequence ID
     */
    public void updateSequenceIdDisplay() {
        sequenceIdLabel.setText(getFormattedIdText());
    }

    private String getFormattedIdText() {
        return "Sequence ID: " + 
            (sequencer.getMelodicSequenceId() == 0 ? "New" : sequencer.getMelodicSequenceId());
    }

    /**
     * Enable/disable buttons based on current sequence position
     */
    private void updateButtonStates() {
        long currentId = sequencer.getMelodicSequenceId();
        boolean hasSequences = redisService.getAllMelodicSequenceIds(sequencer.getId()).size() > 0;
        
        // Get first/last sequence IDs
        Long firstId = redisService.getMinimumMelodicSequenceId(sequencer.getId());
        Long lastId = redisService.getMaximumMelodicSequenceId(sequencer.getId());
        
        // First/Previous buttons - enabled if we're not at the first sequence
        boolean isFirst = !hasSequences || (firstId != null && currentId <= firstId);
        
        // Next button is ALWAYS enabled
        // This allows creating new sequences after the last one
        
        // Last button - only enabled if we're not at the last sequence
        boolean isLast = !hasSequences || (lastId != null && currentId >= lastId);
        
        // Enable/disable buttons
        firstButton.setEnabled(hasSequences && !isFirst);
        prevButton.setEnabled(hasSequences && (!isFirst || currentId == 0));
        nextButton.setEnabled(true);  // Always enable next
        lastButton.setEnabled(hasSequences && !isLast);
        
        logger.debug("Button states: currentId={}, firstId={}, lastId={}, isFirst={}, isLast={}",
                  currentId, firstId, lastId, isFirst, isLast);
    }

    /**
     * Load the sequence with the given ID
     */
    private void loadSequence(Long sequenceId) {
        if (sequenceId != null) {
            redisService.applyMelodicSequenceToSequencer(
                redisService.findMelodicSequenceById(sequenceId, sequencer.getId()),
                sequencer
            );
            
            // Update display
            updateSequenceIdDisplay();
            
            // Reset the sequencer to ensure proper step indicator state
            sequencer.reset();
            
            // Update button states
            updateButtonStates();
            
            // Notify that a pattern was loaded
            CommandBus.getInstance().publish(
                Commands.MELODIC_SEQUENCE_LOADED,
                this,
                new MelodicSequencerHelper.MelodicSequencerEvent(
                    sequencer.getId(), 
                    sequencer.getMelodicSequenceId()
                )
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
        Long prevId = redisService.getPreviousMelodicSequenceId(
            sequencer.getId(), 
            sequencer.getMelodicSequenceId()
        );
        
        if (prevId != null) {
            loadSequence(prevId);
        }
    }

    /**
     * Load the next sequence, or create a new one if at the last sequence
     */
    private void loadNextSequence() {
        Long nextId = redisService.getNextMelodicSequenceId(
            sequencer.getId(), 
            sequencer.getMelodicSequenceId()
        );

        if (nextId != null) {
            loadSequence(nextId);
        } else if (sequencer.getMelodicSequenceId() != 0) {
            // We're at the last saved sequence, so create a new blank one
            sequencer.setMelodicSequenceId(0L); // Set to 0 to indicate new unsaved sequence

            // Reset the sequencer and clear pattern
            sequencer.reset();
            sequencer.clearPattern();

            // Set default parameters
            sequencer.setPatternLength(16);
            sequencer.setDirection(Direction.FORWARD);
            sequencer.setTimingDivision(TimingDivision.NORMAL);
            sequencer.setLooping(true);
            
            // Update UI
            updateSequenceIdDisplay();
            updateButtonStates();
            
            // Notify listeners
            CommandBus.getInstance().publish(
                Commands.MELODIC_SEQUENCE_UPDATED,
                this,
                new MelodicSequencerHelper.MelodicSequencerEvent(
                    sequencer.getId(), 
                    sequencer.getMelodicSequenceId()
                )
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
        // Save the sequence
        redisService.saveMelodicSequence(sequencer);
        
        // Update display and button states
        updateSequenceIdDisplay();
        updateButtonStates();
        
        // Publish event
        CommandBus.getInstance().publish(
            Commands.MELODIC_SEQUENCE_SAVED,
            this,
            new MelodicSequencerHelper.MelodicSequencerEvent(
                sequencer.getId(), 
                sequencer.getMelodicSequenceId()
            )
        );
        
        logger.info("Saved melodic sequence: {} for sequencer {}", 
                   sequencer.getMelodicSequenceId(), sequencer.getId());
    }
}