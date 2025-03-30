package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.service.InternalSynthManager;

/**
 * Panel for controlling a MIDI synthesizer with tabs for oscillator, envelope,
 * filter and modulation parameters.
 */
public class SynthControlPanel extends JPanel {
    
    private final Synthesizer synthesizer;
    private JComboBox<PresetItem> presetCombo;
    private final int midiChannel = 15; // Use channel 16 (index 15)

    /**
     * Create a new SynthControlPanel
     * 
     * @param synthesizer The MIDI synthesizer to control
     */
    public SynthControlPanel(Synthesizer synthesizer) {
        super(new BorderLayout(5, 5));
        this.synthesizer = synthesizer;
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        setupUI();
    }
    
    private void setupUI() {
        // Create preset selection at top
        JPanel presetPanel = createPresetPanel();
        add(presetPanel, BorderLayout.NORTH);
        
        // Main panel with tabs
        JTabbedPane paramTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        paramTabs.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        
        // Add tabs for different parameter groups
        paramTabs.addTab("Oscillator", createOscillatorPanel());
        paramTabs.addTab("Envelope", createEnvelopePanel());
        paramTabs.addTab("Filter", createFilterPanel());
        paramTabs.addTab("Modulation", createModulationPanel());
        
        // Add the tabs to the main panel
        add(paramTabs, BorderLayout.CENTER);
    }
    
