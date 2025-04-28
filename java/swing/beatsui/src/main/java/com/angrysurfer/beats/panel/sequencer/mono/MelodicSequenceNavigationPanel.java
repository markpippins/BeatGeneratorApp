package com.angrysurfer.beats.panel.sequencer.mono;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.redis.MelodicSequencerHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.service.MelodicSequencerManager;

/**
 * Panel providing navigation controls for melodic sequences
 */
public class MelodicSequenceNavigationPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceNavigationPanel.class);
    
    private static final int LABEL_WIDTH = 85;

    private JLabel sequenceIdLabel;
    private JButton firstButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton lastButton;
    private JButton saveButton;
    private JButton newButton; // Add new button like in DrumSequenceNavigationPanel

    private final MelodicSequencer sequencer;
    private final RedisService redisService;
    private final MelodicSequencerManager manager;
    private MelodicSequencerPanel parentPanel;

    // Update the constructor to accept the parent panel reference
    public MelodicSequenceNavigationPanel(MelodicSequencer sequencer, MelodicSequencerPanel parentPanel) {
        this.sequencer = sequencer;
        this.parentPanel = parentPanel;  // Store the reference
        
        // Rest of constructor remains the same
        this.redisService = RedisService.getInstance();
        this.manager = MelodicSequencerManager.getInstance();
        
        initializeUI();
    }

    private void initializeUI() {
        // Set layout and border with more compact spacing
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.getColors()[MelodicSequencerManager.getInstance().getSequencerCount()]),
                "Sequence",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        // Create ID label
        sequenceIdLabel = new JLabel(getFormattedIdText(), SwingConstants.CENTER);
        sequenceIdLabel.setPreferredSize(new Dimension(LABEL_WIDTH, UIUtils.CONTROL_HEIGHT));
        sequenceIdLabel.setOpaque(true);
        sequenceIdLabel.setBackground(UIUtils.darkGray);
        sequenceIdLabel.setForeground(UIUtils.coolBlue);
        sequenceIdLabel.setFont(sequenceIdLabel.getFont().deriveFont(12f));

        // Create new sequence button with plus icon
        newButton = createButton("➕", "Create new sequence", e -> createNewSequence());

        // Create navigation buttons with icons instead of text
        firstButton = createButton("⏮", "First sequence", e -> loadFirstSequence());
        prevButton = createButton("◀", "Previous sequence", e -> loadPreviousSequence());
        nextButton = createButton("▶", "Next sequence", e -> loadNextSequence());
        lastButton = createButton("⏭", "Last sequence", e -> loadLastSequence());
        
        // Create save button with icon
        saveButton = createButton("💾", "Save current sequence", e -> saveCurrentSequence());

        // Add components to panel
        add(sequenceIdLabel);
        add(newButton);      // Add new button like in DrumSequenceNavigationPanel
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
        
        // Set consistent size and margins to match other panels
        button.setPreferredSize(new Dimension(UIUtils.SMALL_CONTROL_WIDTH, UIUtils.CONTROL_HEIGHT));
        button.setMargin(new Insets(2, 2, 2, 2));
        
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
        return "Seq: " + 
            (sequencer.getMelodicSequenceId() == 0 ? "New" : sequencer.getMelodicSequenceId());
    }

    /**
     * Enable/disable buttons based on current sequence position
     */
    private void updateButtonStates() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot update button states - sequencer has no ID");
            return;
        }
        
        long currentId = sequencer.getMelodicSequenceId();
        boolean hasSequences = manager.hasSequences(sequencer.getId());
        
        // Get first/last sequence IDs
        Long firstId = manager.getFirstSequenceId(sequencer.getId());
        Long lastId = manager.getLastSequenceId(sequencer.getId());
        
        // First/Previous buttons - enabled if we're not at the first sequence
        boolean isFirst = !hasSequences || (firstId != null && currentId <= firstId);
        
        // Last button - only enabled if we're not at the last sequence
        boolean isLast = !hasSequences || (lastId != null && currentId >= lastId);
        
        // Enable/disable buttons
        firstButton.setEnabled(hasSequences && !isFirst);
        prevButton.setEnabled(hasSequences && !isFirst && currentId > 0);
        nextButton.setEnabled(currentId > 0);  // Next enabled if we have a saved sequence
        lastButton.setEnabled(hasSequences && !isLast);
        
        logger.debug("Button states: currentId={}, firstId={}, lastId={}, isFirst={}, isLast={}",
                  currentId, firstId, lastId, isFirst, isLast);
    }

    /**
     * Create a new sequence and apply it to the sequencer
     */
    private void createNewSequence() {
        try {
            // Verify sequencer has an ID
            if (sequencer.getId() == null) {
                logger.error("Cannot create new sequence - sequencer has no ID");
                return;
            }
            
            // Create a new sequence with an assigned ID right away
            MelodicSequencerHelper.MelodicSequencerEvent event = new MelodicSequencerHelper.MelodicSequencerEvent(
                sequencer.getId(), 0L); // Use 0 to indicate new sequence
            
            // Reset the sequencer and clear pattern
            sequencer.setMelodicSequenceId(0L); // Set to 0 to indicate new unsaved sequence
            sequencer.reset();
            sequencer.clearPattern();

            // Set default parameters
            sequencer.setPatternLength(16);
            sequencer.setDirection(Direction.FORWARD);
            sequencer.setTimingDivision(TimingDivision.NORMAL);
            sequencer.setLooping(true);
            
            // Update UI
            updateSequenceIdDisplay();
            
            // Notify listeners
            CommandBus.getInstance().publish(
                Commands.MELODIC_SEQUENCE_UPDATED,
                this,
                event
            );
            
            logger.info("Created new blank melodic sequence for sequencer {}", sequencer.getId());
        } catch (Exception e) {
            logger.error("Error creating new melodic sequence", e);
        }
    }

    /**
     * Load a sequence with the given ID
     */
    private void loadSequence(Long sequenceId) {
        if (sequenceId != null && sequencer.getId() != null) {
            redisService.applyMelodicSequenceToSequencer(
                redisService.findMelodicSequenceById(sequenceId, sequencer.getId()),
                sequencer
            );
            
            // Update display
            updateSequenceIdDisplay();
            
            // Reset the sequencer to ensure proper step indicator state
            sequencer.reset();
            
            // Notify that a pattern was loaded
            CommandBus.getInstance().publish(
                Commands.MELODIC_SEQUENCE_LOADED,
                this,
                new MelodicSequencerHelper.MelodicSequencerEvent(
                    sequencer.getId(), 
                    sequencer.getMelodicSequenceId()
                )
            );
            
            logger.info("Loaded melodic sequence {} for sequencer {}", sequenceId, sequencer.getId());
        }
    }
    
    private void loadFirstSequence() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot load first sequence - sequencer has no ID");
            return;
        }
        
        Long firstId = manager.getFirstSequenceId(sequencer.getId());
        if (firstId != null) {
            loadSequence(firstId);
        }
    }

    private void loadPreviousSequence() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot load previous sequence - sequencer has no ID");
            return;
        }
        
        Long prevId = manager.getPreviousSequenceId(
            sequencer.getId(), 
            sequencer.getMelodicSequenceId()
        );
        
        if (prevId != null) {
            loadSequence(prevId);
        }
    }

    private void loadNextSequence() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot load next sequence - sequencer has no ID");
            return;
        }
        
        Long nextId = manager.getNextSequenceId(
            sequencer.getId(), 
            sequencer.getMelodicSequenceId()
        );

        if (nextId != null) {
            loadSequence(nextId);
        }
    }

    private void loadLastSequence() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot load last sequence - sequencer has no ID");
            return;
        }
        
        Long lastId = manager.getLastSequenceId(sequencer.getId());
        if (lastId != null) {
            loadSequence(lastId);
        }
    }

    private void saveCurrentSequence() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot save sequence - sequencer has no ID");
            return;
        }
        
        // Save the sequence
        manager.saveSequence(sequencer);
        
        // Update display and button states
        updateSequenceIdDisplay();
        
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

    // Modify the navigateToSequence method to use parentPanel instead of sequencerPanel
    private void navigateToSequence(Long sequenceId) {
        // ...existing code to load the sequence...
        
        // After loading the sequence, explicitly update the tilt panel
        if (parentPanel != null && parentPanel.getTiltSequencerPanel() != null) {
            logger.info("Explicitly updating tilt panel after navigation");
            parentPanel.getTiltSequencerPanel().syncWithSequencer();
        }
    }
}