package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiChannel;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.UIHelper;
import com.angrysurfer.core.service.InternalSynthManager;

/**
 * Panel for controlling a MIDI synthesizer with tabs for oscillator, envelope,
 * filter and modulation parameters.
 */
public class InternalSynthControlPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(InternalSynthControlPanel.class);

    private final Synthesizer synthesizer;
    private JComboBox<PresetItem> presetCombo;
    private final int midiChannel = 15; // Use channel 16 (index 15)
    private JComboBox<String> soundbankCombo;
    private JComboBox<Integer> bankCombo;
    private InternalSynthOscillatorPanel[] oscillatorPanels;
    private InternalSynthLFOPanel lfoPanel;
    private InternalSynthFilterPanel filterPanel;
    private InternalSynthEnvelopePanel envelopePanel;
    private InternalSynthEffectsPanel effectsPanel;
    private InternalSynthMixerPanel mixerPanel;
    
    // Track the currently selected soundbank name
    private String currentSoundbankName = null;

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
    protected void setControlChange(int ccNumber, int value) {
        setControlChange(midiChannel, ccNumber, value);
    }

    /**
     * Set a MIDI CC value on the synth with specified channel
     * 
     * @param channel MIDI channel (0-15)
     * @param ccNumber The CC number to set
     * @param value The value to set (0-127)
     */
    protected void setControlChange(int channel, int ccNumber, int value) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel midiCh = synthesizer.getChannels()[channel];
                if (midiCh != null) {
                    midiCh.controlChange(ccNumber, value);
                }
            } catch (Exception e) {
                logger.error("Error setting CC {} on channel {}: {}", ccNumber, channel + 1, e.getMessage());
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
        bankSection.add(new JLabel("Soundbank:"));
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
            if (soundbankCombo.getSelectedItem() != null) {
                String selectedName = (String) soundbankCombo.getSelectedItem();
                selectSoundbank(selectedName);
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
                logger.info("Selected preset: {} (Bank {}, Program {})", item.getName(), bank, item.getNumber());
            }
        });

        return soundPanel;
    }

    /**
     * Initialize available soundbanks
     */
    private void initializeSoundbanks() {
        try {
            // Use the manager instance to initialize soundbanks
            InternalSynthManager manager = InternalSynthManager.getInstance();
            manager.initializeSoundbanks();
            
            // Update UI with data from the manager
            soundbankCombo.removeAllItems();
            List<String> names = manager.getSoundbankNames();
            for (String name : names) {
                soundbankCombo.addItem(name);
            }
            
            // Temporarily remove the action listener to prevent double-triggering
            ActionListener[] listeners = soundbankCombo.getActionListeners();
            for (ActionListener listener : listeners) {
                soundbankCombo.removeActionListener(listener);
            }
            
            // Select the first soundbank
            if (soundbankCombo.getItemCount() > 0) {
                soundbankCombo.setSelectedIndex(0);
                String selectedName = (String) soundbankCombo.getSelectedItem();
                currentSoundbankName = selectedName;
                
                // Explicitly populate banks for this soundbank
                populateBanksCombo(selectedName);
            }
            
            // Restore the action listeners
            for (ActionListener listener : listeners) {
                soundbankCombo.addActionListener(listener);
            }
        } catch (Exception e) {
            logger.error("Error initializing soundbanks: {}", e.getMessage());
        }
    }

    /**
     * Select a soundbank and load it into the synthesizer
     */
    private void selectSoundbank(String soundbankName) {
        try {
            // Store the selected soundbank name
            currentSoundbankName = soundbankName;
            
            // Get the soundbank by name
            InternalSynthManager manager = InternalSynthManager.getInstance();
            Soundbank soundbank = manager.getSoundbankByName(soundbankName);
            
            // Load the soundbank into the synthesizer if it's not null
            if (soundbank != null && synthesizer != null && synthesizer.isOpen()) {
                // Unload any current instruments
                synthesizer.unloadAllInstruments(synthesizer.getDefaultSoundbank());
                
                // Load the new soundbank
                boolean loaded = synthesizer.loadAllInstruments(soundbank);
                if (loaded) {
                    logger.info("Loaded soundbank: {}", soundbankName);
                } else {
                    logger.error("Failed to load soundbank: {}", soundbankName);
                }
            }
            
            // Populate banks for this soundbank
            populateBanksCombo(soundbankName);
        } catch (Exception e) {
            logger.error("Error selecting soundbank: {}", e.getMessage());
        }
    }

    /**
     * Populate the bank combo box with available banks for a specific soundbank
     */
    private void populateBanksCombo(String soundbankName) {
        try {
            bankCombo.removeAllItems();
            
            // Get banks by soundbank name
            List<Integer> banks = InternalSynthManager.getInstance()
                    .getAvailableBanksByName(soundbankName);
            
            // Add banks to combo box
            for (Integer bank : banks) {
                bankCombo.addItem(bank);
            }

            // Temporarily remove the action listener
            ActionListener[] listeners = bankCombo.getActionListeners();
            for (ActionListener listener : listeners) {
                bankCombo.removeActionListener(listener);
            }
            
            // Select the first bank
            if (bankCombo.getItemCount() > 0) {
                bankCombo.setSelectedIndex(0);
                
                // Explicitly populate presets for this bank
                if (bankCombo.getSelectedItem() instanceof Integer bank) {
                    populatePresetComboForBank(bank);
                }
            }
            
            // Restore the action listeners
            for (ActionListener listener : listeners) {
                bankCombo.addActionListener(listener);
            }
        } catch (Exception e) {
            logger.error("Error populating banks: {}", e.getMessage());
        }
    }

    /**
     * Populate the preset combo box with presets from the selected bank
     */
    private void populatePresetComboForBank(int bank) {
        try {
            presetCombo.removeAllItems();
            
            // Get preset names from InternalSynthManager for the current soundbank
            InternalSynthManager manager = InternalSynthManager.getInstance();
            List<String> presetNames = manager.getPresetNames(currentSoundbankName, bank);
            
            logger.info("Retrieved {} preset names for bank {}", presetNames.size(), bank);
            
            // Add all presets to the combo box with format: "0: Acoustic Grand Piano"
            for (int i = 0; i < Math.min(128, presetNames.size()); i++) {
                String presetName = presetNames.get(i);
                
                // Use generic name if the specific name is empty
                if (presetName == null || presetName.isEmpty()) {
                    presetName = "Program " + i;
                }
                
                // Add the preset to the combo box
                presetCombo.addItem(new PresetItem(i, i + ": " + presetName));
            }
            
            // Temporarily remove the action listener
            ActionListener[] listeners = presetCombo.getActionListeners();
            for (ActionListener listener : listeners) {
                presetCombo.removeActionListener(listener);
            }
            
            // Select the first preset by default
            if (presetCombo.getItemCount() > 0) {
                presetCombo.setSelectedIndex(0);
                
                // Explicitly set the program change
                if (presetCombo.getSelectedItem() instanceof PresetItem item) {
                    setProgramChange(bank, item.getNumber());
                }
            }
            
            // Restore the action listeners
            for (ActionListener listener : listeners) {
                presetCombo.addActionListener(listener);
            }
        } catch (Exception e) {
            logger.error("Error populating presets: {}", e.getMessage());
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
                logger.info("Loading soundbank file: {}", soundbankFile.getAbsolutePath());
                
                // Use the manager to load the soundbank
                Soundbank soundbank = InternalSynthManager.getInstance().loadSoundbankFile(soundbankFile);
                
                if (soundbank != null) {
                    // Update UI with the new soundbank list
                    List<String> names = InternalSynthManager.getInstance().getSoundbankNames();
                    soundbankCombo.removeAllItems();
                    for (String name : names) {
                        soundbankCombo.addItem(name);
                    }
                    
                    // Select the newly added soundbank
                    soundbankCombo.setSelectedIndex(soundbankCombo.getItemCount() - 1);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading soundbank: {}", e.getMessage());
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
                // Handle program changes directly since InternalSynthManager no longer has sendProgramChange
                MidiChannel channel = synthesizer.getChannels()[midiChannel];
                if (channel != null) {
                    // Send bank select MSB (CC 0)
                    channel.controlChange(0, 0);
                    
                    // Send bank select LSB (CC 32)
                    channel.controlChange(32, bank);
                    
                    // Send program change
                    channel.programChange(program);
                    
                    logger.info("Sent program change: channel={}, bank={}, program={}", 
                            midiChannel, bank, program);
                }
                
                // Reset controls and controllers after the change
                resetControlsToDefault();
                reinitializeControllers();
            } catch (Exception e) {
                logger.error("Error changing program: {}", e.getMessage());
            }
        }
    }

    /**
     * Reset UI controls to default values after preset change
     */
    private void resetControlsToDefault() {
        System.out.println("Resetting UI controls to default values...");
        
        // Reset all oscillator panels
        for (InternalSynthOscillatorPanel panel : oscillatorPanels) {
            panel.resetToDefaults();
        }
        
        // Reset all specialized panels
        if (lfoPanel != null) lfoPanel.resetToDefaults();
        if (filterPanel != null) filterPanel.resetToDefaults();
        if (envelopePanel != null) envelopePanel.resetToDefaults();
        if (effectsPanel != null) effectsPanel.resetToDefaults();
        if (mixerPanel != null) mixerPanel.resetToDefaults();
        
        // Reset all sliders (except those in specialized panels)
        UIHelper.findComponentsByType(this, JSlider.class, component -> {
            JSlider slider = (JSlider) component;
            // Skip slider if it's owned by one of the specialized panels
            if (slider.getParent() != null && 
                    (UIHelper.isChildOf(slider, lfoPanel) || 
                     UIHelper.isChildOf(slider, filterPanel) ||
                     UIHelper.isChildOf(slider, envelopePanel) ||
                     UIHelper.isChildOf(slider, effectsPanel))) {
                return;
            }
            
            int defaultValue = (slider.getMaximum() - slider.getMinimum()) / 2 + slider.getMinimum();
            slider.setValue(defaultValue);
            System.out.println("Reset slider to " + defaultValue);
        });
        
        // Reset all dials to appropriate values (using the utility method)
        UIHelper.findComponentsByType(this, Dial.class, component -> {
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

        // ROW 1: Oscillator panels in horizontal layout
        JPanel oscillatorsPanel = new JPanel();
        oscillatorsPanel.setLayout(new BoxLayout(oscillatorsPanel, BoxLayout.X_AXIS));
        oscillatorsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create three oscillator panels
        InternalSynthOscillatorPanel osc1Panel = new InternalSynthOscillatorPanel(synthesizer, midiChannel, 0);
        InternalSynthOscillatorPanel osc2Panel = new InternalSynthOscillatorPanel(synthesizer, midiChannel, 1);
        InternalSynthOscillatorPanel osc3Panel = new InternalSynthOscillatorPanel(synthesizer, midiChannel, 2);
        
        // Store references to the oscillator panels
        oscillatorPanels = new InternalSynthOscillatorPanel[] { osc1Panel, osc2Panel, osc3Panel };
        
        // Add property change listener for osc1 waveform changes
        osc1Panel.addPropertyChangeListener("oscillator1WaveformChanged", evt -> {
            // Handle program change if needed when oscillator 1's waveform changes
            if (presetCombo.getSelectedItem() instanceof PresetItem && synthesizer != null) {
                PresetItem item = (PresetItem) presetCombo.getSelectedItem();
                MidiChannel channel = synthesizer.getChannels()[midiChannel];
                if (channel != null) {
                    channel.programChange(item.getNumber());
                }
            }
        });

        // Add oscillator panels to row 1
        oscillatorsPanel.add(osc1Panel);
        oscillatorsPanel.add(Box.createHorizontalStrut(10));
        oscillatorsPanel.add(osc2Panel);
        oscillatorsPanel.add(Box.createHorizontalStrut(10));
        oscillatorsPanel.add(osc3Panel);

        // Add first row to main panel
        mainPanel.add(oscillatorsPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        
        // ROW 2: Mixer panel (left-aligned)
        JPanel mixerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        mixerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Create mixer panel
        mixerPanel = new InternalSynthMixerPanel(synthesizer, midiChannel);
        mixerRow.add(mixerPanel);
        
        // Add second row to main panel
        mainPanel.add(mixerRow);
        mainPanel.add(Box.createVerticalStrut(15));
        
        // ROW 3: Envelope, Filter, LFO, and Effects panels
        JPanel bottomRow = new JPanel(new GridLayout(1, 4, 10, 0));
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Create all bottom row panels
        envelopePanel = new InternalSynthEnvelopePanel(synthesizer, midiChannel);
        filterPanel = new InternalSynthFilterPanel(synthesizer, midiChannel);
        lfoPanel = new InternalSynthLFOPanel(synthesizer, midiChannel);
        effectsPanel = new InternalSynthEffectsPanel(synthesizer, midiChannel);
        
        // Add panels to the bottom row
        bottomRow.add(envelopePanel);
        bottomRow.add(filterPanel);
        bottomRow.add(lfoPanel);
        bottomRow.add(effectsPanel);
        
        // Add third row to main panel
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
                UIHelper.findComponentsByType(this, JCheckBox.class, component -> {
                    JCheckBox checkBox = (JCheckBox) component;
                    if (checkBox.getName() != null && checkBox.getName().contains("Toggle")) {
                        int oscIndex = Integer.parseInt(checkBox.getName().substring(3, 4));
                        int baseCCForOsc = oscIndex * 20 + 20;
                        boolean enabled = checkBox.isSelected();

                        // Find associated volume dial
                        UIHelper.findComponentsByType(this, Dial.class, dial -> {
                            if (dial.getName() != null && dial.getName().equals("volumeDial" + oscIndex)) {
                                int volume = enabled ? ((Dial) dial).getValue() : 0;
                                setControlChange(midiChannel, baseCCForOsc + 4, volume);
                            }
                        });
                    }
                });

                // Update all oscillator panels to send their control changes
                for (InternalSynthOscillatorPanel panel : oscillatorPanels) {
                    panel.updateSynthState();
                }
                
                // Update LFO panel state
                if (lfoPanel != null) {
                    lfoPanel.updateSynthState();
                }

                // Update filter panel state
                if (filterPanel != null) {
                    filterPanel.updateSynthState();
                }

                // Update envelope panel state
                if (envelopePanel != null) {
                    envelopePanel.updateSynthState();
                }

                // Update effects panel state
                if (effectsPanel != null) {
                    effectsPanel.updateSynthState();
                }

                // Update mixer panel state
                if (mixerPanel != null) {
                    mixerPanel.updateSynthState();
                }

                logger.info("Reinitialized controllers after preset change");
            } catch (Exception e) {
                logger.error("Error reinitializing controllers: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Add a method to play test notes using the InternalSynthManager
     */
    public void playTestNote(int note, int velocity, int preset) {
        if (synthesizer != null && synthesizer.isOpen()) {
            // Use the manager but pass the synthesizer and soundbank name explicitly
            InternalSynthManager.getInstance().playTestNote(
                synthesizer, midiChannel, note, velocity, preset, currentSoundbankName);
        }
    }
}