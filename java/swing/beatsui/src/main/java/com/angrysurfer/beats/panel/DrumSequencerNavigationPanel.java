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
 * Completely revised navigation panel implementation
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
        super(new FlowLayout(FlowLayout.CENTER, 2, 0));
        
        this.sequencer = sequencer;
        this.manager = DrumSequencerManager.getInstance();
        
        setBorder(BorderFactory.createTitledBorder("Sequence"));
        
        initializeUI();
        CommandBus.getInstance().register(this);
    }

    private void initializeUI() {
        // First sequence button
        firstButton = new JButton("⏮");
        firstButton.setToolTipText("First Sequence");
        firstButton.setFocusable(false);
        firstButton.addActionListener(e -> loadFirstSequence());

        // Previous sequence button
        prevButton = new JButton("◀");
        prevButton.setToolTipText("Previous Sequence");
        prevButton.setFocusable(false);
        prevButton.addActionListener(e -> loadPreviousSequence());

        // Sequence ID display
        sequenceIdLabel = new JLabel("New");
        sequenceIdLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        // Next sequence button
        nextButton = new JButton("▶");
        nextButton.setToolTipText("Next Sequence");
        nextButton.setFocusable(false);
        nextButton.addActionListener(e -> loadNextSequence());

        // Last sequence button
        lastButton = new JButton("⏭");
        lastButton.setToolTipText("Last Sequence");
        lastButton.setFocusable(false);
        lastButton.addActionListener(e -> loadLastSequence());

        // Save button
        saveButton = new JButton("💾");
        saveButton.setToolTipText("Save Sequence");
        saveButton.setFocusable(false);
        saveButton.addActionListener(e -> saveCurrentSequence());

        // Add components to panel
        add(firstButton);
        add(prevButton);
        add(sequenceIdLabel);
        add(nextButton);
        add(lastButton);
        add(saveButton);
        
        // Initialize label & buttons
        updateSequenceIdLabel();
        updateButtonState();
    }
    
    // Update ID label based on current sequence
    private void updateSequenceIdLabel() {
        long id = sequencer.getDrumSequenceId();
        if (id <= 0) {
            sequenceIdLabel.setText("New");
        } else {
            sequenceIdLabel.setText("Seq " + id);
        }
        logger.info("Updated sequence label: {}", sequenceIdLabel.getText());
    }
    
    // Update button states based on current position
    private void updateButtonState() {
        Long currentId = sequencer.getDrumSequenceId();
        Long firstId = manager.getFirstSequenceId();
        Long lastId = manager.getLastSequenceId();
        
        // Special case for "New" sequences
        if (currentId <= 0) {
            firstButton.setEnabled(firstId != null);
            prevButton.setEnabled(firstId != null);
            nextButton.setEnabled(true);  // Can always save & create new
            lastButton.setEnabled(lastId != null);
        } else {
            Long prevId = manager.getPreviousSequenceId(currentId);
            Long nextId = manager.getNextSequenceId(currentId);
            
            firstButton.setEnabled(prevId != null);
            prevButton.setEnabled(prevId != null);
            nextButton.setEnabled(true);  // Can always move to new sequence
            lastButton.setEnabled(nextId != null);
        }
        
        logger.info("Button states - first:{}, prev:{}, next:{}, last:{}", 
            firstButton.isEnabled(), prevButton.isEnabled(), 
            nextButton.isEnabled(), lastButton.isEnabled());
    }

    /**
     * DIRECT implementation to absolutely guarantee sequence loading works
     */
    private void loadSequence(Long id) {
        if (id == null) return;
        
        logger.warn("LOADING SEQUENCE: {}", id);
        
        try {
            // 1. Apply sequence data to sequencer through manager
            manager.loadSequence(id);
            
            // 2. DIRECTLY set ID in sequencer to be 100% certain
            sequencer.setDrumSequenceId(id);
            
            // 3. Reset sequencer position
            sequencer.reset();
            
            // 4. FORCE update UI immediately
            updateSequenceIdLabel();
            updateButtonState();
            
            // 5. Notify others about the change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_LOADED, this, id);
            
            logger.warn("SEQUENCE LOADED AND UI UPDATED: {}", id);
        } catch (Exception e) {
            logger.error("Failed to load sequence {}: {}", id, e.getMessage(), e);
        }
    }

    private void loadFirstSequence() {
        Long firstId = manager.getFirstSequenceId();
        logger.warn("Loading first sequence: {}", firstId);
        if (firstId != null) {
            loadSequence(firstId);
        }
    }

    private void loadPreviousSequence() {
        Long currentId = sequencer.getDrumSequenceId();
        Long prevId = manager.getPreviousSequenceId(currentId);
        logger.warn("Loading previous sequence: current={}, prev={}", currentId, prevId);
        if (prevId != null) {
            loadSequence(prevId);
        }
    }

    private void loadNextSequence() {
        Long currentId = sequencer.getDrumSequenceId();
        Long nextId = manager.getNextSequenceId(currentId);
        logger.warn("Loading next sequence: current={}, next={}", currentId, nextId);
        
        if (nextId != null) {
            loadSequence(nextId);
        } else {
            createNewSequence();
        }
    }

    private void loadLastSequence() {
        Long lastId = manager.getLastSequenceId();
        logger.warn("Loading last sequence: {}", lastId);
        if (lastId != null) {
            loadSequence(lastId);
        }
    }

    private void createNewSequence() {
        logger.warn("Creating new sequence");
        
        // 1. Reset the sequencer state
        sequencer.reset();
        
        // 2. Set ID to 0 to indicate "new unsaved sequence"
        sequencer.setDrumSequenceId(0);
        
        // 3. Clear all patterns and set defaults
        for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
            // Clear any active steps
            for (int step = 0; step < 64; step++) {
                if (sequencer.isStepActive(i, step)) {
                    sequencer.toggleStep(i, step);
                }
            }
            
            // Reset to defaults
            sequencer.setPatternLength(i, 16);
            sequencer.setDirection(i, Direction.FORWARD);
            sequencer.setTimingDivision(i, TimingDivision.NORMAL);
            sequencer.setLooping(i, true);
        }
        
        // 4. Update UI immediately
        updateSequenceIdLabel();
        updateButtonState();
        
        // 5. Notify listeners
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_CREATED, this, 0L);
        logger.warn("New sequence created and UI updated");
    }

    private void saveCurrentSequence() {
        logger.warn("Saving current sequence");
        
        // 1. Save using manager
        manager.saveSequence();
        
        // 2. Force refresh the list of sequences
        manager.refreshSequenceList();
        
        // 3. Update UI immediately 
        updateSequenceIdLabel();
        updateButtonState();
        
        // 4. Notify listeners
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_SAVED, this, sequencer.getDrumSequenceId());
        logger.warn("Sequence saved with ID: {}", sequencer.getDrumSequenceId());
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;
        
        switch (action.getCommand()) {
            case Commands.DRUM_SEQUENCE_LOADED, 
                 Commands.DRUM_SEQUENCE_SAVED, 
                 Commands.DRUM_SEQUENCE_CREATED,
                 Commands.DRUM_SEQUENCE_DELETED -> {
                // Only update if event came from elsewhere
                if (action.getSender() != this) {
                    SwingUtilities.invokeLater(() -> {
                        updateSequenceIdLabel();
                        updateButtonState();
                    });
                }
            }
        }
    }
}