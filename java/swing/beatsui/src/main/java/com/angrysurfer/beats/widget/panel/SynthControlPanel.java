package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
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
        // Main panel with vertical layout for oscillator rows
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Create a container for the oscillator rows
        JPanel oscillatorsPanel = new JPanel();
        oscillatorsPanel.setLayout(new BoxLayout(oscillatorsPanel, BoxLayout.Y_AXIS));
        oscillatorsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create three oscillators
        oscillatorsPanel.add(createOscillatorRow("Oscillator 1", 0));
        oscillatorsPanel.add(Box.createVerticalStrut(10)); // Spacer
        oscillatorsPanel.add(createOscillatorRow("Oscillator 2", 1));
        oscillatorsPanel.add(Box.createVerticalStrut(10)); // Spacer
        oscillatorsPanel.add(createOscillatorRow("Oscillator 3", 2));
        
        // Global controls for oscillators
        JPanel globalControls = createOscillatorMixingPanel();
        oscillatorsPanel.add(Box.createVerticalStrut(15));
        oscillatorsPanel.add(globalControls);
        
        // Add oscillators panel to main panel with glue at bottom
        mainPanel.add(oscillatorsPanel, BorderLayout.NORTH);
        mainPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);
        
        // Add a scroll pane in case the panel gets too wide
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(scrollPane, BorderLayout.CENTER);
        
        return containerPanel;
    }

    /**
     * Create a single row of oscillator controls
     * 
     * @param title The oscillator's title
     * @param index Oscillator index (0-2) for CC mapping
     * @return A panel containing a row of controls for one oscillator
     */
    private JPanel createOscillatorRow(String title, int index) {
        // Base CC offsets for different oscillators
        int ccOffset = index * 20; // Each osc uses CCs in its own range
        
        // Create panel for the oscillator with titled border
        JPanel oscPanel = new JPanel();
        oscPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 11)
        ));
        
        // Use horizontal box layout for oscillator controls
        oscPanel.setLayout(new BoxLayout(oscPanel, BoxLayout.X_AXIS));
        oscPanel.add(Box.createHorizontalStrut(5));
        
        // Waveform selector
        JPanel waveGroup = createCompactGroup("Waveform");
        JComboBox<String> waveformCombo = new JComboBox<>(
                new String[]{"Sine", "Square", "Saw", "Triangle", "Pulse"});
        waveGroup.add(waveformCombo);
        
        // Octave selector
        JPanel octaveGroup = createCompactGroup("Octave");
        JComboBox<String> octaveCombo = new JComboBox<>(
                new String[]{"-2", "-1", "0", "+1", "+2"});
        octaveGroup.add(octaveCombo);
        
        // Create parameter dials
        Dial detuneDial = createCompactDial("", "Detune Amount", 0);
        Dial pulseDial = createCompactDial("", "Pulse Width", 50);
        Dial levelDial = createCompactDial("", "Level", 75);
        
        // Create groups for each dial
        JPanel detuneGroup = createCompactGroup("Detune");
        detuneGroup.add(detuneDial);
        
        JPanel pulseGroup = createCompactGroup("Width");
        pulseGroup.add(pulseDial);
        
        JPanel levelGroup = createCompactGroup("Level");
        levelGroup.add(levelDial);
        
        // Add toggle switch for oscillator on/off
        JPanel toggleGroup = createCompactGroup("On/Off");
        JCheckBox enabledToggle = new JCheckBox();
        enabledToggle.setSelected(index == 0); // First oscillator on by default
        toggleGroup.add(enabledToggle);
        
        // Add components to the oscillator panel
        oscPanel.add(toggleGroup);
        oscPanel.add(Box.createHorizontalStrut(5));
        oscPanel.add(waveGroup);
        oscPanel.add(Box.createHorizontalStrut(5));
        oscPanel.add(octaveGroup);
        oscPanel.add(Box.createHorizontalStrut(5));
        oscPanel.add(detuneGroup);
        oscPanel.add(Box.createHorizontalStrut(5));
        oscPanel.add(pulseGroup);
        oscPanel.add(Box.createHorizontalStrut(5));
        oscPanel.add(levelGroup);
        oscPanel.add(Box.createHorizontalGlue()); // Push everything to the left
        
        // Add event handlers with oscillator-specific CC numbers
        waveformCombo.addActionListener(e -> {
            int waveformType = waveformCombo.getSelectedIndex();
            setControlChange(70 + ccOffset, waveformType * 25);
        });
        
        octaveCombo.addActionListener(e -> {
            int octave = octaveCombo.getSelectedIndex() - 2; // -2 to +2 range
            setControlChange(18 + ccOffset, (octave + 2) * 25);
        });
        
        detuneDial.addChangeListener(e -> setControlChange(94 + ccOffset, detuneDial.getValue()));
        pulseDial.addChangeListener(e -> setControlChange(70 + ccOffset, pulseDial.getValue()));
        levelDial.addChangeListener(e -> setControlChange(7 + ccOffset, levelDial.getValue()));
        
        enabledToggle.addActionListener(e -> {
            int value = enabledToggle.isSelected() ? 127 : 0;
            setControlChange(12 + ccOffset, value);
        });
        
        return oscPanel;
    }

    /**
     * Create global mixer panel for oscillator balance
     */
    private JPanel createOscillatorMixingPanel() {
        JPanel mixerPanel = new JPanel();
        mixerPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Oscillator Mix",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 11)
        ));
        mixerPanel.setLayout(new BoxLayout(mixerPanel, BoxLayout.X_AXIS));
        
        // Balance between osc 1 & 2
        JPanel balance12Group = createCompactGroup("Osc 1-2");
        Dial balance12Dial = createCompactDial("", "Balance Osc 1-2", 64);
        balance12Group.add(balance12Dial);
        
        // Balance between result and osc 3
        JPanel balance3Group = createCompactGroup("Osc 3 Mix");
        Dial balance3Dial = createCompactDial("", "Mix in Osc 3", 32);
        balance3Group.add(balance3Dial);
        
        // Master level
        JPanel masterGroup = createCompactGroup("Master");
        Dial masterDial = createCompactDial("", "Master Level", 100);
        masterGroup.add(masterDial);
        
        // Add components
        mixerPanel.add(Box.createHorizontalStrut(5));
        mixerPanel.add(balance12Group);
        mixerPanel.add(Box.createHorizontalStrut(10));
        mixerPanel.add(balance3Group);
        mixerPanel.add(Box.createHorizontalStrut(10));
        mixerPanel.add(masterGroup);
        mixerPanel.add(Box.createHorizontalGlue());
        
        // Add event handlers
        balance12Dial.addChangeListener(e -> setControlChange(8, balance12Dial.getValue()));
        balance3Dial.addChangeListener(e -> setControlChange(9, balance3Dial.getValue()));
        masterDial.addChangeListener(e -> setControlChange(7, masterDial.getValue()));
        
        return mixerPanel;
    }

    /**
     * Create a compact group panel for oscillator controls
     */
    private JPanel createCompactGroup(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Add title label
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(2));
        
        return panel;
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