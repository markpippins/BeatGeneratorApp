package com.angrysurfer.beats.panel.sequencer.mono;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.CircleOfFifthsDial;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.MelodicSequencerEvent;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grid panel for melodic sequencer with step controls
 */
public class MelodicSequencerGridPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerGridPanel.class);
    // Reference to sequencer
    private final MelodicSequencer sequencer;
    // UI state variables
    private final List<TriggerButton> triggerButtons = new ArrayList<>();
    private final List<Dial> noteDials = new ArrayList<>();
    private final List<CircleOfFifthsDial> circleOfFifthsDials = new ArrayList<>();
    private final Map<Integer, JPanel> noteDialPanels = new HashMap<>();
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> gateDials = new ArrayList<>();
    private final List<Dial> probabilityDials = new ArrayList<>();
    private final List<Dial> nudgeDials = new ArrayList<>();
    // Flag to prevent recursive updates
    private boolean listenersEnabled = true;

    /**
     * Create a new melodic sequencer grid panel
     *
     * @param sequencer The melodic sequencer to control
     */
    public MelodicSequencerGridPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        initialize();
        registerForEvents();
        forceSync();  // Add this line to sync on panel creation
    }

    private static Color getHighlightColor(int newStep) {
        Color highlightColor;

        if (newStep < 16) {
            // First 16 steps - orange highlight
            highlightColor = UIHelper.fadedOrange;
        } else if (newStep < 32) {
            // Steps 17-32
            highlightColor = UIHelper.coolBlue;
        } else if (newStep < 48) {
            // Steps 33-48
            highlightColor = UIHelper.deepNavy;
        } else {
            // Steps 49-64
            highlightColor = UIHelper.mutedOlive;
        }
        return highlightColor;
    }

    /**
     * Initialize the grid panel
     */
    private void initialize() {
        // Use BorderLayout for the main panel to allow proper expansion
        setLayout(new BorderLayout(2, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setMinimumSize(new Dimension(800, 700));

        // Create panel for the 16 columns
        JPanel sequencePanel = new JPanel();
        sequencePanel.setLayout(new GridLayout(1, 16, 2, 0)); // Use GridLayout for equal width columns

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            sequencePanel.add(columnPanel);
        }

        // Add the sequence panel to the main panel
        add(sequencePanel, BorderLayout.CENTER);

        // Add component listener to handle resizing
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // updateDialSizes();
            }
        });
    }

    /**
     * Create a column for one step in the sequence
     */
    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));

        // Create standard dials first (velocity, gate, probability, nudge)
        for (int i = 0; i < 4; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            // Make label more compact with smaller padding
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            // Set consistent font size
            label.setFont(label.getFont().deriveFont(11f));

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            labelPanel.add(label);
            column.add(labelPanel);

            // Create standard dials
            Dial dial = new Dial();
            dial.setSequencerId(sequencer.getId());

            // Store the dial in the appropriate collection based on its type
            switch (i) {
                case 0 -> {
                    velocityDials.add(dial);
                    dial.setKnobColor(UIHelper.getDialColor("velocity"));
                }
                case 1 -> {
                    gateDials.add(dial);
                    dial.setKnobColor(UIHelper.getDialColor("gate"));
                }
                case 2 -> {
                    dial.setMinimum(0);
                    dial.setMaximum(100);
                    dial.setValue(100); // Default to 100%
                    dial.setKnobColor(UIHelper.getDialColor("probability"));
                    probabilityDials.add(dial);
                }
                case 3 -> {
                    dial.setMinimum(0);
                    dial.setMaximum(250);
                    dial.setValue(0); // Default to no nudge
                    dial.setKnobColor(UIHelper.getDialColor("nudge"));
                    nudgeDials.add(dial);
                }
            }

            dial.setUpdateOnResize(false); // Enable auto-updating on resize
            dial.setToolTipText(String.format("Step %d %s", index + 1, getKnobLabel(i)));
            dial.setName("JDial-" + index + "-" + i);
            dial.setMaximumSize(new Dimension(50, 50));
            // Center the dial horizontally with minimal padding
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }

        // Create both dial types for note selection
        NoteSelectionDial noteDial = new NoteSelectionDial();
        CircleOfFifthsDial circleDial = new CircleOfFifthsDial();

        // Configure both dials
        noteDial.setSequencerId(sequencer.getId());
        circleDial.setSequencerId(sequencer.getId());

        // Set initial range and value for both
        noteDial.setMinimum(0);
        noteDial.setMaximum(127);
        noteDial.setValue(60);
        noteDial.setKnobColor(UIHelper.getDialColor("note"));

        circleDial.setMinimum(0);
        circleDial.setMaximum(127);
        circleDial.setValue(60);
        circleDial.setKnobColor(UIHelper.getDialColor("note").darker());

        // Add to respective collections
        noteDials.add(noteDial);
        circleOfFifthsDials.add(circleDial);

        // Create panels for each dial type
        JPanel noteDialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        noteDialPanel.add(noteDial);
        noteDialPanel.setName("NoteDialPanel-" + index);

        JPanel circleDialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        circleDialPanel.add(circleDial);
        circleDialPanel.setName("CircleDialPanel-" + index);

        // Initially show the note dial (assume not following)
        column.add(noteDialPanel);

        // Store reference to the panel containing the note dial for this step
        noteDialPanels.put(index, noteDialPanel);

        // Set the dial's tool tips
        noteDial.setToolTipText(String.format("Step %d Note", index + 1));
        circleDial.setToolTipText(String.format("Step %d Note (Following)", index + 1));

        noteDial.setName("NoteDial-" + index);
        circleDial.setName("CircleDial-" + index);

        // Add change listener to circle dial too
        circleDial.addChangeListener(e -> {
            if (!listenersEnabled) return;
            updateSequencerData(index);
        });

        // Add listeners after all dials are created and added to collections
        addDialListeners(index);

        // Add trigger button
        column.add(Box.createRigidArea(new Dimension(0, 2)));
        TriggerButton triggerButton = createTriggerButton(index);
        triggerButtons.add(triggerButton);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.add(triggerButton);
        buttonPanel.setName("ButtonPanel-" + index);
        column.add(buttonPanel);

        return column;
    }

    /**
     * Update the note dials display based on follow state
     */
    private void updateNoteDialsDisplay(Integer followId) {
        // Flag to prevent event recursion
        listenersEnabled = false;

        try {
            boolean isFollowing = followId != null && followId > -1;
            logger.info("Updating note dials display - following: {}", isFollowing ? "Melo " + (followId + 1) : "None");

            // Update each column
            for (int i = 0; i < Math.min(noteDials.size(), circleOfFifthsDials.size()); i++) {
                JPanel columnPanel = (JPanel) noteDialPanels.get(i).getParent();

                // Find and manage trigger button visibility
                for (Component component : columnPanel.getComponents()) {
                    if (component instanceof JPanel && component.getName() != null &&
                            component.getName().equals("ButtonPanel-" + i)) {
                        // Hide trigger buttons when showing circle of fifths (following)
                        component.setVisible(!isFollowing);
                        break;
                    }
                }

                // Remove the current note dial panel
                Component[] components = columnPanel.getComponents();
                for (Component component : components) {
                    if (component instanceof JPanel &&
                            (component.getName() != null &&
                                    (component.getName().startsWith("NoteDialPanel") ||
                                            component.getName().startsWith("CircleDialPanel")))) {
                        columnPanel.remove(component);
                        break;
                    }
                }

                // Create the appropriate dial panel based on follow state
                JPanel newDialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                if (isFollowing) {
                    // When following, show the circle of fifths dial
                    CircleOfFifthsDial circleDial = circleOfFifthsDials.get(i);

                    // Make it larger when shown (since trigger buttons are hidden)
                    // circleDial.setPreferredSize(new Dimension(60, 60));

                    // Ensure the circle dial has the same value as the note dial
                    circleDial.setValue(noteDials.get(i).getValue(), false);

                    newDialPanel.add(circleDial);
                    newDialPanel.setName("CircleDialPanel-" + i);
                } else {
                    // When not following, show the regular note dial
                    newDialPanel.add(noteDials.get(i));
                    newDialPanel.setName("NoteDialPanel-" + i);
                }

                // Add the new panel at the end of the column
                columnPanel.add(newDialPanel);

                // Store the new panel reference
                noteDialPanels.put(i, newDialPanel);
            }

            // Force UI update
            revalidate();
            repaint();
        } finally {
            listenersEnabled = true;
        }
    }

    private TriggerButton createTriggerButton(int index) {
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));
        triggerButton.setPreferredSize(new Dimension(20, 20)); // Smaller size
        triggerButton.setToggleable(true);

        triggerButton.addActionListener(e -> {
            if (!listenersEnabled) return;

            boolean isSelected = triggerButton.isSelected();
            // Get existing step data
            int note = noteDials.get(index).getValue();
            int velocity = velocityDials.get(index).getValue();
            int gate = gateDials.get(index).getValue();
            int probability = probabilityDials.get(index).getValue();
            int nudge = nudgeDials.get(index).getValue();
            // Update sequencer pattern data
            sequencer.setStepData(index, isSelected, note, velocity, gate, probability, nudge);
        });
        return triggerButton;
    }

    /**
     * Add listeners to dials at the specified index
     */
    private void addDialListeners(int index) {
        // Note dial listener
        noteDials.get(index).addChangeListener(e -> {
            if (!listenersEnabled) return;
            updateSequencerData(index);
        });

        // Velocity dial listener
        velocityDials.get(index).addChangeListener(e -> {
            if (!listenersEnabled) return;
            updateSequencerData(index);
        });

        // Gate dial listener
        gateDials.get(index).addChangeListener(e -> {
            if (!listenersEnabled) return;
            updateSequencerData(index);
        });

        // Probability dial listener
        probabilityDials.get(index).addChangeListener(e -> {
            if (!listenersEnabled) return;
            updateSequencerData(index);
        });

        // Nudge dial listener
        nudgeDials.get(index).addChangeListener(e -> {
            if (!listenersEnabled) return;
            updateSequencerData(index);
        });
    }

    /**
     * Update sequencer data with values from all dials at the specified index
     */
    private void updateSequencerData(int index) {
        if (index < 0 || index >= triggerButtons.size()) return;

        // Get the note value from either dial type
        int noteValue;
        Integer followId = sequencer.getSequenceData().getFollowSequencerId();
        boolean isFollowing = followId != null && followId > -1;

        if (isFollowing) {
            noteValue = circleOfFifthsDials.get(index).getValue();
        } else {
            noteValue = noteDials.get(index).getValue();
        }

        sequencer.setStepData(index,
                triggerButtons.get(index).isSelected(),
                noteValue,
                velocityDials.get(index).getValue(),
                gateDials.get(index).getValue(),
                probabilityDials.get(index).getValue(),
                nudgeDials.get(index).getValue());
    }


    /**
     * Overridden to update dial sizes when first displayed
     */
    @Override
    public void addNotify() {
        super.addNotify();
        // Schedule dial size update after component is fully displayed
        //SwingUtilities.invokeLater(this::updateDialSizes);
    }

    /**
     * Get the label for a knob based on its type
     */
    private String getKnobLabel(int i) {
        return i == 0 ? "Velocity" : i == 1 ? "Gate" : i == 2 ? "Probability" : i == 3 ? "Nudge" : "Note";
    }

    /**
     * Update step highlighting based on sequence position
     */
    public void updateStepHighlighting(int oldStep, int newStep) {
        // Un-highlight old step
        if (oldStep >= 0 && oldStep < triggerButtons.size()) {
            triggerButtons.get(oldStep).setHighlighted(false);
            triggerButtons.get(oldStep).repaint();
        }

        // Highlight new step with color based on position
        if (newStep >= 0 && newStep < triggerButtons.size()) {
            Color highlightColor = getHighlightColor(newStep);

            triggerButtons.get(newStep).setHighlighted(true);
            triggerButtons.get(newStep).setHighlightColor(highlightColor);
            triggerButtons.get(newStep).repaint();
        }
    }

    /**
     * Synchronize all UI elements with the current sequencer state
     */
    public void syncWithSequencer() {
        if (sequencer == null) {
            logger.error("Cannot sync with null sequencer");
            return;
        }

        listenersEnabled = false;
        try {
            forceSync();
            // Make sure dial sizes are appropriate
            // updateDialSizes();
        } finally {
            listenersEnabled = true;
        }
    }

    /**
     * Force initialization and synchronization with sequencer data
     */
    public void forceSync() {
        try {
            if (sequencer == null) {
                logger.error("Cannot sync with null sequencer");
                return;
            }

            listenersEnabled = false;

            // Check follow state and update display accordingly
            Integer followId = sequencer.getSequenceData().getFollowSequencerId();
            updateNoteDialsDisplay(followId);

            // Log what we're syncing
            logger.info("Force syncing grid panel with sequencer - activeSteps:{} steps",
                    sequencer.getSequenceData().getActiveSteps() != null ?
                            sequencer.getSequenceData().getActiveSteps().size() : 0);

            // Update trigger buttons
            List<Boolean> activeSteps = sequencer.getSequenceData().getActiveSteps();

            // Get all other step parameters from sequencer
            List<Integer> notes = sequencer.getSequenceData().getNoteValues();
            List<Integer> velocities = sequencer.getSequenceData().getVelocityValues();
            List<Integer> gates = sequencer.getSequenceData().getGateValues();
            List<Integer> probabilities = sequencer.getSequenceData().getProbabilityValues();
            List<Integer> nudges = sequencer.getSequenceData().getNudgeValues();

            for (int i = 0; i < Math.min(triggerButtons.size(), activeSteps.size()); i++) {
                // Update trigger button state
                boolean active = activeSteps.get(i);
                triggerButtons.get(i).setSelected(active);

                // Update all dial values - make sure indexes are valid first
                if (i < notes.size() && i < noteDials.size()) {
                    noteDials.get(i).setValue(notes.get(i));
                }

                if (i < velocities.size() && i < velocityDials.size()) {
                    velocityDials.get(i).setValue(velocities.get(i));
                }

                if (i < gates.size() && i < gateDials.size()) {
                    gateDials.get(i).setValue(gates.get(i));
                }

                if (i < probabilities.size() && i < probabilityDials.size()) {
                    probabilityDials.get(i).setValue(probabilities.get(i));
                }

                if (i < nudges.size() && i < nudgeDials.size()) {
                    nudgeDials.get(i).setValue(nudges.get(i));
                }

                // Force immediate visual update for trigger button
                triggerButtons.get(i).repaint();
            }

            // Force immediate update for all components
            revalidate();
            repaint();
        } finally {
            listenersEnabled = true;
        }
    }

    /**
     * Register for command bus events
     */
    private void registerForEvents() {
        // Register for specific events only
        CommandBus.getInstance().register(this, new String[]{
                Commands.PATTERN_UPDATED,
                Commands.MELODIC_SEQUENCE_UPDATED,
                Commands.MELODIC_SEQUENCE_CREATED,
                Commands.MELODIC_SEQUENCE_LOADED,
                Commands.SCALE_SELECTED,
                Commands.HIGHLIGHT_SCALE_NOTE,
                Commands.WINDOW_RESIZED,
                Commands.SEQUENCER_FOLLOW_EVENT  // Add this event
        });

        logger.debug("MelodicSequencerGridPanel registered for specific events");
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) return;

        // Make sure we're only processing events for our sequencer
        if (action.getData() instanceof MelodicSequencerEvent event) {
            if (event.getSequencerId() != null && !event.getSequencerId().equals(sequencer.getId())) {
                return; // Not for our sequencer
            }
        }

        switch (action.getCommand()) {
            case Commands.PATTERN_UPDATED:
            case Commands.MELODIC_PATTERN_UPDATED:
            case Commands.MELODIC_SEQUENCE_UPDATED:
            case Commands.MELODIC_SEQUENCE_LOADED:
            case Commands.MELODIC_SEQUENCE_CREATED:
                // Sync UI with sequencer data
                SwingUtilities.invokeLater(this::syncWithSequencer);
                logger.debug("Grid panel updated due to pattern/sequence change");
                break;

            case Commands.SEQUENCER_FOLLOW_EVENT:
                // Check if this event is for our sequencer
                if (action.getSender() == sequencer ||
                        (action.getSender() instanceof MelodicSequencer ms && ms.getId().equals(sequencer.getId()))) {
                    // Update the note dials display based on follow state
                    Integer followId = null;
                    if (action.getData() instanceof Integer) {
                        followId = (Integer) action.getData();
                    }

                    // Update the UI on the EDT
                    final Integer finalFollowId = followId;
                    SwingUtilities.invokeLater(() -> updateNoteDialsDisplay(finalFollowId));
                    logger.debug("Updating follow state to: {}", followId);
                }
                break;

            case Commands.SCALE_SELECTED:
                // If scale changes, we should update note dial displays
                if (action.getSender() != this) {
                    SwingUtilities.invokeLater(this::updateNoteDialsForScale);
                }
                break;

            case Commands.HIGHLIGHT_SCALE_NOTE:
                // Handle note highlighting for scale visualization
                if (action.getData() instanceof Integer noteValue) {
                    // Implementation for highlighting
                }
                break;

            case Commands.WINDOW_RESIZED:
                // Update dial sizes when window is resized
                break;
        }
    }

    /**
     * Update note dials to reflect the current scale
     */
    private void updateNoteDialsForScale() {
        if (sequencer.getSequenceData().isQuantizeEnabled()) {
            for (Dial dial : noteDials) {
                if (dial instanceof NoteSelectionDial noteSelectionDial) {
                    // Ensure notes reflect current scale setting
                    noteSelectionDial.repaint();
                }
            }
        }
    }
}
