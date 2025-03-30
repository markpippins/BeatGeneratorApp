package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Hashtable;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
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
        
        // Main panel with tabs - simplified to just one tab
        JTabbedPane paramTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        paramTabs.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        
        // Add a single comprehensive Parameters tab
        paramTabs.addTab("Parameters", createParamsPanel());
        
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
    
    private JPanel createParamsPanel() {
        // Main panel with vertical layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create a container for the oscillator rows
        JPanel oscillatorsPanel = new JPanel();
        oscillatorsPanel.setLayout(new BoxLayout(oscillatorsPanel, BoxLayout.Y_AXIS));
        
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
        
        // Add oscillators section to main panel
        mainPanel.add(oscillatorsPanel);
        mainPanel.add(Box.createVerticalStrut(20)); // Spacer between sections
        
        // Create bottom row with Envelope, Filter, and Modulation panels
        JPanel bottomRow = new JPanel(new GridLayout(1, 3, 10, 0));
        
        // 1. Add Envelope panel (extracted from createEnvelopePanel)
        JPanel envelopePanel = createCompactEnvelopePanel();
        bottomRow.add(envelopePanel);
        
        // 2. Add vertical Filter panel container
        JPanel filterContainer = new JPanel();
        filterContainer.setLayout(new BoxLayout(filterContainer, BoxLayout.Y_AXIS));
        
        // Extract filter components from createFilterPanel method
        JPanel filterTypePanel = createFilterTypePanel();
        JPanel filterParamsPanel = createFilterParamsPanel();
        
        filterContainer.add(filterTypePanel);
        filterContainer.add(Box.createVerticalStrut(5));
        filterContainer.add(filterParamsPanel);
        bottomRow.add(filterContainer);
        
        // 3. Add Modulation panel (extracted from createModulationPanel)
        JPanel modulationPanel = createCompactLfoPanel();
        bottomRow.add(modulationPanel);
        
        // Add bottom row to main panel
        mainPanel.add(bottomRow);
        
        // Add scrolling for the entire panel
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
    
    /**
     * Create a more compact envelope panel for use in the Parameters tab
     */
    private JPanel createCompactEnvelopePanel() {
        JPanel adsrPanel = new JPanel();
        adsrPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Envelope",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 11)
        ));
        
        // Use FlowLayout for sliders in a row
        adsrPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));
        
        // Create ADSR sliders
        JSlider attackSlider = createVerticalSlider("Attack Time", 10);
        JSlider decaySlider = createVerticalSlider("Decay Time", 50);
        JSlider sustainSlider = createVerticalSlider("Sustain Level", 80);
        JSlider releaseSlider = createVerticalSlider("Release Time", 20);
        
        // Create slider groups with labels
        JPanel attackGroup = createSliderGroup("Attack", attackSlider);
        JPanel decayGroup = createSliderGroup("Decay", decaySlider);
        JPanel sustainGroup = createSliderGroup("Sustain", sustainSlider);
        JPanel releaseGroup = createSliderGroup("Release", releaseSlider);
        
        // Add labeled sliders to envelope panel
        adsrPanel.add(attackGroup);
        adsrPanel.add(decayGroup);
        adsrPanel.add(sustainGroup);
        adsrPanel.add(releaseGroup);
        
        // Add control change listeners
        attackSlider.addChangeListener(e -> {
            if (!attackSlider.getValueIsAdjusting()) {
                setControlChange(73, attackSlider.getValue());
            }
        });
        
        decaySlider.addChangeListener(e -> {
            if (!decaySlider.getValueIsAdjusting()) {
                setControlChange(75, decaySlider.getValue());
            }
        });
        
        sustainSlider.addChangeListener(e -> {
            if (!sustainSlider.getValueIsAdjusting()) {
                setControlChange(79, sustainSlider.getValue());
            }
        });
        
        releaseSlider.addChangeListener(e -> {
            if (!releaseSlider.getValueIsAdjusting()) {
                setControlChange(72, releaseSlider.getValue());
            }
        });
        
        return adsrPanel;
    }

    /**
     * Create just the filter type panel
     */
    private JPanel createFilterTypePanel() {
        JPanel typePanel = new JPanel(new BorderLayout());
        typePanel.setBorder(BorderFactory.createTitledBorder("Filter Type"));
        
        JComboBox<String> filterTypeCombo = new JComboBox<>(
                new String[]{"Low Pass", "High Pass", "Band Pass", "Notch"});
        typePanel.add(filterTypeCombo, BorderLayout.CENTER);
        
        // Add event listener
        filterTypeCombo.addActionListener(e -> {
            int filterType = filterTypeCombo.getSelectedIndex();
            setControlChange(102, filterType * 32); // Custom CC for filter type
        });
        
        return typePanel;
    }

    /**
     * Create just the filter parameters panel
     */
    private JPanel createFilterParamsPanel() {
        JPanel filterParamsPanel = new JPanel();
        filterParamsPanel.setBorder(BorderFactory.createTitledBorder("Filter Parameters"));
        filterParamsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));
        
        // Create sliders
        JSlider cutoffSlider = createVerticalSlider("Filter Cutoff Frequency", 100);
        JSlider resonanceSlider = createVerticalSlider("Resonance/Q", 10);
        JSlider envAmountSlider = createVerticalSlider("Envelope Amount", 0);
        
        // Create slider groups
        JPanel cutoffGroup = createSliderGroup("Cutoff", cutoffSlider);
        JPanel resonanceGroup = createSliderGroup("Resonance", resonanceSlider);
        JPanel envAmtGroup = createSliderGroup("Env Amount", envAmountSlider);
        
        // Add to filter params panel
        filterParamsPanel.add(cutoffGroup);
        filterParamsPanel.add(resonanceGroup);
        filterParamsPanel.add(envAmtGroup);
        
        // Add event listeners
        cutoffSlider.addChangeListener(e -> {
            if (!cutoffSlider.getValueIsAdjusting()) {
                setControlChange(74, cutoffSlider.getValue());
            }
        });
        
        resonanceSlider.addChangeListener(e -> {
            if (!resonanceSlider.getValueIsAdjusting()) {
                setControlChange(71, resonanceSlider.getValue());
            }
        });
        
        envAmountSlider.addChangeListener(e -> {
            if (!envAmountSlider.getValueIsAdjusting()) {
                setControlChange(110, envAmountSlider.getValue());
            }
        });
        
        return filterParamsPanel;
    }

    /**
     * Create a more compact LFO panel for use in the Parameters tab
     */
    private JPanel createCompactLfoPanel() {
        JPanel lfoPanel = new JPanel();
        lfoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "LFO Controls",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Dialog", Font.BOLD, 11)
        ));
        
        // Use FlowLayout for sliders in a row
        lfoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));
        
        // Create vertical sliders with labeled ticks
        JSlider waveformSlider = createLabeledVerticalSlider(
            "LFO Waveform", 0, 3, 0, 
            new String[]{"Sine", "Triangle", "Square", "S&H"}
        );
        
        JSlider destinationSlider = createLabeledVerticalSlider(
            "LFO Destination", 0, 3, 0,
            new String[]{"Off", "Pitch", "Filter", "Amp"}
        );
        
        JSlider rateSlider = createVerticalSlider("LFO Rate", 50);
        JSlider amountSlider = createVerticalSlider("LFO Amount", 0);
        
        // Create slider groups with labels
        JPanel waveGroup = createSliderGroup("Waveform", waveformSlider);
        JPanel destGroup = createSliderGroup("Destination", destinationSlider);
        JPanel rateGroup = createSliderGroup("Rate", rateSlider);
        JPanel amountGroup = createSliderGroup("Amount", amountSlider);
        
        // Add slider groups to panel
        lfoPanel.add(waveGroup);
        lfoPanel.add(destGroup);
        lfoPanel.add(rateGroup);
        lfoPanel.add(amountGroup);
        
        // Add event listeners
        waveformSlider.addChangeListener(e -> {
            if (!waveformSlider.getValueIsAdjusting()) {
                int value = waveformSlider.getValue();
                setControlChange(12, value * 42); // Scale to 0-127 range
            }
        });
        
        destinationSlider.addChangeListener(e -> {
            if (!destinationSlider.getValueIsAdjusting()) {
                int value = destinationSlider.getValue();
                setControlChange(13, value * 42); // Scale to 0-127 range
            }
        });
        
        rateSlider.addChangeListener(e -> {
            if (!rateSlider.getValueIsAdjusting()) {
                setControlChange(76, rateSlider.getValue());
            }
        });
        
        amountSlider.addChangeListener(e -> {
            if (!amountSlider.getValueIsAdjusting()) {
                setControlChange(77, amountSlider.getValue());
            }
        });
        
        return lfoPanel;
    }

    /**
     * Create a slider with a label underneath
     */
    private JPanel createSliderGroup(String title, JSlider slider) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        
        // Center the slider
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sliderPanel.add(slider);
        
        // Add label at bottom
        JLabel label = new JLabel(title);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        group.add(sliderPanel);
        group.add(label);
        
        return group;
    }

    /**
     * Create a vertical slider with consistent styling
     * 
     * @param tooltip Tooltip text
     * @param initialValue Initial value (0-127)
     * @return Configured JSlider
     */
    private JSlider createVerticalSlider(String tooltip, int initialValue) {
        JSlider slider = new JSlider(SwingConstants.VERTICAL, 0, 127, initialValue);
        slider.setToolTipText(tooltip);
        
        // Set up tick marks
        slider.setMajorTickSpacing(32);
        slider.setMinorTickSpacing(16);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(false);
        
        // Create tick labels - just show a few key values
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(32, new JLabel("32"));
        labelTable.put(64, new JLabel("64"));
        labelTable.put(96, new JLabel("96"));
        labelTable.put(127, new JLabel("127"));
        slider.setLabelTable(labelTable);
        
        // FlatLaf properties
        slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        slider.putClientProperty("JSlider.paintThumbArrowShape", Boolean.TRUE);
        
        // Set reasonable size for a vertical slider
        slider.setPreferredSize(new Dimension(60, 120));
        
        return slider;
    }

    /**
     * Create a vertical slider with labeled tick marks
     * 
     * @param tooltip Tooltip text
     * @param min Minimum value
     * @param max Maximum value 
     * @param initialValue Initial value
     * @param labels Array of labels for tick marks
     * @return Configured JSlider with labels
     */
    private JSlider createLabeledVerticalSlider(String tooltip, int min, int max, int initialValue, String[] labels) {
        JSlider slider = new JSlider(SwingConstants.VERTICAL, min, max, initialValue);
        slider.setToolTipText(tooltip);
        
        // Set up tick marks and labels
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        
        // Create tick labels
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        for (int i = min; i <= max; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Dialog", Font.PLAIN, 9));
            labelTable.put(i, label);
        }
        slider.setLabelTable(labelTable);
        
        // Add FlatLaf styling
        slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        slider.putClientProperty("JSlider.paintThumbArrowShape", Boolean.TRUE);
        
        // Set reasonable size for a vertical slider
        slider.setPreferredSize(new Dimension(60, 120));
        
        return slider;
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
}