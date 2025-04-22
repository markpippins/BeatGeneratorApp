package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.DrumSequencerButton;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Panel containing drum pad selectors
 */
public class DrumSelectorPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSelectorPanel.class);
    
    // Reference to the sequencer and parent panel
    private final DrumSequencer sequencer;
    private final DrumSequencerPanel parentPanel;
    
    // UI components
    private final List<DrumSequencerButton> drumButtons = new ArrayList<>();
    
    // Constants
    private static final int DRUM_PAD_COUNT = DrumSequencer.DRUM_PAD_COUNT;
    private static final int MIDI_DRUM_NOTE_OFFSET = DrumSequencer.MIDI_DRUM_NOTE_OFFSET;
    
    /**
     * Creates a new DrumSelectorPanel
     * @param sequencer The drum sequencer
     * @param parentPanel The parent panel for callbacks
     */
    public DrumSelectorPanel(DrumSequencer sequencer, DrumSequencerPanel parentPanel) {
        this.sequencer = sequencer;
        this.parentPanel = parentPanel;
        
        // Use GridLayout for perfect vertical alignment with grid cells
        setLayout(new GridLayout(DRUM_PAD_COUNT, 1, 2, 2));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
        
        initializeButtons();
    }
    
    /**
     * Initialize all drum selector buttons
     */
    private void initializeButtons() {
        // Create drum buttons for standard drum kit sounds
        String[] drumNames = {
                "Kick", "Snare", "Closed HH", "Open HH",
                "Tom 1", "Tom 2", "Tom 3", "Crash",
                "Ride", "Rim", "Clap", "Cow",
                "Clave", "Shaker", "Perc 1", "Perc 2"
        };

        // Default MIDI notes for General MIDI drums
        int[] defaultNotes = {
                MIDI_DRUM_NOTE_OFFSET, MIDI_DRUM_NOTE_OFFSET + 2, MIDI_DRUM_NOTE_OFFSET + 6, MIDI_DRUM_NOTE_OFFSET + 10,
                MIDI_DRUM_NOTE_OFFSET + 5, MIDI_DRUM_NOTE_OFFSET + 7, MIDI_DRUM_NOTE_OFFSET + 9,
                MIDI_DRUM_NOTE_OFFSET + 13,
                MIDI_DRUM_NOTE_OFFSET + 15, MIDI_DRUM_NOTE_OFFSET + 1, MIDI_DRUM_NOTE_OFFSET + 3,
                MIDI_DRUM_NOTE_OFFSET + 20,
                MIDI_DRUM_NOTE_OFFSET + 39, MIDI_DRUM_NOTE_OFFSET + 34, MIDI_DRUM_NOTE_OFFSET + 24,
                MIDI_DRUM_NOTE_OFFSET + 25
        };

        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            final int drumIndex = i;

            // Create a Strike object for this drum pad
            Strike strike = new Strike();
            strike.setName(drumNames[i]);
            strike.setRootNote(defaultNotes[i]);
            strike.setLevel(100); // Default velocity

            // Set the strike in the sequencer
            sequencer.setPlayer(drumIndex, strike);

            // Create the drum button with proper selection handling
            DrumSequencerButton drumButton = new DrumSequencerButton(drumIndex, sequencer);
            drumButton.setText(drumNames[i]);
            drumButton.setToolTipText("Select " + drumNames[i] + " (Note: " + defaultNotes[i] + ")");

            // THIS IS THE KEY PART - Add action listener for drum selection
            drumButton.addActionListener(e -> parentPanel.selectDrumPad(drumIndex));

            // Add to our tracking list
            drumButtons.add(drumButton);

            // Add to the panel
            add(drumButton);
        }
    }
    
    /**
     * Get the list of drum buttons
     */
    public List<DrumSequencerButton> getDrumButtons() {
        return drumButtons;
    }
    
    /**
     * Update the appearance of all drum buttons
     */
    public void updateButtonSelection(int selectedIndex) {
        for (int i = 0; i < drumButtons.size(); i++) {
            drumButtons.get(i).setSelected(i == selectedIndex);
        }
    }
}