    /**
     * Set a MIDI CC value on the synth
     * 
     * @param ccNumber The CC number to set
     * @param value The value to set (0-127)
     */
    private void setControlChange(int ccNumber, int value) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel channel = synthesizer.getChannels()[midiChannel];
                if (channel != null) {
                    channel.controlChange(ccNumber, value);
                }
            } catch (Exception e) {
                System.err.println("Error setting CC " + ccNumber + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Inner class to represent preset items in the combo box
     */
    private static class PresetItem {
        private final int number;
        private final String name;

        public PresetItem(int number, String name) {
            this.number = number;
            this.name = name;
        }

        public int getNumber() {
            return number;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    
    /**
     * Create a panel with a preset selector combo box
     */
    private JPanel createPresetPanel() {
        JPanel presetPanel = new JPanel(new BorderLayout(5, 5));
        presetPanel.setBorder(BorderFactory.createTitledBorder("Synth Presets"));

        // Create preset combo box
        presetCombo = new JComboBox<>();
        populatePresetCombo();

        // Add listener to change synth preset when selected
        presetCombo.addActionListener(e -> {
            if (presetCombo.getSelectedItem() instanceof PresetItem) {
                PresetItem item = (PresetItem) presetCombo.getSelectedItem();
                setProgramChange(item.getNumber());
                System.out.println("Selected preset: " + item.getName() + " (#" + item.getNumber() + ")");
            }
        });

        // Add a label and the combo box to the panel
        JPanel innerPanel = new JPanel(new BorderLayout(5, 0));
        innerPanel.add(new JLabel("Preset:"), BorderLayout.WEST);
        innerPanel.add(presetCombo, BorderLayout.CENTER);

        presetPanel.add(innerPanel, BorderLayout.NORTH);

        return presetPanel;
    }

    /**
     * Load presets for the current instrument into the combo box
     */
    private void populatePresetCombo() {
        presetCombo.removeAllItems();

        // Get preset names from InternalSynthManager - use GM synth ID (1)
        List<String> presetNames = InternalSynthManager.getInstance().getPresetNames(1L);

        // Always add all 128 GM presets
        for (int i = 0; i < 128; i++) {
            // Get name from the list if available, otherwise use generic name
            String presetName;
            if (i < presetNames.size() && presetNames.get(i) != null && !presetNames.get(i).isEmpty()) {
                presetName = presetNames.get(i);
            } else {
                presetName = "Program " + i;
            }

            // Add the preset to the combo box
            presetCombo.addItem(new PresetItem(i, i + ": " + presetName));
        }

        // Select the first preset by default
        if (presetCombo.getItemCount() > 0) {
            presetCombo.setSelectedIndex(0);
        }
    }
    
    /**
     * Send program change to the synthesizer
     */
    private void setProgramChange(int program) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel channel = synthesizer.getChannels()[midiChannel];
                
                if (channel != null) {
                    channel.programChange(program);
                    System.out.println("Changed synth program to " + program);
                    
                    // Reset and reinitialize controllers after program change
                    reinitializeControllers();
                }
            } catch (Exception e) {
                System.err.println("Error changing program: " + e.getMessage());
            }
        }
    }
    
    /**
     * Reset controllers and synchronize controls after preset change
     */
    private void reinitializeControllers() {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel channel = synthesizer.getChannels()[midiChannel];
                
                // First reset all controllers
                channel.resetAllControllers();
                
                // Wait a tiny bit
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {}
                
                // Set basic channel parameters
                channel.controlChange(7, 100); // Volume
                channel.controlChange(10, 64); // Pan center
                
                // Enable expression
                channel.controlChange(11, 127);
                
                System.out.println("Reinitialized controllers after preset change");
            } catch (Exception e) {
                System.err.println("Error reinitializing controllers: " + e.getMessage());
            }
        }
    }
    
    private JPanel createOscillatorPanel() {
        // Use a BorderLayout as the main container to expand to full width
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Inner panel with GridBagLayout for control placement
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; // Take up all horizontal space
        
        // Create control groups - now they'll stretch to fill available width
        JPanel waveGroup = createGroupPanel("Waveform");
        JPanel mixGroup = createGroupPanel("Mix");
        JPanel detuneGroup = createGroupPanel("Detune");
        JPanel widthGroup = createGroupPanel("Width");
        JPanel octaveGroup = createGroupPanel("Octave");
        
        // Create controls with smaller sizes
        JComboBox<String> waveformCombo = new JComboBox<>(
                new String[]{"Sine", "Square", "Saw", "Triangle", "Pulse"});
        
        // Create parameter dials - smaller and more consistent size
        Dial oscMixDial = createCompactDial("", "Oscillator Mix", 50);
        Dial detuneDial = createCompactDial("", "Detune Amount", 0);
        Dial pulseDial = createCompactDial("", "Pulse Width", 50);
        
        // Create octave selector
        JComboBox<String> octaveCombo = new JComboBox<>(new String[]{"-2", "-1", "0", "+1", "+2"});
        
        // Add waveform selector to its group - make it fill width
        waveformCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, waveformCombo.getPreferredSize().height));
        JPanel wavePanel = new JPanel(new BorderLayout());
        wavePanel.add(waveformCombo, BorderLayout.CENTER);
        waveGroup.add(wavePanel);
        
        // Add each dial to its own dedicated group
        addControlsToGroupPanel(mixGroup, oscMixDial);
        addControlsToGroupPanel(detuneGroup, detuneDial);
        addControlsToGroupPanel(widthGroup, pulseDial);
        
        // Add octave selector to its group
        JPanel octavePanel = new JPanel(new BorderLayout());
        octavePanel.add(octaveCombo, BorderLayout.CENTER);
        octaveGroup.add(octavePanel);
        
        // First row: Waveform selector
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(waveGroup, gbc);
        
        // Second row: Mix and Detune controls side by side
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(mixGroup, gbc);
        
        gbc.gridx = 1;
        panel.add(detuneGroup, gbc);
        
        // Third row: Width and Octave controls side by side
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(widthGroup, gbc);
        
        gbc.gridx = 1;
        panel.add(octaveGroup, gbc);
        
        // Add event handlers
        waveformCombo.addActionListener(e -> {
            int waveformType = waveformCombo.getSelectedIndex();
            setControlChange(70, waveformType * 25);
        });
        
        oscMixDial.addChangeListener(e -> setControlChange(7, oscMixDial.getValue()));
        detuneDial.addChangeListener(e -> setControlChange(94, detuneDial.getValue()));
        pulseDial.addChangeListener(e -> setControlChange(70, pulseDial.getValue()));
        
        octaveCombo.addActionListener(e -> {
            int octave = octaveCombo.getSelectedIndex() - 2; // -2 to +2 range
            setControlChange(18, (octave + 2) * 25); // Scale to 0-127 range
        });
        
        // Add the panel to the main container with some glue at bottom
        mainPanel.add(panel, BorderLayout.NORTH);
        mainPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private JPanel createEnvelopePanel() {
        // Use a BorderLayout as the main container to expand to full width
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Inner panel with GridBagLayout for control placement
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Create ADSR groups
        JPanel attackGroup = createGroupPanel("Attack");
        JPanel decayGroup = createGroupPanel("Decay");
        JPanel sustainGroup = createGroupPanel("Sustain");
        JPanel releaseGroup = createGroupPanel("Release");
        
        // Create parameter dials - smaller and more consistent size
        Dial attackDial = createCompactDial("", "Attack Time", 0);
        Dial decayDial = createCompactDial("", "Decay Time", 50);
        Dial sustainDial = createCompactDial("", "Sustain Level", 80);
        Dial releaseDial = createCompactDial("", "Release Time", 20);
        
        // Add dials to their groups
        addControlsToGroupPanel(attackGroup, attackDial);
        addControlsToGroupPanel(decayGroup, decayDial);
        addControlsToGroupPanel(sustainGroup, sustainDial);
        addControlsToGroupPanel(releaseGroup, releaseDial);
        
        // First row: Attack and Decay
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(attackGroup, gbc);
        
        gbc.gridx = 1;
        panel.add(decayGroup, gbc);
        
        // Second row: Sustain and Release
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(sustainGroup, gbc);
        
        gbc.gridx = 1;
        panel.add(releaseGroup, gbc);
        
        // Add control change listeners
        attackDial.addChangeListener(e -> setControlChange(73, attackDial.getValue()));
        decayDial.addChangeListener(e -> setControlChange(75, decayDial.getValue()));
        sustainDial.addChangeListener(e -> setControlChange(79, sustainDial.getValue()));
        releaseDial.addChangeListener(e -> setControlChange(72, releaseDial.getValue()));
        
        // Add the panel to the main container with some glue at bottom
        mainPanel.add(panel, BorderLayout.NORTH);
        mainPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private JPanel createFilterPanel() {
        // Use a BorderLayout as the main container to expand to full width
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Inner panel with GridBagLayout for control placement
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Create filter control groups
        JPanel typeGroup = createGroupPanel("Type");
        JPanel cutoffGroup = createGroupPanel("Cutoff");
        JPanel resonanceGroup = createGroupPanel("Resonance");
        JPanel envAmtGroup = createGroupPanel("Env Amount");
        
        // Create controls
        JComboBox<String> filterTypeCombo = new JComboBox<>(
                new String[]{"Low Pass", "High Pass", "Band Pass", "Notch"});
        Dial cutoffDial = createCompactDial("", "Filter Cutoff Frequency", 100);
        Dial resonanceDial = createCompactDial("", "Filter Resonance", 0);
        Dial envAmountDial = createCompactDial("", "Envelope Modulation Amount", 0);
        
        // Add controls to their groups
        JPanel typePanel = new JPanel(new BorderLayout());
        typePanel.add(filterTypeCombo, BorderLayout.CENTER);
        typeGroup.add(typePanel);
        
        addControlsToGroupPanel(cutoffGroup, cutoffDial);
        addControlsToGroupPanel(resonanceGroup, resonanceDial);
        addControlsToGroupPanel(envAmtGroup, envAmountDial);
        
        // First row: Type and Cutoff
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(typeGroup, gbc);
        
        // Second row: Cutoff and Resonance
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(cutoffGroup, gbc);
        
        gbc.gridx = 1;
        panel.add(resonanceGroup, gbc);
        
        // Third row: Env Amount
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(envAmtGroup, gbc);
        
        // Add control change listeners
        filterTypeCombo.addActionListener(e -> {
            int filterType = filterTypeCombo.getSelectedIndex();
            setControlChange(102, filterType * 32); // Custom CC for filter type
        });
        
        cutoffDial.addChangeListener(e -> setControlChange(74, cutoffDial.getValue()));
        resonanceDial.addChangeListener(e -> setControlChange(71, resonanceDial.getValue()));
        envAmountDial.addChangeListener(e -> setControlChange(110, envAmountDial.getValue()));
        
        // Add the panel to the main container with some glue at bottom
        mainPanel.add(panel, BorderLayout.NORTH);
        mainPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private JPanel createModulationPanel() {
        // Use a BorderLayout as the main container to expand to full width
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Inner panel with GridBagLayout for control placement
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Create LFO control groups
        JPanel waveGroup = createGroupPanel("LFO Waveform");
        JPanel rateGroup = createGroupPanel("Rate");
        JPanel amountGroup = createGroupPanel("Amount");
        JPanel destGroup = createGroupPanel("Destination");
        
        // Create controls
        JComboBox<String> lfoWaveformCombo = new JComboBox<>(
                new String[]{"Sine", "Triangle", "Square", "Sample & Hold"});
        Dial lfoRateDial = createCompactDial("", "LFO Speed", 50);
        Dial lfoAmountDial = createCompactDial("", "LFO Amount", 0);
        JComboBox<String> lfoDestCombo = new JComboBox<>(
                new String[]{"Off", "Pitch", "Filter", "Amp"});
        
        // Add controls to their groups
        JPanel wavePanel = new JPanel(new BorderLayout());
        wavePanel.add(lfoWaveformCombo, BorderLayout.CENTER);
        waveGroup.add(wavePanel);
        
        addControlsToGroupPanel(rateGroup, lfoRateDial);
        addControlsToGroupPanel(amountGroup, lfoAmountDial);
        
        JPanel destPanel = new JPanel(new BorderLayout());
        destPanel.add(lfoDestCombo, BorderLayout.CENTER);
        destGroup.add(destPanel);
        
        // First row: Waveform selector
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(waveGroup, gbc);
        
        // Second row: Rate and Amount
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(rateGroup, gbc);
        
        gbc.gridx = 1;
        panel.add(amountGroup, gbc);
        
        // Third row: Destination
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(destGroup, gbc);
        
        // Add control change listeners
        lfoWaveformCombo.addActionListener(e -> {
            int waveform = lfoWaveformCombo.getSelectedIndex();
            setControlChange(12, waveform * 32);
        });
        
        lfoRateDial.addChangeListener(e -> setControlChange(76, lfoRateDial.getValue()));
        lfoAmountDial.addChangeListener(e -> setControlChange(77, lfoAmountDial.getValue()));
        
        lfoDestCombo.addActionListener(e -> {
            int dest = lfoDestCombo.getSelectedIndex();
            setControlChange(13, dest * 32);
        });
        
        // Add the panel to the main container with some glue at bottom
        mainPanel.add(panel, BorderLayout.NORTH);
        mainPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    // Helper method to create compact dials with consistent style
    private Dial createCompactDial(String label, String tooltip, int initialValue) {
        Dial dial = new Dial();
        dial.setLabel(label);
        dial.setToolTipText(tooltip);
        dial.setValue(initialValue);
        dial.setMaximumSize(new Dimension(40, 40));
        dial.setPreferredSize(new Dimension(40, 40));
        return dial;
    }
    
    // Modify the group panel to expand horizontally
    private JPanel createGroupPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
        panel.add(innerPanel, BorderLayout.CENTER);
        
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            title, 
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Dialog", Font.PLAIN, 11)
        ));
        
        return panel;
    }
    
    // Modify addControlsToGroupPanel to add to the inner panel
    private void addControlsToGroupPanel(JPanel group, Component... controls) {
        JPanel innerPanel = (JPanel)group.getComponent(0);
        for (Component control : controls) {
            innerPanel.add(control);
        }
    }
    
    // Helper method to create a labeled control panel
    private JPanel createLabeledControl(String labelText, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(new JLabel(labelText), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }
}