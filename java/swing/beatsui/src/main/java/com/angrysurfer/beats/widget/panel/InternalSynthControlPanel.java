package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Consumer;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.service.InternalSynthManager;

/**
 * Panel for controlling a MIDI synthesizer with tabs for oscillator, envelope,
 * filter and modulation parameters.
 */
public class InternalSynthControlPanel extends JPanel {

    private final Synthesizer synthesizer;
    private JComboBox<PresetItem> presetCombo;
    private final int midiChannel = 15; // Use channel 16 (index 15)
    private JComboBox<String> soundbankCombo;
    private JComboBox<Integer> bankCombo;
    private Soundbank currentSoundbank;
    private List<Soundbank> loadedSoundbanks = new ArrayList<>();
    private List<String> soundbankNames = new ArrayList<>();

    /**
     * Create a new SynthControlPanel
     * 
     * @param synthesizer The MIDI synthesizer to control
     */
    public InternalSynthControlPanel(Synthesizer synthesizer) {
        super(new BorderLayout(5, 5));
        this.synthesizer = synthesizer;
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        setupUI();
    }

    private void setupUI() {
        // Create preset selection at top
        JPanel soundPanel = createSoundPanel();
        add(soundPanel, BorderLayout.NORTH);

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
        setControlChange(midiChannel, ccNumber, value);
    }

