package com.angrysurfer.beats.widget.panel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.angrysurfer.beats.widget.Dial;

/**
 * Specialized panel for a single oscillator in the internal synthesizer
 */
public class InternalSynthOscillatorPanel extends JPanel {
    private final Synthesizer synthesizer;
    private final int midiChannel;
    private final int oscillatorIndex;
    private final int baseCCForOsc;
    
    // UI Components
    private JCheckBox enabledToggle;
    private JComboBox<String> waveformCombo;
    private JComboBox<String> octaveCombo;
    private Dial detuneDial;
    private Dial brightnessDialDial;
    private Dial volumeDial;
    
    /**
     * Create a panel for a single oscillator
     * 
     * @param synthesizer The MIDI synthesizer to control
     * @param midiChannel MIDI channel (0-15)
     * @param oscillatorIndex Index of this oscillator (0-2)
     */
    public InternalSynthOscillatorPanel(Synthesizer synthesizer, int midiChannel, int oscillatorIndex) {
        super(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        this.synthesizer = synthesizer;
        this.midiChannel = midiChannel;
        this.oscillatorIndex = oscillatorIndex;
        this.baseCCForOsc = oscillatorIndex * 20 + 20; // Space out CC numbers
        
        setupUI();
    }
    
    private void setupUI() {
        // Create panel with titled border
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Oscillator " + (oscillatorIndex + 1),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Dialog", Font.BOLD, 11)
        ));
        
        // Create the toggle switch for oscillator on/off
        JLabel toggleLabel = new JLabel("On/Off:");
        toggleLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        enabledToggle = new JCheckBox();
        enabledToggle.setName("osc" + oscillatorIndex + "Toggle");
        enabledToggle.setSelected(oscillatorIndex == 0); // First oscillator on by default
        
        // Waveform selector with inline label
        JLabel waveLabel = new JLabel("Waveform:");
        waveLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        waveformCombo = new JComboBox<>(
                new String[]{"Sine", "Square", "Saw", "Triangle", "Pulse"});
        waveformCombo.setName("waveformCombo" + oscillatorIndex);
        waveformCombo.setPreferredSize(new Dimension(80, 25));
        
        // Octave selector with inline label
        JLabel octaveLabel = new JLabel("Octave:");
        octaveLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        octaveCombo = new JComboBox<>(
                new String[]{"-2", "-1", "0", "+1", "+2"});
        octaveCombo.setName("octaveCombo" + oscillatorIndex);
        octaveCombo.setSelectedIndex(2); // Default to "0"
        octaveCombo.setPreferredSize(new Dimension(50, 25));

        // Create parameter dials with inline labels
        JLabel tuneLabel = new JLabel("Tune:");
        tuneLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        detuneDial = createCompactDial("", "Tuning", 64);
        detuneDial.setName("detuneDial" + oscillatorIndex);

        JLabel brightLabel = new JLabel("Bright:");
        brightLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        brightnessDialDial = createCompactDial("", "Brightness", 64);
        brightnessDialDial.setName("brightnessDial" + oscillatorIndex);

        JLabel volumeLabel = new JLabel("Volume:");
        volumeLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        volumeDial = createCompactDial("", "Volume", oscillatorIndex == 0 ? 100 : 0);
        volumeDial.setName("volumeDial" + oscillatorIndex);

        // Add all components to the oscillator panel in a single row
        add(toggleLabel);
        add(enabledToggle);
        add(Box.createHorizontalStrut(10));
        
        add(waveLabel);
        add(waveformCombo);
        add(Box.createHorizontalStrut(10));
        
        add(octaveLabel);
        add(octaveCombo);
        add(Box.createHorizontalStrut(10));
        
        add(tuneLabel);
        add(detuneDial);
        add(Box.createHorizontalStrut(10));
        
        add(brightLabel);
        add(brightnessDialDial);
        add(Box.createHorizontalStrut(10));
        
        add(volumeLabel);
        add(volumeDial);

        // Add event handlers
        setupEventHandlers();
    }
    
    private void setupEventHandlers() {
        waveformCombo.addActionListener(e -> {
            int waveformType = waveformCombo.getSelectedIndex();

            if (enabledToggle.isSelected()) {
                setControlChange(baseCCForOsc, waveformType * 25);
                System.out.println("Osc " + (oscillatorIndex + 1) + " waveform: " + 
                        waveformCombo.getSelectedItem() + " (CC" + baseCCForOsc + "=" + 
                        (waveformType * 25) + ")");

                // Force program change on main channel if this is oscillator 1
                if (oscillatorIndex == 0) {
                    // This will be handled by the main panel
                    firePropertyChange("oscillator1WaveformChanged", -1, waveformType);
                }
            }
        });

        octaveCombo.addActionListener(e -> {
            int octave = octaveCombo.getSelectedIndex() - 2; // -2 to +2 range
            int value = (octave + 2) * 25 + 14;

            if (enabledToggle.isSelected()) {
                setControlChange(baseCCForOsc + 1, value);
                System.out.println("Osc " + (oscillatorIndex + 1) + " octave: " + octave +
                        " (CC" + (baseCCForOsc + 1) + "=" + value + ")");
            }
        });

        detuneDial.addChangeListener(e -> {
            if (enabledToggle.isSelected()) {
                setControlChange(baseCCForOsc + 2, detuneDial.getValue());
                System.out.println("Osc " + (oscillatorIndex + 1) + " tune: " + detuneDial.getValue() +
                        " (CC" + (baseCCForOsc + 2) + ")");
            }
        });

        brightnessDialDial.addChangeListener(e -> {
            if (enabledToggle.isSelected()) {
                setControlChange(baseCCForOsc + 3, brightnessDialDial.getValue());
                System.out.println("Osc " + (oscillatorIndex + 1) + " brightness: " + 
                        brightnessDialDial.getValue() + " (CC" + (baseCCForOsc + 3) + ")");
            }
        });

        volumeDial.addChangeListener(e -> {
            if (enabledToggle.isSelected()) {
                setControlChange(baseCCForOsc + 4, volumeDial.getValue());
                System.out.println("Osc " + (oscillatorIndex + 1) + " volume: " + 
                        volumeDial.getValue() + " (CC" + (baseCCForOsc + 4) + ")");
            } else {
                // Force volume to zero if disabled
                setControlChange(baseCCForOsc + 4, 0);
            }
        });

        enabledToggle.addActionListener(e -> {
            boolean enabled = enabledToggle.isSelected();

            int volume = enabled ? volumeDial.getValue() : 0;
            setControlChange(baseCCForOsc + 4, volume);

            System.out.println("Osc " + (oscillatorIndex + 1) + " " + 
                    (enabled ? "enabled" : "disabled") + " (CC" + (baseCCForOsc + 4) + 
                    "=" + volume + ")");

            // If enabling, also re-send all other settings
            if (enabled) {
                int waveformType = waveformCombo.getSelectedIndex();
                setControlChange(baseCCForOsc, waveformType * 25);

                int octave = octaveCombo.getSelectedIndex() - 2;
                setControlChange(baseCCForOsc + 1, (octave + 2) * 25 + 14);

                setControlChange(baseCCForOsc + 2, detuneDial.getValue());
                setControlChange(baseCCForOsc + 3, brightnessDialDial.getValue());
            }
        });
    }
    
    /**
     * Reset all controls to default values
     */
    public void resetToDefaults() {
        // Set enabled state
        enabledToggle.setSelected(oscillatorIndex == 0);
        
        // Reset all controls
        waveformCombo.setSelectedIndex(0);
        octaveCombo.setSelectedIndex(2); // Default to "0"
        detuneDial.setValue(64);
        brightnessDialDial.setValue(64);
        
        // Set volume based on oscillator index
        if (oscillatorIndex == 0) {
            volumeDial.setValue(100);
        } else {
            volumeDial.setValue(0);
        }
        
        // Force control updates
        int waveformType = waveformCombo.getSelectedIndex();
        int octave = octaveCombo.getSelectedIndex() - 2;
        int volume = enabledToggle.isSelected() ? volumeDial.getValue() : 0;
        
        setControlChange(baseCCForOsc, waveformType * 25);
        setControlChange(baseCCForOsc + 1, (octave + 2) * 25 + 14);
        setControlChange(baseCCForOsc + 2, detuneDial.getValue());
        setControlChange(baseCCForOsc + 3, brightnessDialDial.getValue());
        setControlChange(baseCCForOsc + 4, volume);
    }
    
    /**
     * Update MIDI synthesizer with current state
     */
    public void updateSynthState() {
        boolean enabled = enabledToggle.isSelected();
        int volume = enabled ? volumeDial.getValue() : 0;
        
        // Only send updates if we're enabled
        if (enabled) {
            int waveformType = waveformCombo.getSelectedIndex();
            int octave = octaveCombo.getSelectedIndex() - 2;
            
            setControlChange(baseCCForOsc, waveformType * 25);
            setControlChange(baseCCForOsc + 1, (octave + 2) * 25 + 14);
            setControlChange(baseCCForOsc + 2, detuneDial.getValue());
            setControlChange(baseCCForOsc + 3, brightnessDialDial.getValue());
        }
        
        // Always update volume
        setControlChange(baseCCForOsc + 4, volume);
    }
    
    /**
     * Helper method to send a control change to the synth
     */
    private void setControlChange(int ccNumber, int value) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel channel = synthesizer.getChannels()[midiChannel];
                if (channel != null) {
                    channel.controlChange(ccNumber, value);
                }
            } catch (Exception e) {
                System.err.println("Error setting CC " + ccNumber + " on channel " + 
                        (midiChannel + 1) + ": " + e.getMessage());
            }
        }
    }
    
    // Getters for all controls
    public boolean isEnabled() {
        return enabledToggle.isSelected();
    }
    
    public int getWaveformType() {
        return waveformCombo.getSelectedIndex();
    }
    
    public int getOctave() {
        return octaveCombo.getSelectedIndex() - 2; // Convert to -2 to +2 range
    }
    
    public int getDetune() {
        return detuneDial.getValue();
    }
    
    public int getBrightness() {
        return brightnessDialDial.getValue();
    }
    
    public int getVolume() {
        return volumeDial.getValue();
    }
    
    private Dial createCompactDial(String label, String tooltip, int initialValue) {
        Dial dial = new Dial();
        dial.setLabel(label);
        dial.setToolTipText(tooltip);
        dial.setValue(initialValue);
        dial.setMaximumSize(new Dimension(40, 40));
        dial.setPreferredSize(new Dimension(40, 40));
        return dial;
    }
}