package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.DrumStepParametersEvent;
import com.angrysurfer.core.event.NoteEvent;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * A sequencer panel with X0X-style step sequencing capabilities
 */
@Getter
@Setter
public class DrumParamsSequencerPanel extends PolyPanel implements IBusListener {

    // UI Components
    final static List<Dial> velocityDials = new ArrayList<>();
    final static List<Dial> decayDials = new ArrayList<>();
    final static List<Dial> probabilityDials = new ArrayList<>();
    final static List<Dial> nudgeDials = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(DrumParamsSequencerPanel.class.getName());


    /**
     * Create a new SequencerPanel
     *
     * @param noteEventConsumer Callback for when a note should be played
     */
    public DrumParamsSequencerPanel(Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());
        setNoteEventConsumer(noteEventConsumer);
    }

    /**
     * Create a column for the sequencer (modified to remove drum buttons)
     */
    JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        // REDUCED: from 5,2,5,2 to 2,1,2,1
        column.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));

        JPanel accentPanel = createAccentPanel(index);
        column.add(accentPanel);

        for (int i = 0; i < 4; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            labelPanel.add(label);
            column.add(labelPanel);

            // Create dial with appropriate settings based on type
            Dial dial = null;

            // Configure dial based on its type
            switch (i) {
                case 0: // Velocity
                    dial = createDial("velocity", index, 0, 127, 100);
                    velocityDials.add(dial);
                    dial.addChangeListener(e -> {
                        if (!isUpdatingControls() && getSelectedPadIndex() >= 0) {
                            int value = ((Dial) e.getSource()).getValue();
                            getSequencer().setStepVelocity(getSelectedPadIndex(), index, value);

                            // Publish event using sequencer-based constructor
                            CommandBus.getInstance().publish(
                                    Commands.DRUM_STEP_PARAMETERS_CHANGED,
                                    this,
                                    new DrumStepParametersEvent(getSequencer(), getSelectedPadIndex(), index)
                            );
                        }
                    });
                    break;

                case 1: // Decay
                    dial = createDial("decay", index, 0, 1000, 250);
                    decayDials.add(dial);
                    dial.addChangeListener(e -> {
                        if (!isUpdatingControls() && getSelectedPadIndex() >= 0) {
                            int value = ((Dial) e.getSource()).getValue();
                            getSequencer().setStepDecay(getSelectedPadIndex(), index, value);

                            // Publish event using sequencer-based constructor
                            CommandBus.getInstance().publish(
                                    Commands.DRUM_STEP_PARAMETERS_CHANGED,
                                    this,
                                    new DrumStepParametersEvent(getSequencer(), getSelectedPadIndex(), index)
                            );
                        }
                    });
                    break;


                case 2: // Probability
                    dial = createDial("probability", index, 0, 100, 100);
                    probabilityDials.add(dial);
                    dial.addChangeListener(e -> {
                        if (!isUpdatingControls() && getSelectedPadIndex() >= 0) {
                            int value = ((Dial) e.getSource()).getValue();
                            getSequencer().setStepProbability(getSelectedPadIndex(), index, value);

                            // Publish event using sequencer-based constructor
                            CommandBus.getInstance().publish(
                                    Commands.DRUM_STEP_PARAMETERS_CHANGED,
                                    this,
                                    new DrumStepParametersEvent(getSequencer(), getSelectedPadIndex(), index)
                            );
                        }
                    });
                    break;

                case 3: // Nudge
                    dial = createDial("nudge", index, -50, 50, 0);
                    nudgeDials.add(dial);
                    dial.addChangeListener(e -> {
                        if (!isUpdatingControls() && getSelectedPadIndex() >= 0) {
                            int value = ((Dial) e.getSource()).getValue();
                            getSequencer().setStepNudge(getSelectedPadIndex(), index, value);

                            // Publish event using sequencer-based constructor
                            CommandBus.getInstance().publish(
                                    Commands.DRUM_STEP_PARAMETERS_CHANGED,
                                    this,
                                    new DrumStepParametersEvent(getSequencer(), getSelectedPadIndex(), index)
                            );
                        }
                    });
                    break;
            }

            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }

        // REDUCED: from 0,5 to 0,2
        column.add(Box.createRigidArea(new Dimension(0, 2)));

        // Add only the trigger button - not the drum button
        JPanel buttonPanel = createTriggerPanel(index);
        column.add(buttonPanel);

        return column;
    }

    void updateControlsFromSequencer() {
        if (getSelectedPadIndex() < 0) {
            return;
        }

        // Update all dials to match the sequencer's current values for the selected
        // drum
        for (int i = 0; i < getSelectorButtons().size(); i++) {
            // Update velocity dials
            if (i < velocityDials.size()) {
                Dial dial = velocityDials.get(i);
                int velocity = getSequencer().getStepVelocity(getSelectedPadIndex(), i);
                dial.setValue(velocity);
                dial.repaint();
            }

            // Update decay dials
            if (i < decayDials.size()) {
                Dial dial = decayDials.get(i);
                dial.setValue(getSequencer().getStepDecay(getSelectedPadIndex(), i));
                dial.repaint();
            }

            // Update probability dials
            if (i < probabilityDials.size()) {
                Dial dial = probabilityDials.get(i);
                dial.setValue(getSequencer().getStepProbability(getSelectedPadIndex(), i));
                dial.repaint();
            }

            // Update nudge dials
            if (i < nudgeDials.size()) {
                Dial dial = nudgeDials.get(i);
                dial.setValue(getSequencer().getStepNudge(getSelectedPadIndex(), i));
                dial.repaint();
            }
        }
    }

    @Override
    public void onAction(Command action) {
        super.onAction(action);

        // Add specific handling for direct refresh
        if (action.getCommand() != null &&
                action.getCommand().equals(Commands.DRUM_GRID_REFRESH_REQUESTED)) {

            if (action.getData() instanceof Integer drumIndex) {
                if (drumIndex == getSelectedPadIndex()) {
                    SwingUtilities.invokeLater(this::updateControlsFromSequencer);
                }
            }
        }
    }

}