    /**
     * Set a MIDI CC value on the synth with specified channel
     * 
     * @param channel MIDI channel (0-15)
     * @param ccNumber The CC number to set
     * @param value The value to set (0-127)
     */
    private void setControlChange(int channel, int ccNumber, int value) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel midiChannel = synthesizer.getChannels()[channel];
                if (midiChannel != null) {
                    midiChannel.controlChange(ccNumber, value);
                }
            } catch (Exception e) {
                System.err.println("Error setting CC " + ccNumber + " on channel " + (channel + 1) +
                        ": " + e.getMessage());
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
     * Create a panel with soundbank and preset selectors
     */
    private JPanel createSoundPanel() {
        JPanel soundPanel = new JPanel(new BorderLayout(5, 5));
        soundPanel.setBorder(BorderFactory.createTitledBorder("Sounds"));

        // Create a single row panel for all controls
        JPanel controlsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));

        // 1. Soundbank selector section
        JPanel soundbankSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        soundbankSection.add(new JLabel("Soundbank:"));
        soundbankCombo = new JComboBox<>();
        soundbankCombo.setPrototypeDisplayValue("SoundFont 2.0 (Default)XXXXXX");
        soundbankSection.add(soundbankCombo);

        // Load soundbank button
        JButton loadSoundbankBtn = new JButton("Load...");
        loadSoundbankBtn.addActionListener(e -> loadSoundbankFile());
        soundbankSection.add(loadSoundbankBtn);

        // 2. Bank selector section
        JPanel bankSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        bankSection.add(new JLabel("Bank:"));
        bankCombo = new JComboBox<>();
        bankCombo.setPreferredSize(new Dimension(60, 25));
        bankSection.add(bankCombo);

        // 3. Preset selector section
        JPanel presetSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        presetSection.add(new JLabel("Preset:"));
        presetCombo = new JComboBox<>();
        presetCombo.setPreferredSize(new Dimension(250, 25));
        presetSection.add(presetCombo);

        // Add all sections to the row
        controlsRow.add(soundbankSection);
        controlsRow.add(bankSection);
        controlsRow.add(presetSection);

        // Add the controls row to the main panel
        soundPanel.add(controlsRow, BorderLayout.NORTH);

        // Initialize default soundbank
        initializeSoundbanks();

        // Add listeners
        soundbankCombo.addActionListener(e -> {
            if (soundbankCombo.getSelectedIndex() >= 0) {
                selectSoundbank(soundbankCombo.getSelectedIndex());
            }
        });

        bankCombo.addActionListener(e -> {
            if (bankCombo.getSelectedIndex() >= 0) {
                populatePresetComboForBank((Integer) bankCombo.getSelectedItem());
            }
        });

        presetCombo.addActionListener(e -> {
            if (presetCombo.getSelectedItem() instanceof PresetItem) {
                PresetItem item = (PresetItem) presetCombo.getSelectedItem();
                int bank = bankCombo.getSelectedIndex() >= 0 ? (Integer) bankCombo.getSelectedItem() : 0;
                setProgramChange(bank, item.getNumber());
                System.out.println("Selected preset: " + item.getName() + " (Bank " + bank + ", Program " + item.getNumber() + ")");
            }
        });

        return soundPanel;
    }

    /**
     * Initialize available soundbanks
     */
    private void initializeSoundbanks() {
        try {
            // Clear previous data
            soundbankCombo.removeAllItems();
            loadedSoundbanks.clear();
            soundbankNames.clear();

            // Add the default Java soundbank
            soundbankCombo.addItem("Java Internal Soundbank");
            loadedSoundbanks.add(null); // Placeholder for default soundbank
            soundbankNames.add("Java Internal Soundbank");

            // If synthesizer is using a soundbank already, add it
            if (synthesizer != null && synthesizer.getDefaultSoundbank() != null) {
                Soundbank defaultSoundbank = synthesizer.getDefaultSoundbank();
                String name = defaultSoundbank.getName();
                if (name != null && !name.isEmpty() && !name.equals("Unknown")) {
                    soundbankCombo.addItem(name);
                    loadedSoundbanks.add(defaultSoundbank);
                    soundbankNames.add(name);
                }
            }

            // Select the first soundbank
            if (soundbankCombo.getItemCount() > 0) {
                soundbankCombo.setSelectedIndex(0);
            }
        } catch (Exception e) {
            System.err.println("Error initializing soundbanks: " + e.getMessage());
        }
    }

    /**
     * Select a soundbank and load it into the synthesizer
     */
    private void selectSoundbank(int index) {
        try {
            if (index < 0 || index >= loadedSoundbanks.size()) {
                return;
            }

            Soundbank soundbank = loadedSoundbanks.get(index);
            currentSoundbank = soundbank;

            // Load the soundbank into the synthesizer if it's not the default
            if (soundbank != null && synthesizer != null) {
                // First unload any current instruments
                synthesizer.unloadAllInstruments(synthesizer.getDefaultSoundbank());

                // Then load the new soundbank
                boolean loaded = synthesizer.loadAllInstruments(soundbank);
                if (loaded) {
                    System.out.println("Loaded soundbank: " + soundbank.getName());
                } else {
                    System.err.println("Failed to load soundbank: " + soundbank.getName());
                }
            }

            // Populate banks for this soundbank
            populateBanksCombo();
        } catch (Exception e) {
            System.err.println("Error selecting soundbank: " + e.getMessage());
        }
    }

    /**
     * Populate the bank combo box with available banks
     */
    private void populateBanksCombo() {
        bankCombo.removeAllItems();

        if (synthesizer != null) {
            // Always add bank 0 (GM sounds)
            bankCombo.addItem(0);

            // Add banks 1-15 if this is a multi-bank soundfont
            if (currentSoundbank != null) {
                // Check if this is likely a multi-bank soundfont by sampling a few instruments
                boolean hasMultipleBanks = false;

                for (javax.sound.midi.Instrument instrument : currentSoundbank.getInstruments()) {
                    Patch patch = instrument.getPatch();
                    if (patch.getBank() > 0) {
                        hasMultipleBanks = true;
                        break;
                    }
                }

                if (hasMultipleBanks) {
                    // Add standard banks
                    for (int i = 1; i <= 15; i++) {
                        bankCombo.addItem(i);
                    }
                }
            }
        }

        // Select the first bank
        if (bankCombo.getItemCount() > 0) {
            bankCombo.setSelectedIndex(0);
        }
    }

    /**
     * Populate the preset combo box with presets from the selected bank
     */
    private void populatePresetComboForBank(int bank) {
        presetCombo.removeAllItems();

        // Get preset names from InternalSynthManager or create generic names
        List<String> presetNames = new ArrayList<>();

        try {
            // First try to get from manager
            presetNames = com.angrysurfer.core.service.InternalSynthManager.getInstance().getPresetNames(1L);
        } catch (Exception e) {
            System.err.println("Error getting preset names: " + e.getMessage());
        }

        // Check if we have a soundbank with named instruments
        if (currentSoundbank != null) {
            for (int i = 0; i < 128; i++) {
                // Try to find instrument in the soundbank
                javax.sound.midi.Instrument instrument = currentSoundbank.getInstrument(new Patch(bank, i));
                String presetName;

                if (instrument != null) {
                    presetName = instrument.getName();
                } else if (i < presetNames.size() && presetNames.get(i) != null && !presetNames.get(i).isEmpty()) {
                    presetName = presetNames.get(i);
                } else {
                    presetName = "Program " + i;
                }

                // Add the preset to the combo box
                presetCombo.addItem(new PresetItem(i, i + ": " + presetName));
            }
        } else {
            // Default GM sounds
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
        }

        // Select the first preset by default
        if (presetCombo.getItemCount() > 0) {
            presetCombo.setSelectedIndex(0);
        }
    }

    /**
     * Load a soundbank file from disk
     */
    private void loadSoundbankFile() {
        try {
            // Use DialogManager approach but implement directly here
            File soundbankFile = showSoundbankFileChooser();

            if (soundbankFile != null && soundbankFile.exists()) {
                System.out.println("Loading soundbank file: " + soundbankFile.getAbsolutePath());

                // Load the soundbank
                Soundbank soundbank = MidiSystem.getSoundbank(soundbankFile);

                if (soundbank != null) {
                    // Add to our list
                    String name = soundbank.getName();
                    if (name == null || name.isEmpty()) {
                        name = soundbankFile.getName();
                    }

                    soundbankCombo.addItem(name);
                    loadedSoundbanks.add(soundbank);
                    soundbankNames.add(name);

                    // Select the newly added soundbank
                    soundbankCombo.setSelectedIndex(soundbankCombo.getItemCount() - 1);

                    System.out.println("Loaded soundbank: " + name);
                } else {
                    System.err.println("Failed to load soundbank from file");
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading soundbank: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show a file chooser dialog for selecting soundbank files
     */
    private File showSoundbankFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Soundbank File");

        // Add filters for soundbank formats
        FileNameExtensionFilter sfFilter = new FileNameExtensionFilter(
                "SoundFont Files (*.sf2, *.dls)", "sf2", "dls");
        fileChooser.addChoosableFileFilter(sfFilter);
        fileChooser.setFileFilter(sfFilter);
        fileChooser.setAcceptAllFileFilterUsed(true);

        // Show open dialog
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }

        return null;
    }

    /**
     * Send program change to the synthesizer with bank selection
     */
    private void setProgramChange(int bank, int program) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                // First, silence any playing notes
                for (int i = 0; i < 16; i++) {
                    MidiChannel channel = synthesizer.getChannels()[i];
                    if (channel != null) {
                        channel.allNotesOff();
                        channel.allSoundOff();
                    }
                }

                // Set the bank and program on the main channel
                MidiChannel channel = synthesizer.getChannels()[midiChannel];
                if (channel != null) {
                    // Send bank select MSB (CC 0) and LSB (CC 32)
                    channel.controlChange(0, bank >> 7); // Bank MSB
                    channel.controlChange(32, bank & 0x7F); // Bank LSB

                    // Small delay to ensure bank change is processed
                    Thread.sleep(5);

                    // Then program change
                    channel.programChange(program);
                    System.out.println("Changed synth to bank " + bank + ", program " + program);

                    // Reset all channels to ensure clean state
                    for (int i = 0; i < 3; i++) {
                        if (synthesizer.getChannels()[i] != null) {
                            synthesizer.getChannels()[i].resetAllControllers();
                            Thread.sleep(1); // Small delay
                        }
                    }

                    // Force update UI controls regardless of previous state
                    resetControlsToDefault();

                    // Force reinitialize all controller values
                    reinitializeControllers();
                }
            } catch (Exception e) {
                System.err.println("Error changing program: " + e.getMessage());
            }
        }
    }

    /**
     * Reset UI controls to default values after preset change
     */
    private void resetControlsToDefault() {
     System.out.println("Resetting UI controls to default values...");
        
        // Process all checkboxes first - turn off oscillators 2 and 3
        findAndResetComponents(this, JCheckBox.class, component -> {
            JCheckBox checkBox = (JCheckBox) component;
            if (checkBox.getName() != null) {
                boolean shouldBeSelected = checkBox.getName().equals("osc0Toggle");
                checkBox.setSelected(shouldBeSelected);
                System.out.println("Reset " + checkBox.getName() + " to " + shouldBeSelected);
            }
        });
        
        // Reset all combo boxes to first position
        findAndResetComponents(this, JComboBox.class, component -> {
            JComboBox<?> comboBox = (JComboBox<?>) component;
            if (comboBox.getName() != null && 
                !comboBox.getName().equals("presetCombo") && 
                !comboBox.getName().equals("soundbankCombo") && 
                !comboBox.getName().equals("bankCombo")) {
                comboBox.setSelectedIndex(0);
                System.out.println("Reset " + comboBox.getName() + " to index 0");
            }
        });
        
        // Reset all sliders to middle values
        findAndResetComponents(this, JSlider.class, component -> {
            JSlider slider = (JSlider) component;
            int defaultValue = (slider.getMaximum() - slider.getMinimum()) / 2 + slider.getMinimum();
            slider.setValue(defaultValue);
            System.out.println("Reset slider to " + defaultValue);
        });
        
        // Reset all dials to appropriate values
        findAndResetComponents(this, Dial.class, component -> {
            Dial dial = (Dial) component;
            String name = dial.getName();
            int value = 64; // Default to middle
            
            // Set specific defaults based on control name
            if (name != null) {
                if (name.contains("volume")) {
                    // Set oscillators 2-3 to zero, first one to full
                    if (name.equals("volumeDial0")) {
                        value = 100;  // Oscillator 1 on full volume
                    } else {
                        value = 0;    // Other oscillators muted
                    }
                } else if (name.contains("tune") || name.contains("detune")) {
                    value = 64;  // Middle for tuning
                } else if (name.contains("master")) {
                    value = 100; // Master volume full
                }
                System.out.println("Reset " + name + " to " + value);
            }
            
            dial.setValue(value);
        });
        
        // After resetting UI, force MIDI controller resets for all channels
        try {
            for (int ch = 0; ch < 3; ch++) {
                // Set volume for the oscillators - only first one on
                int baseCCForOsc = ch * 20 + 20;
                int volume = (ch == 0) ? 100 : 0;
                setControlChange(midiChannel, baseCCForOsc + 4, volume);
                
                // Reset all other controllers
                setControlChange(midiChannel, baseCCForOsc, 0);     // Waveform
                setControlChange(midiChannel, baseCCForOsc + 1, 64); // Octave
                setControlChange(midiChannel, baseCCForOsc + 2, 64); // Tune
                setControlChange(midiChannel, baseCCForOsc + 3, 64); // Brightness
            }
            
            // Set master volume
            setControlChange(midiChannel, 7, 100);
        } catch (Exception e) {
            System.err.println("Error resetting controllers: " + e.getMessage());
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

        // Create bottom row with Envelope, Filter, Modulation, and Effects panels
        JPanel bottomRow = new JPanel(new GridLayout(1, 4, 10, 0));

        // 1. Add Envelope panel
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

        // 3. Add Modulation panel
        JPanel modulationPanel = createCompactLfoPanel();
        bottomRow.add(modulationPanel);

        // 4. Add Effects panel
        JPanel effectsPanel = createEffectsPanel();
        bottomRow.add(effectsPanel);

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
     * Create effects panel with vertical sliders for various effects parameters
     */
    private JPanel createEffectsPanel() {
        JPanel effectsPanel = new JPanel();
        effectsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Effects",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Dialog", Font.BOLD, 11)
        ));

        // Use FlowLayout for sliders in a row with good spacing
        effectsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 5));

        // Create effect type selector with labeled vertical slider
        JSlider effectTypeSlider = createLabeledVerticalSlider(
                "Effect Type", 0, 3, 0,
                new String[]{"Reverb", "Delay", "Chorus", "Drive"}
        );

        // Create parameter sliders
        JSlider param1Slider = createVerticalSlider("Parameter 1", 0);
        JSlider param2Slider = createVerticalSlider("Parameter 2", 0);
        JSlider mixSlider = createVerticalSlider("Mix", 0);

        // Create slider groups with labels
        JPanel typeGroup = createSliderGroup("Type", effectTypeSlider);
        JPanel param1Group = createSliderGroup("Size/Time", param1Slider);
        JPanel param2Group = createSliderGroup("Decay/Fdbk", param2Slider);
        JPanel mixGroup = createSliderGroup("Mix", mixSlider);

        // Add slider groups to panel
        effectsPanel.add(typeGroup);
        effectsPanel.add(param1Group);
        effectsPanel.add(param2Group);
        effectsPanel.add(mixGroup);

        // Add event listeners
        effectTypeSlider.addChangeListener(e -> {
            if (!effectTypeSlider.getValueIsAdjusting()) {
                int effectType = effectTypeSlider.getValue();
                setControlChange(91, effectType * 32); // CC 91 for effect type

                // Update labels based on effect type
                switch (effectType) {
                    case 0: // Reverb
                        param1Group.setBorder(BorderFactory.createTitledBorder("Size"));
                        param2Group.setBorder(BorderFactory.createTitledBorder("Decay"));
                        break;
                    case 1: // Delay
                        param1Group.setBorder(BorderFactory.createTitledBorder("Time"));
                        param2Group.setBorder(BorderFactory.createTitledBorder("Feedback"));
                        break;
                    case 2: // Chorus
                        param1Group.setBorder(BorderFactory.createTitledBorder("Depth"));
                        param2Group.setBorder(BorderFactory.createTitledBorder("Rate"));
                        break;
                    case 3: // Drive
                        param1Group.setBorder(BorderFactory.createTitledBorder("Amount"));
                        param2Group.setBorder(BorderFactory.createTitledBorder("Tone"));
                        break;
                }
            }
        });

        param1Slider.addChangeListener(e -> {
            if (!param1Slider.getValueIsAdjusting()) {
                setControlChange(92, param1Slider.getValue()); // CC 92 for param 1
            }
        });

        param2Slider.addChangeListener(e -> {
            if (!param2Slider.getValueIsAdjusting()) {
                setControlChange(93, param2Slider.getValue()); // CC 93 for param 2
            }
        });

        mixSlider.addChangeListener(e -> {
            if (!mixSlider.getValueIsAdjusting()) {
                setControlChange(94, mixSlider.getValue()); // CC 94 for mix level
            }
        });

        return effectsPanel;
    }

    /**
     * Create a single row of oscillator controls
     * 
     * @param title The oscillator's title
     * @param index Oscillator index (0-2) for CC mapping
     * @return A panel containing a row of controls for one oscillator
     */
    private JPanel createOscillatorRow(String title, int index) {
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

        // Use a more direct CC approach instead of multiple channels
        // Map common synth parameters to standard GM CCs
        int baseCCForOsc = index * 20 + 20; // Space out CC numbers by oscillator 

        // Waveform selector 
        JPanel waveGroup = createCompactGroup("Waveform");
        JComboBox<String> waveformCombo = new JComboBox<>(
                new String[]{"Sine", "Square", "Saw", "Triangle", "Pulse"});
        waveformCombo.setName("waveformCombo" + index);
        waveGroup.add(waveformCombo);

        // Octave selector
        JPanel octaveGroup = createCompactGroup("Octave");
        JComboBox<String> octaveCombo = new JComboBox<>(
                new String[]{"-2", "-1", "0", "+1", "+2"});
        octaveCombo.setName("octaveCombo" + index);
        octaveCombo.setSelectedIndex(2); // Default to "0"
        octaveGroup.add(octaveCombo);

        // Create parameter dials
        Dial detuneDial = createCompactDial("", "Tuning", 64);
        detuneDial.setName("detuneDial" + index);

        Dial brightnessDialDial = createCompactDial("", "Brightness", 64);
        brightnessDialDial.setName("brightnessDial" + index);

        Dial volumeDial = createCompactDial("", "Volume", index == 0 ? 100 : 0);
        volumeDial.setName("volumeDial" + index);

        // Create groups for each dial
        JPanel detuneGroup = createCompactGroup("Tune");
        detuneGroup.add(detuneDial);

        JPanel brightnessGroup = createCompactGroup("Bright");
        brightnessGroup.add(brightnessDialDial);

        JPanel volumeGroup = createCompactGroup("Volume");
        volumeGroup.add(volumeDial);

        // Add toggle switch for oscillator on/off
        JPanel toggleGroup = createCompactGroup("On/Off");
        JCheckBox enabledToggle = new JCheckBox();
        enabledToggle.setName("osc" + index + "Toggle");
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
        oscPanel.add(brightnessGroup);
        oscPanel.add(Box.createHorizontalStrut(5));
        oscPanel.add(volumeGroup);
        oscPanel.add(Box.createHorizontalGlue()); // Push everything to the left

        // Add improved event handlers
        waveformCombo.addActionListener(e -> {
            int waveformType = waveformCombo.getSelectedIndex();

            // Use a better approach for waveforms - send to main channel but use different CCs
            if (enabledToggle.isSelected()) {
                // Use CC for timbre instead of program changes - CC70-72 are for sound controllers
                setControlChange(midiChannel, baseCCForOsc, waveformType * 25);
                System.out.println("Osc " + (index + 1) + " waveform: " + waveformCombo.getSelectedItem() +
                        " (CC" + baseCCForOsc + "=" + (waveformType * 25) + ")");

                // Also force program change on main channel if this is oscillator 1
                if (index == 0 && presetCombo.getSelectedItem() instanceof PresetItem) {
                    PresetItem item = (PresetItem) presetCombo.getSelectedItem();
                    MidiChannel channel = synthesizer.getChannels()[midiChannel];
                    if (channel != null) {
                        channel.programChange(item.getNumber());
                    }
                }
            }
        });

        octaveCombo.addActionListener(e -> {
            int octave = octaveCombo.getSelectedIndex() - 2; // -2 to +2 range
            int value = (octave + 2) * 25 + 14;

            // Send as unique CC for this oscillator's octave
            if (enabledToggle.isSelected()) {
                setControlChange(midiChannel, baseCCForOsc + 1, value);
                System.out.println("Osc " + (index + 1) + " octave: " + octave +
                        " (CC" + (baseCCForOsc + 1) + "=" + value + ")");
            }
        });

        detuneDial.addChangeListener(e -> {
            if (enabledToggle.isSelected()) {
                setControlChange(midiChannel, baseCCForOsc + 2, detuneDial.getValue());
                System.out.println("Osc " + (index + 1) + " tune: " + detuneDial.getValue() +
                        " (CC" + (baseCCForOsc + 2) + ")");
            }
        });

        brightnessDialDial.addChangeListener(e -> {
            if (enabledToggle.isSelected()) {
                setControlChange(midiChannel, baseCCForOsc + 3, brightnessDialDial.getValue());
                System.out.println("Osc " + (index + 1) + " brightness: " + brightnessDialDial.getValue() +
                        " (CC" + (baseCCForOsc + 3) + ")");
            }
        });

        volumeDial.addChangeListener(e -> {
            // Only update if oscillator is enabled
            if (enabledToggle.isSelected()) {
                setControlChange(midiChannel, baseCCForOsc + 4, volumeDial.getValue());
                System.out.println("Osc " + (index + 1) + " volume: " + volumeDial.getValue() +
                        " (CC" + (baseCCForOsc + 4) + ")");
            } else {
                // Force volume to zero if disabled
                setControlChange(midiChannel, baseCCForOsc + 4, 0);
            }
        });

        enabledToggle.addActionListener(e -> {
            boolean enabled = enabledToggle.isSelected();

            // Set volume CC based on enabled state
            int volume = enabled ? volumeDial.getValue() : 0;
            setControlChange(midiChannel, baseCCForOsc + 4, volume);

            // Always send a message - even if volume is already 0
            System.out.println("Osc " + (index + 1) + " " + (enabled ? "enabled" : "disabled") +
                    " (CC" + (baseCCForOsc + 4) + "=" + volume + ")");

            // If enabling, also re-send all other settings
            if (enabled) {
                int waveformType = waveformCombo.getSelectedIndex();
                setControlChange(midiChannel, baseCCForOsc, waveformType * 25);

                int octave = octaveCombo.getSelectedIndex() - 2;
                setControlChange(midiChannel, baseCCForOsc + 1, (octave + 2) * 25 + 14);

                setControlChange(midiChannel, baseCCForOsc + 2, detuneDial.getValue());
                setControlChange(midiChannel, baseCCForOsc + 3, brightnessDialDial.getValue());
            }
        });

        return oscPanel;
    }

    /**
     * Reset controllers and synchronize controls after preset change
     */
    private void reinitializeControllers() {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel channel = synthesizer.getChannels()[midiChannel];

                // First reset all controllers and send all notes off
                channel.resetAllControllers();
                channel.allNotesOff();
                channel.allSoundOff();

                // Wait a tiny bit
                Thread.sleep(5);

                // Set basic channel parameters
                channel.controlChange(7, 100); // Volume
                channel.controlChange(10, 64); // Pan center
                channel.controlChange(11, 127); // Expression

                // Set all oscillator volumes to match UI state
                findAndResetComponents(this, JCheckBox.class, component -> {
                    JCheckBox checkBox = (JCheckBox) component;
                    if (checkBox.getName() != null && checkBox.getName().contains("Toggle")) {
                        int oscIndex = Integer.parseInt(checkBox.getName().substring(3, 4));
                        int baseCCForOsc = oscIndex * 20 + 20;
                        boolean enabled = checkBox.isSelected();

                        // Find associated volume dial
                        findAndResetComponents(this, Dial.class, dial -> {
                            if (dial.getName() != null && dial.getName().equals("volumeDial" + oscIndex)) {
                                int volume = enabled ? ((Dial) dial).getValue() : 0;
                                setControlChange(midiChannel, baseCCForOsc + 4, volume);
                            }
                        });
                    }
                });

                System.out.println("Reinitialized controllers after preset change");
            } catch (Exception e) {
                System.err.println("Error reinitializing controllers: " + e.getMessage());
            }
        }
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

    /**
     * Helper method to find and reset components by type
     * 
     * @param <T> The type of component to find
     * @param container The container to search in
     * @param componentClass The class of the component type to find
     * @param resetAction The action to perform on each found component
     */
    private <T extends Component> void findAndResetComponents(Container container,
            Class<T> componentClass, Consumer<Component> resetAction) {

        // Check all components in the container
        for (Component component : container.getComponents()) {
            // If component matches the requested class, apply the reset action
            if (componentClass.isAssignableFrom(component.getClass())) {
                resetAction.accept(component);
            }

            // If component is itself a container, recursively search it
            if (component instanceof Container) {
                findAndResetComponents((Container) component, componentClass, resetAction);
            }
        }
    }
}