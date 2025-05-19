package com.angrysurfer.beats.panel.sequencer.mono;

import com.angrysurfer.beats.panel.PlayerAwarePanel;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.event.MelodicSequencerEvent;
import com.angrysurfer.core.sequencer.Direction;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.service.MelodicSequencerManager;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * Panel providing navigation controls for melodic sequences
 */
@Getter
@Setter
public class MelodicSequenceNavigationPanel extends PlayerAwarePanel {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceNavigationPanel.class);
    private MelodicSequencer sequencer;

    private JLabel sequenceIdLabel;
    private JButton firstButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton lastButton;

    // Update the constructor to accept the parent panel reference
    public MelodicSequenceNavigationPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;

        initializeUI();
        // registerForEvents();
    }

    private void initializeUI() {
        // Change to use the same FlowLayout as DrumSequenceNavigationPanel
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));

        // Keep the border style with sequence position
        UIHelper.setWidgetPanelBorder(this, "Sequence");
        
        // Create ID label with identical styling as DrumSequenceNavigationPanel
        sequenceIdLabel = new JLabel(getFormattedIdText(), SwingConstants.CENTER);
        sequenceIdLabel.setPreferredSize(new Dimension(UIHelper.ID_LABEL_WIDTH - 5, UIHelper.CONTROL_HEIGHT - 2));
        sequenceIdLabel.setOpaque(true);
        sequenceIdLabel.setBackground(UIHelper.darkGray);
        sequenceIdLabel.setForeground(UIHelper.coolBlue);
        sequenceIdLabel.setFont(sequenceIdLabel.getFont().deriveFont(12f));

        // Create navigation buttons with consistent styling - exactly like DrumSequenceNavigationPanel
        JButton newButton = createButton("➕", "Create new sequence", e -> createNewSequence());
        firstButton = createButton("⏮", "First sequence", e -> loadFirstSequence());
        prevButton = createButton("◀", "Previous sequence", e -> loadPreviousSequence());
        nextButton = createButton("▶", "Next sequence", e -> loadNextSequence());
        lastButton = createButton("⏭", "Last sequence", e -> loadLastSequence());
        JButton saveButton = createButton("💾", "Save current sequence", e -> saveCurrentSequence());

        // Add components in same order as DrumSequenceNavigationPanel
        add(sequenceIdLabel);
        add(newButton);
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

        // Match DrumSequenceNavigationPanel button sizing exactly
        button.setPreferredSize(new Dimension(24, 24));
        button.setMargin(new Insets(2, 2, 2, 2));

        return button;
    }

    /**
     * Update the ID display with current sequence ID
     */
    public void updateSequenceIdDisplay() {
        sequenceIdLabel.setText(getFormattedIdText());
        updateButtonStates();
        updateBorderTitle();
    }

    private String getFormattedIdText() {
        return "Seq: "
                + (sequencer.getSequenceData().getId() == 0 ? "New" : sequencer.getSequenceData().getId());
    }

    /**
     * Enable/disable buttons based on current sequence position
     */
    private void updateButtonStates() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot update button states - sequencer has no ID");
            return;
        }

        long currentId = sequencer.getSequenceData().getId();
        boolean hasSequences = MelodicSequencerManager.getInstance().hasSequences(sequencer.getId());

        // Get first/last sequence IDs
        Long firstId = MelodicSequencerManager.getInstance().getFirstSequenceId(sequencer.getId());
        Long lastId = MelodicSequencerManager.getInstance().getLastSequenceId(sequencer.getId());

        // Directly check if previous/next sequence exists
        Long prevId = MelodicSequencerManager.getInstance().getPreviousSequenceId(
                sequencer.getId(), currentId);
        Long nextId = MelodicSequencerManager.getInstance().getNextSequenceId(
                sequencer.getId(), currentId);

        // Enable/disable buttons based on direct availability
        firstButton.setEnabled(hasSequences && prevId != null);
        prevButton.setEnabled(hasSequences && prevId != null);
        nextButton.setEnabled(hasSequences && nextId != null);
        lastButton.setEnabled(hasSequences && nextId != null);

        // Log detailed state for debugging
        logger.debug("Button states: currentId={}, firstId={}, lastId={}, prevId={}, nextId={}",
                currentId, firstId, lastId, prevId, nextId);
    }

    /**
     * Update the border to show current sequence position
     */
    private void updateBorderTitle() {
        if (sequencer == null || sequencer.getId() == null) {
            return;
        }
        
        // Get all sequence IDs for this sequencer
        List<Long> allIds = MelodicSequencerManager.getInstance()
                .getAllMelodicSequenceIds(sequencer.getId());
        
        if (allIds == null || allIds.isEmpty()) {
            UIHelper.setWidgetPanelBorder(this, "Sequence");
            return;
        }
        
        // Sort IDs for consistent ordering
        Collections.sort(allIds);
        
        // Get current sequence ID
        long currentId = sequencer.getSequenceData().getId();
        
        // Find index of current ID (0-based)
        int currentIndex = allIds.indexOf(currentId);
        
        // Format title with 1-based index for user-friendliness
        String title = String.format("Sequence %d of %d", 
                currentIndex + 1, allIds.size());
        
        // Update border
        UIHelper.setWidgetPanelBorder(this, title);
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
            MelodicSequencerEvent event = new MelodicSequencerEvent(
                    sequencer.getId(), 0L); // Use 0 to indicate new sequence

            // Reset the sequencer and clear pattern
            sequencer.setId(0); // Set to 0 to indicate new unsaved sequence
            sequencer.reset();
            sequencer.getSequenceData().clearPattern();

            // Set default parameters
            sequencer.getSequenceData().setPatternLength(16);
            sequencer.getSequenceData().setDirection(Direction.FORWARD);
            sequencer.getSequenceData().setTimingDivision(TimingDivision.NORMAL);
            sequencer.setLooping(true);

            // Update UI
            updateSequenceIdDisplay();

            // Notify listeners
            CommandBus.getInstance().publish(
                    Commands.MELODIC_SEQUENCE_UPDATED,
                    this,
                    event);

            logger.info("Created new blank melodic sequence for sequencer {}", sequencer.getId());
            
            // Update border title
            updateBorderTitle();
        } catch (Exception e) {
            logger.error("Error creating new melodic sequence", e);
        }
    }

    /**
     * Load a sequence with the given ID
     */
    private void loadSequence(Long sequenceId) {
        if (sequenceId != null && sequencer.getId() != null) {
            // Replace direct RedisService call with manager call
            boolean success = MelodicSequencerManager.getInstance()
                    .applySequenceById(sequencer.getId(), sequenceId);

            if (!success) {
                logger.warn("Failed to load sequence {} for sequencer {}",
                        sequenceId, sequencer.getId());
                return;
            }

            // Update display
            updateSequenceIdDisplay();

            // Reset the sequencer to ensure proper step indicator state
            sequencer.reset();

            // Notify that a pattern was loaded
            CommandBus.getInstance().publish(
                    Commands.MELODIC_SEQUENCE_LOADED,
                    this,
                    new MelodicSequencerEvent(
                            sequencer.getId(),
                            sequencer.getSequenceData().getId()));

            logger.info("Loaded melodic sequence {} for sequencer {}", sequenceId, sequencer.getId());
            
            // Update border title
            updateBorderTitle();
        }
    }

    private void loadFirstSequence() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot load first sequence - sequencer has no ID");
            return;
        }

        Long firstId = MelodicSequencerManager.getInstance().getFirstSequenceId(sequencer.getId());
        if (firstId != null) {
            loadSequence(firstId);
        } else {
            // No sequences available - create one
            createAndSaveNewSequence();
        }
    }

    private void loadPreviousSequence() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot load previous sequence - sequencer has no ID");
            return;
        }

        Long prevId = MelodicSequencerManager.getInstance().getPreviousSequenceId(
                sequencer.getId(),
                sequencer.getSequenceData().getId());

        if (prevId != null) {
            loadSequence(prevId);
        }
    }

    private void loadNextSequence() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot load next sequence - sequencer has no ID");
            return;
        }

        Long nextId = MelodicSequencerManager.getInstance().getNextSequenceId(
                sequencer.getId(),
                sequencer.getSequenceData().getId());

        if (nextId != null) {
            loadSequence(nextId);
        }
    }

    private void loadLastSequence() {
        if (sequencer.getId() == null) {
            logger.warn("Cannot load last sequence - sequencer has no ID");
            return;
        }

        Long lastId = MelodicSequencerManager.getInstance().getLastSequenceId(sequencer.getId());
        if (lastId != null) {
            loadSequence(lastId);
        }
    }

    private void saveCurrentSequence() {
        // Before saving, make sure any changes are applied to the sequencer's data
        MelodicSequencerManager.getInstance().saveSequence(sequencer);
        
        // Update border after saving
        updateBorderTitle();
    }

    // Add this new method to handle the case where no sequences exist
    private void createAndSaveNewSequence() {
        // Create a new sequence
        createNewSequence();

        // Save it immediately
        saveCurrentSequence();

        logger.info("Created and saved new default sequence for sequencer {}", sequencer.getId());
    }

    @Override
    public void handlePlayerActivated() {
        if (getTargetPlayer().isMelodicPlayer()) {
            setSequencer(((MelodicSequencer) getTargetPlayer().getOwner()));
            updateSequenceIdDisplay();
            updateBorderTitle();
        }
    }

    @Override
    public void handlePlayerUpdated() {
        if (getTargetPlayer().isMelodicPlayer()) {
            setSequencer(((MelodicSequencer) getTargetPlayer().getOwner()));
            updateSequenceIdDisplay();
            updateBorderTitle();
        }
    }

    /**
     * Register for command bus events
     */
//    private void registerForEvents() {
//        // Register only for sequence navigation related events
//        CommandBus.getInstance().register(this, new String[] {
//            Commands.MELODIC_SEQUENCE_LOADED,
//            Commands.MELODIC_SEQUENCE_CREATED,
//            Commands.MELODIC_SEQUENCE_DELETED,
//            Commands.MELODIC_SEQUENCE_SAVED
//        });
//
//        logger.debug("MelodicSequenceNavigationPanel registered for specific events");
//    }
}
