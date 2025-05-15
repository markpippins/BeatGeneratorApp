package com.angrysurfer.beats.panel.sequencer.mono;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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

        for (int i = 0; i < 5; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            // Make label more compact with smaller padding
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            // Set consistent font size
            label.setFont(label.getFont().deriveFont(11f));

            if (i < 4) {
                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                labelPanel.add(label);
                column.add(labelPanel);
            }

            // Create dial - first one is always a NoteSelectionDial
            Dial dial = i == 4 ? new NoteSelectionDial() : new Dial();

            // Set default sizes - we'll update these in updateDialSizes()
            dial.setPreferredSize(new Dimension(50, 50));

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
                case 4 -> noteDials.add(dial);
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

            dial.setUpdateOnResize(true); // Enable auto-updating on resize
            dial.setToolTipText(String.format("Step %d %s", index + 1, getKnobLabel(i)));
            dial.setName("JDial-" + index + "-" + i);

            // Center the dial horizontally with minimal padding
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }

        // Add listeners after all dials are created and added to collections
        addDialListeners(index);

        column.add(Box.createRigidArea(new Dimension(0, 2)));

        // Make trigger button more compact
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

        triggerButtons.add(triggerButton);
        // Compact panel for trigger button
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton);
        column.add(buttonPanel1);

        return column;
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

        sequencer.setStepData(index,
                triggerButtons.get(index).isSelected(),
                noteDials.get(index).getValue(),
                velocityDials.get(index).getValue(),
                gateDials.get(index).getValue(),
                probabilityDials.get(index).getValue(),
                nudgeDials.get(index).getValue());
    }

    /**
     * Update dial sizes based on container width
     */
    private void updateDialSizes() {
        // Get available width per column (16 columns total)
        int totalWidth = getWidth();
        if (totalWidth <= 0) return; // Skip if not yet displayed

        // Account for borders and padding
        int availableWidth = totalWidth - 40; // Subtract border/padding (adjust as needed)
        int columnWidth = Math.max(40, availableWidth / 16); // Ensure minimum width

        // Calculate dial sizes based on column width
        int standardDialSize = Math.max(30, (int) (columnWidth * 0.8)); // 80% of column width
        int noteDialSize = Math.max(40, (int) (columnWidth * 0.95)); // 95% of column width

        logger.debug("Resizing dials - column width: {}, standard size: {}, note size: {}",
                columnWidth, standardDialSize, noteDialSize);

        // Update all dials
        for (int i = 0; i < noteDials.size(); i++) {
            // Update standard dials
            if (i < velocityDials.size()) {
                velocityDials.get(i).setPreferredSize(new Dimension(standardDialSize, standardDialSize));
            }
            if (i < gateDials.size()) {
                gateDials.get(i).setPreferredSize(new Dimension(standardDialSize, standardDialSize));
            }
            if (i < probabilityDials.size()) {
                probabilityDials.get(i).setPreferredSize(new Dimension(standardDialSize, standardDialSize));
            }
            if (i < nudgeDials.size()) {
                nudgeDials.get(i).setPreferredSize(new Dimension(standardDialSize, standardDialSize));
            }

            // Update note dials (larger)
            if (i < noteDials.size()) {
                noteDials.get(i).setPreferredSize(new Dimension(noteDialSize, noteDialSize));
            }
        }

        // Force immediate update
        revalidate();
        repaint();
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

            // Log what we're syncing
            logger.info("Force syncing grid panel with sequencer - activeSteps:{} steps",
                    sequencer.getSequenceData().getActiveSteps() != null ? sequencer.getSequenceData().getActiveSteps().size() : 0);

            // Update trigger buttons
            List<Boolean> activeSteps = sequencer.getSequenceData().getActiveSteps();
            for (int i = 0; i < Math.min(triggerButtons.size(), activeSteps.size()); i++) {
                boolean active = activeSteps.get(i);
                triggerButtons.get(i).setSelected(active);
                // Force immediate visual update
                triggerButtons.get(i).repaint();

                logger.debug("Step {} set to {}", i, active);
            }

            // Update other controls...

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
                Commands.WINDOW_RESIZED
        });

        logger.debug("MelodicSequencerGridPanel registered for specific events");
    }

    @Override
    public void onAction(Command action) {

    }
}
