package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.sequencer.DrumSequencer;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A sequencer panel with X0X-style step sequencing capabilities for effects
 */
@Getter
@Setter
public class DrumEffectsSequencerPanel extends DrumSequencerPanel {

    // UI Components for effects-specific parameters
    private static final List<Dial> panDials = new ArrayList<>();
    private static final List<Dial> delayDials = new ArrayList<>();
    private static final List<Dial> chorusDials = new ArrayList<>();
    private static final List<Dial> reverbDials = new ArrayList<>();

    /**
     * Create a new SequencerPanel
     *
     * @param noteEventConsumer Callback for when a note should be played
     */
    public DrumEffectsSequencerPanel(Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());
        setNoteEventConsumer(noteEventConsumer);
    }

    /**
     * Create a column for the sequencer (modified to handle effects parameters)
     */
    @Override
    JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        // column.setBackground(UIHelper.coolBlue);

        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 1));

        JPanel accentPanel = createAccentPanel(index);
        column.add(accentPanel);
        // column.add(createOffsetPanel(index));

        for (int rowIndex = 0; rowIndex < 4; rowIndex++) {
            JLabel label = new JLabel(getKnobLabel(rowIndex));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            labelPanel.add(label);
            column.add(labelPanel);

            // Create dial with appropriate settings based on type
            Dial dial = switch (rowIndex) {
                case 0 -> createPanDial(index);
                case 1 -> createDelayDial(index);
                case 2 -> createChorusDial(index);
                case 3 -> createReverbDial(index);
                default -> createDial(rowIndex, 0, 127, 100);
            };

            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }

        column.add(Box.createRigidArea(new Dimension(0, 2)));

        column.add(createTriggerPanel(index));

        return column;
    }

    /**
     * Create the Pan dial for a column
     */
    private Dial createPanDial(int columnIndex) {
        Dial dial = super.createDial(columnIndex, 0, 127, 64, 0);
        dial.setKnobColor(UIHelper.getDialColor("pan"));

        panDials.add(dial);
        dial.addChangeListener(e -> {
            if (!isUpdatingControls() && getSelectedPadIndex() >= 0) {
                int value = ((Dial) e.getSource()).getValue();
                getSequencer().setStepPan(getSelectedPadIndex(), columnIndex, value);
                CommandBus.getInstance().publish(Commands.DRUM_STEP_EFFECTS_CHANGED, this,
                        createDrumStepEffectsEvent(getSequencer(), getSelectedPadIndex(), columnIndex));
            }
        });

        return dial;
    }

    /**
     * Create the Delay dial for a column
     */
    private Dial createDelayDial(int columnIndex) {
        Dial dial = super.createDial(columnIndex, 1, 200, 60, 1);
        dial.setKnobColor(UIHelper.getDialColor("delay"));

        delayDials.add(dial);
        dial.addChangeListener(e -> {
            if (!isUpdatingControls() && getSelectedPadIndex() >= 0) {
                int value = ((Dial) e.getSource()).getValue();
                getSequencer().setStepDecay(getSelectedPadIndex(), columnIndex, value);
                CommandBus.getInstance().publish(Commands.DRUM_STEP_EFFECTS_CHANGED, this,
                        createDrumStepEffectsEvent(getSequencer(), getSelectedPadIndex(), columnIndex));
            }
        });

        return dial;
    }

    /**
     * Create the Chorus dial for a column
     */
    private Dial createChorusDial(int columnIndex) {
        Dial dial = super.createDial(columnIndex, 0, 127, 0, 2);
        dial.setKnobColor(UIHelper.getDialColor("chorus"));

        chorusDials.add(dial);
        dial.addChangeListener(e -> {
            if (!isUpdatingControls() && getSelectedPadIndex() >= 0) {
                int value = ((Dial) e.getSource()).getValue();
                getSequencer().setStepChorus(getSelectedPadIndex(), columnIndex, value);
                CommandBus.getInstance().publish(Commands.DRUM_STEP_EFFECTS_CHANGED, this,
                        createDrumStepEffectsEvent(getSequencer(), getSelectedPadIndex(), columnIndex));
            }
        });

        return dial;
    }

    /**
     * Create the Reverb dial for a column
     */
    private Dial createReverbDial(int columnIndex) {
        Dial dial = super.createDial(columnIndex, 0, 127, 0, 3);
        dial.setKnobColor(UIHelper.getDialColor("reverb"));

        reverbDials.add(dial);
        dial.addChangeListener(e -> {
            if (!isUpdatingControls() && getSelectedPadIndex() >= 0) {
                int value = ((Dial) e.getSource()).getValue();
                getSequencer().setStepReverb(getSelectedPadIndex(), columnIndex, value);
                CommandBus.getInstance().publish(Commands.DRUM_STEP_EFFECTS_CHANGED, this,
                        createDrumStepEffectsEvent(getSequencer(), getSelectedPadIndex(), columnIndex));
            }
        });

        return dial;
    }

    /**
     * Create an Object array with effects parameters for event publishing
     */
    private Object[] createDrumStepEffectsEvent(DrumSequencer sequencer, int drumIndex, int stepIndex) {
        return new Object[]{
                drumIndex,
                stepIndex,
                sequencer.getStepPan(drumIndex, stepIndex),
                sequencer.getStepChorus(drumIndex, stepIndex),
                sequencer.getStepReverb(drumIndex, stepIndex)
        };
    }

    /**
     * Update control values from the sequencer
     */
    void updateControlsFromSequencer() {
        if (getSelectedPadIndex() < 0) {
            return;
        }

        // Update all dials to match the sequencer's current values for the selected drum
        for (int i = 0; i < getSelectorButtons().size(); i++) {
            // Update pan dials
            if (i < panDials.size()) {
                Dial dial = panDials.get(i);
                dial.setValue(getSequencer().getStepPan(getSelectedPadIndex(), i));
                dial.repaint();
            }

            // Update delay dials
            if (i < delayDials.size()) {
                Dial dial = delayDials.get(i);
                dial.setValue(getSequencer().getStepDecay(getSelectedPadIndex(), i));
                dial.repaint();
            }

            // Update chorus dials
            if (i < chorusDials.size()) {
                Dial dial = chorusDials.get(i);
                dial.setValue(getSequencer().getStepChorus(getSelectedPadIndex(), i));
                dial.repaint();
            }

            // Update reverb dials
            if (i < reverbDials.size()) {
                Dial dial = reverbDials.get(i);
                dial.setValue(getSequencer().getStepReverb(getSelectedPadIndex(), i));
                dial.repaint();
            }
        }
    }

    @Override
    public void onAction(Command action) {
        super.onAction(action);
    }

    /**
     * Get the knob label for a specific index
     */
    @Override
    public String getKnobLabel(int i) {
        return switch (i) {
            case 0 -> "Pan";
            case 1 -> "Decay";
            case 2 -> "Chorus";
            case 3 -> "Reverb";
            default -> "";
        };
    }
}
