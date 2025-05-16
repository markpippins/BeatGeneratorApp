package com.angrysurfer.beats.panel.sequencer.mono;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.event.MelodicSequencerEvent;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.Direction;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.service.MelodicSequencerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel providing navigation controls for melodic sequences
 */
public class MelodicSequenceNavigationPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceNavigationPanel.class);
    private final MelodicSequencer sequencer;

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
        // Change to use BoxLayout instead of FlowLayout to match SoundParametersPanel
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // Match the border style to SoundParametersPanel - use compound border
        UIHelper.setWidgetPanelBorder(this, "Sequence");
        // Create ID label with adjusted sizing
        sequenceIdLabel = new JLabel(getFormattedIdText(), SwingConstants.CENTER);
        sequenceIdLabel.setPreferredSize(new Dimension(UIHelper.ID_LABEL_WIDTH - 5, UIHelper.CONTROL_HEIGHT - 2));
        sequenceIdLabel.setOpaque(true);
        sequenceIdLabel.setBackground(UIHelper.darkGray);
        sequenceIdLabel.setForeground(UIHelper.coolBlue);
        sequenceIdLabel.setFont(sequenceIdLabel.getFont().deriveFont(12f));

        // Add horizontal strut to match spacing in SoundParametersPanel
        add(Box.createHorizontalStrut(2));
        add(sequenceIdLabel);
        add(Box.createHorizontalStrut(4));

        // Create navigation buttons with consistent styling
        // Add new button like in DrumSequenceNavigationPanel
        JButton newButton = createButton("➕", "Create new sequence", e -> createNewSequence());
        firstButton = createButton("⏮", "First sequence", e -> loadFirstSequence());
        prevButton = createButton("◀", "Previous sequence", e -> loadPreviousSequence());
        nextButton = createButton("▶", "Next sequence", e -> loadNextSequence());
        lastButton = createButton("⏭", "Last sequence", e -> loadLastSequence());
        JButton saveButton = createButton("💾", "Save current sequence", e -> saveCurrentSequence());

        // Add components with consistent spacing
        add(newButton);
        add(Box.createHorizontalStrut(4));
        add(firstButton);
        add(Box.createHorizontalStrut(2));
        add(prevButton);
        add(Box.createHorizontalStrut(2));
        add(nextButton);
        add(Box.createHorizontalStrut(2));
        add(lastButton);
        add(Box.createHorizontalStrut(4));
        add(saveButton);

        // Add flexible space at the end
        add(Box.createHorizontalGlue());

        // Set initial button state
        updateButtonStates();
    }

    private JButton createButton(String text, String tooltip, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
        button.setFocusable(false);

        // Match the sizing of buttons in SoundParametersPanel
        button.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        button.setMaximumSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        button.setMargin(new Insets(1, 1, 1, 1));

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
                (sequencer.getSequenceData().getId() == 0 ? "New" : sequencer.getSequenceData().getId());
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

        // First/Previous buttons - enabled if we're not at the first sequence
        boolean isFirst = !hasSequences || (firstId != null && currentId <= firstId);

        // Last button - only enabled if we're not at the last sequence
        boolean isLast = !hasSequences || (lastId != null && currentId >= lastId);

        // Enable/disable buttons
        firstButton.setEnabled(hasSequences && !isFirst);
        prevButton.setEnabled(hasSequences && !isFirst && currentId > 0);
        nextButton.setEnabled(currentId > 0); // Next enabled if we have a saved sequence
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
        } catch (Exception e) {
            logger.error("Error creating new melodic sequence", e);
        }
    }

    /**
     * Load a sequence with the given ID
     */
    private void loadSequence(Long sequenceId) {
        if (sequenceId != null && sequencer.getId() != null) {
            RedisService.getInstance().applyMelodicSequenceToSequencer(
                    RedisService.getInstance().findMelodicSequenceById(sequenceId, sequencer.getId()),
                    sequencer);

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
        if (sequencer.getId() == null) {
            logger.warn("Cannot save sequence - sequencer has no ID");
            return;
        }

        // Save the sequence
        MelodicSequencerManager.getInstance().saveSequence(sequencer);

        // Update display and button states
        updateSequenceIdDisplay();

        // Publish event
        CommandBus.getInstance().publish(
                Commands.MELODIC_SEQUENCE_SAVED,
                this,
                new MelodicSequencerEvent(
                        sequencer.getId(),
                        sequencer.getSequenceData().getId()));

        logger.info("Saved melodic sequence: {} for sequencer {}",
                sequencer.getSequenceData().getId(), sequencer.getId());
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