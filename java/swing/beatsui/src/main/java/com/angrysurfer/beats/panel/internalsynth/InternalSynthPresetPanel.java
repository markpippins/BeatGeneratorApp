package com.angrysurfer.beats.panel.internalsynth;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.preset.DrumItem;
import com.angrysurfer.core.model.preset.PresetItem;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.SoundbankManager;
import com.angrysurfer.core.service.UserConfigManager;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A panel containing basic player properties controls including
 * soundbank/preset support for internal synths
 */

@Getter
@Setter
public class InternalSynthPresetPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(InternalSynthPresetPanel.class);

    // Player reference
    private Player player;

    // Basic components
    private JTextField nameField;
    private JComboBox<InstrumentWrapper> instrumentCombo;
    private JSpinner channelSpinner;
    private JPanel presetControlPanel;

    // Preset controls - different for internal/external synths and drum/melodic channels
    private JSpinner presetSpinner;
    private JComboBox<Object> presetCombo;

    // Soundbank controls for internal synths
    private JPanel soundbankPanel;
    private JComboBox<String> soundbankCombo;
    private JComboBox<Integer> bankCombo;
    private JButton loadSoundbankButton;
    private JButton previewButton;

    // State tracking
    private boolean usingInternalSynth = false;
    private boolean isDrumChannel = false;
    private boolean initializing = false;

    /**
     * Create a new basic properties panel for a player
     *
     * @param player The player to edit
     */
    public InternalSynthPresetPanel(Player player) {
        super(new GridBagLayout());
        this.player = player;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Basic Properties"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        setupComponents();
        layoutComponents();

        // Register for instrument updates
        registerForInstrumentUpdates();

        // Initialize control states
        updatePresetControls();
    }

    /**
     * Initialize all UI components
     */
    private void setupComponents() {
        // Basic properties
        nameField = new JTextField(player.getName());

        // Channel spinner
        channelSpinner = new JSpinner(new SpinnerNumberModel(
                (long) player.getChannel(), 0, 15, 1));

        // Preset spinner for external devices
        presetSpinner = new JSpinner(new SpinnerNumberModel(
                player.getPreset() != null ? player.getPreset() : 0, 0, 127, 1));

        // Preset combo for internal synths
        presetCombo = new JComboBox<>();

        // Preset control container with CardLayout to switch between spinner and combo
        presetControlPanel = new JPanel(new CardLayout());
        presetControlPanel.add(presetSpinner, "spinner");
        presetControlPanel.add(presetCombo, "combo");

        // Soundbank controls
        soundbankCombo = new JComboBox<>();
        bankCombo = new JComboBox<>();
        loadSoundbankButton = new JButton("Load...");
        previewButton = new JButton("Preview");

        setupInstrumentCombo();
        setupSoundbankPanel();

        // Add listeners
        channelSpinner.addChangeListener(e -> {
            int channelValue = ((Number) channelSpinner.getValue()).intValue();
            boolean newIsDrumChannel = (channelValue == SequencerConstants.MIDI_DRUM_CHANNEL);

            // If drum channel status changed, update the UI
            if (newIsDrumChannel != isDrumChannel) {
                isDrumChannel = newIsDrumChannel;
                updatePresetControls();
            }

            player.setDefaultChannel(channelValue);
        });

        // Setup action listeners for soundbank controls
        soundbankCombo.addActionListener(e -> {
            if (!initializing && soundbankCombo.getSelectedItem() != null) {
                populateBanksCombo();
            }
        });

        bankCombo.addActionListener(e -> {
            if (!initializing && bankCombo.getSelectedItem() instanceof Integer) {
                populatePresetComboForBank((Integer) bankCombo.getSelectedItem());
            }
        });

        loadSoundbankButton.addActionListener(e -> loadSoundbankFile());
        previewButton.addActionListener(e -> playPreviewNote());
    }

    /**
     * Set up the instrument combo box
     */
    private void setupInstrumentCombo() {
        instrumentCombo = new JComboBox<>();

        // Add custom renderer to display instrument names
        instrumentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof InstrumentWrapper instrument) {
                    setText(instrument.getName());
                }
                return this;
            }
        });

        // Get instruments from UserConfigManager
        List<InstrumentWrapper> instruments = UserConfigManager.getInstance().getInstruments();

        // Add internal synths
        instruments.addAll(getInternalSynths());

        if (instruments == null || instruments.isEmpty()) {
            logger.error("No instruments found");
            // Add a default instrument to prevent null selections
            InstrumentWrapper defaultInstrument = new InstrumentWrapper();
            defaultInstrument.setId(0L);
            defaultInstrument.setName("Default Instrument");
            instrumentCombo.addItem(defaultInstrument);
        } else {
            // Sort instruments by name
            instruments.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

            for (InstrumentWrapper inst : instruments) {
                if (inst.getAvailable() && Objects.nonNull(inst.getDevice())) {
                    instrumentCombo.addItem(inst);
                }
            }
        }

        // Select the player's instrument if it exists
        if (player.getInstrument() != null) {
            for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                InstrumentWrapper item = instrumentCombo.getItemAt(i);
                if (item.getId().equals(player.getInstrument().getId())) {
                    instrumentCombo.setSelectedIndex(i);

                    // Check if it's an internal synth
                    usingInternalSynth = InternalSynthManager.getInstance().isInternalSynth(item);
                    isDrumChannel = (player.getChannel() == 9);
                    break;
                }
            }
        }

        // Add listener to update preset controls when instrument changes
        instrumentCombo.addActionListener(e -> {
            if (e.getActionCommand().equals("comboBoxChanged")) {
                InstrumentWrapper selectedInstrument = (InstrumentWrapper) instrumentCombo.getSelectedItem();
                if (selectedInstrument != null) {
                    boolean isInternal = InternalSynthManager.getInstance().isInternalSynth(selectedInstrument);
                    if (isInternal != usingInternalSynth) {
                        usingInternalSynth = isInternal;
                        updatePresetControls();
                    }

                    // Update soundbank panel visibility
                    soundbankPanel.setVisible(isInternal);

                    // Update player's instrument
                    player.setInstrument(selectedInstrument);
                    player.setInstrumentId(selectedInstrument.getId());

                    // If internal synth, initialize soundbanks
                    if (isInternal) {
                        initializeSoundbanks();
                    }
                }
            }
        });
    }

    /**
     * Get all available internal synthesizers in the system
     *
     * @return A list of InstrumentWrapper objects that are internal synthesizers
     */
    public List<InstrumentWrapper> getInternalSynths() {
        List<InstrumentWrapper> internalSynths = new ArrayList<>();

        try {
            // Get all MIDI device info
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

            for (MidiDevice.Info info : infos) {
                try {
                    // Try to get the device
                    MidiDevice device = MidiSystem.getMidiDevice(info);

                    // Check if it's a synthesizer
                    if (device instanceof Synthesizer) {
                        // Create an instrument wrapper for this synth
                        InstrumentWrapper wrapper = new InstrumentWrapper();
                        wrapper.setId(System.currentTimeMillis()); // Unique ID
                        wrapper.setName(info.getName());
                        wrapper.setDeviceName(info.getName());
                        wrapper.setDevice(device);
                        wrapper.setDescription(info.getDescription());
                        wrapper.setInternalSynth(true);

                        // Add to the list
                        internalSynths.add(wrapper);

                        logger.debug("Found internal synthesizer: {}", info.getName());
                    }
                } catch (Exception e) {
                    logger.warn("Error checking device {}: {}", info.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error getting internal synthesizers: {}", e.getMessage(), e);
        }

        return internalSynths;
    }

    /**
     * Set up the soundbank panel
     */
    private void setupSoundbankPanel() {
        soundbankPanel = new JPanel(new BorderLayout(5, 5));
        soundbankPanel.setBorder(BorderFactory.createTitledBorder("Soundbank"));

        // Create a panel for soundbank selection with load button
        JPanel soundbankSelectionPanel = new JPanel(new BorderLayout(5, 0));
        soundbankSelectionPanel.add(soundbankCombo, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(loadSoundbankButton);
        soundbankSelectionPanel.add(buttonPanel, BorderLayout.EAST);

        // Create a panel for bank selection
        JPanel bankPanel = new JPanel(new BorderLayout(5, 0));
        bankPanel.add(new JLabel("Bank:"), BorderLayout.WEST);
        bankPanel.add(bankCombo, BorderLayout.CENTER);

        // Create a panel for preview button
        JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        previewPanel.add(previewButton);

        // Add components to soundbank panel
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        controlsPanel.add(new JLabel("Soundbank:"), gbc);

        gbc.gridy = 1;
        controlsPanel.add(soundbankSelectionPanel, gbc);

        gbc.gridy = 2;
        controlsPanel.add(bankPanel, gbc);

        soundbankPanel.add(controlsPanel, BorderLayout.CENTER);
        soundbankPanel.add(previewPanel, BorderLayout.SOUTH);

        // Only show for internal synths
        soundbankPanel.setVisible(usingInternalSynth);
    }

    /**
     * Layout all components using GridBagLayout
     */
    private void layoutComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // First row
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        add(nameField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        add(new JLabel("Instrument:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1.0;
        add(instrumentCombo, gbc);

        // Second row
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(new JLabel("Channel:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        add(channelSpinner, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        add(new JLabel("Preset:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 1.0;
        add(presetControlPanel, gbc);

        // Third row - Soundbank panel (spans all columns)
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(soundbankPanel, gbc);
    }

    /**
     * Update preset control UI based on instrument type and channel
     */
    private void updatePresetControls() {
        CardLayout cl = (CardLayout) presetControlPanel.getLayout();

        if (usingInternalSynth) {
            // Check if we're on the drum channel
            if (isDrumChannel) {
                // Populate with drum names
                populateDrumCombo();
            } else {
                // For melodic channels, populate with presets
                populatePresetCombo();
            }
            cl.show(presetControlPanel, "combo");

            // Show soundbank panel
            soundbankPanel.setVisible(true);

            // Initialize soundbanks
            initializeSoundbanks();
        } else {
            // For external instruments, use the spinner
            presetSpinner.setValue(player.getPreset() != null ? player.getPreset() : 0);
            cl.show(presetControlPanel, "spinner");

            // Hide soundbank panel
            soundbankPanel.setVisible(false);
        }

        // Request re-layout
        revalidate();
        repaint();
    }

    /**
     * Initialize soundbanks for internal synths
     */
    private void initializeSoundbanks() {
        try {
            initializing = true;

            // Initialize soundbanks using InternalSynthManager
            InternalSynthManager.getInstance().loadDefaultSoundbank();

            // Get soundbank names
            List<String> names = InternalSynthManager.getInstance().getSoundbankNames();

            // Remember selected soundbank
            String selectedSoundbankName = null;
            if (soundbankCombo.getSelectedItem() != null) {
                selectedSoundbankName = (String) soundbankCombo.getSelectedItem();
            }

            // Clear and populate soundbank combo
            soundbankCombo.removeAllItems();
            for (String name : names)
                soundbankCombo.addItem(name);

            // Try to restore selection or use player's stored soundbank
            boolean restored = false;

            // First try player's instrument settings
            InstrumentWrapper instrument = player.getInstrument();
            if (instrument != null && instrument.getSoundbankName() != null) {
                String storedName = instrument.getSoundbankName();
                for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                    if (soundbankCombo.getItemAt(i).equals(storedName)) {
                        soundbankCombo.setSelectedIndex(i);
                        restored = true;
                        break;
                    }
                }
            }

            // Then try to restore previous selection
            if (!restored && selectedSoundbankName != null) {
                for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                    if (soundbankCombo.getItemAt(i).equals(selectedSoundbankName)) {
                        soundbankCombo.setSelectedIndex(i);
                        restored = true;
                        break;
                    }
                }
            }

            // Default to first item if nothing restored
            if (!restored && soundbankCombo.getItemCount() > 0) {
                soundbankCombo.setSelectedIndex(0);
            }

            // Populate banks for selected soundbank
            populateBanksCombo();

            initializing = false;
        } catch (Exception e) {
            logger.error("Error initializing soundbanks: {}", e.getMessage());
            initializing = false;
        }
    }

    /**
     * Populate bank combo box based on selected soundbank
     */
    private void populateBanksCombo() {
        try {
            initializing = true;

            // Get selected soundbank
            String selectedSoundbank = null;
            if (soundbankCombo.getSelectedItem() != null) {
                selectedSoundbank = (String) soundbankCombo.getSelectedItem();
            }

            if (selectedSoundbank == null) {
                bankCombo.removeAllItems();
                initializing = false;
                return;
            }

            // Remember selected bank
            Integer selectedBank = null;
            if (bankCombo.getSelectedItem() instanceof Integer) {
                selectedBank = (Integer) bankCombo.getSelectedItem();
            }

            // Get banks for this soundbank
            List<Integer> banks = InternalSynthManager.getInstance().getAvailableBanksByName(selectedSoundbank);

            // Update bank combo
            bankCombo.removeAllItems();
            for (Integer bank : banks) {
                bankCombo.addItem(bank);
            }

            // Restore selection or use player's stored bank
            boolean restored = false;

            // First try player's instrument settings
            InstrumentWrapper instrument = player.getInstrument();
            if (instrument != null && instrument.getBankIndex() != null) {
                Integer storedBank = instrument.getBankIndex();
                for (int i = 0; i < bankCombo.getItemCount(); i++) {
                    if (bankCombo.getItemAt(i).equals(storedBank)) {
                        bankCombo.setSelectedIndex(i);
                        restored = true;
                        break;
                    }
                }
            }

            // Then try previous selection
            if (!restored && selectedBank != null) {
                for (int i = 0; i < bankCombo.getItemCount(); i++) {
                    if (bankCombo.getItemAt(i).equals(selectedBank)) {
                        bankCombo.setSelectedIndex(i);
                        restored = true;
                        break;
                    }
                }
            }

            // Default to first item
            if (!restored && bankCombo.getItemCount() > 0) {
                bankCombo.setSelectedIndex(0);
            }

            // Populate presets for the selected bank
            if (bankCombo.getSelectedItem() instanceof Integer) {
                populatePresetComboForBank((Integer) bankCombo.getSelectedItem());
            }

            initializing = false;
        } catch (Exception e) {
            logger.error("Error populating banks: {}", e.getMessage());
            initializing = false;
        }
    }

    /**
     * Populate preset combo box for melodic instruments
     */
    private void populatePresetCombo() {
        try {
            InstrumentWrapper selectedInstrument = (InstrumentWrapper) instrumentCombo.getSelectedItem();
            if (selectedInstrument == null) {
                return;
            }

            // Remember current preset
            long currentPreset = player.getPreset() != null ? player.getPreset() : 0;

            // Clear the combo
            presetCombo.removeAllItems();

            // Get preset names
            List<String> presetNames;

            if (usingInternalSynth && bankCombo.getSelectedItem() instanceof Integer) {
                // For internal synths, get preset names for the selected soundbank and bank
                String soundbankName = (String) soundbankCombo.getSelectedItem();
                int bank = (Integer) bankCombo.getSelectedItem();
                presetNames = InternalSynthManager.getInstance().getPresetNames(soundbankName, bank);
            } else {
                // For other instruments, get preset names for the instrument
                Long instrumentId = selectedInstrument.getId(); // getId() already returns Long
                presetNames = SoundbankManager.getInstance().getPresetNames(instrumentId);
            }

            // If no presets found, use generic names
            if (presetNames.isEmpty() || presetNames.size() < 128) {
                int startIdx = presetNames.size();
                for (int i = startIdx; i < 128; i++) {
                    if (i >= presetNames.size()) {
                        presetNames.add("Program " + i);
                    } else if (presetNames.get(i) == null || presetNames.get(i).isEmpty()) {
                        presetNames.set(i, "Program " + i);
                    }
                }
            }

            // Add all presets to combo
            for (int i = 0; i < Math.min(128, presetNames.size()); i++) {
                presetCombo.addItem(new PresetItem(i, i + ": " + presetNames.get(i)));
            }

            // Select the current preset
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                PresetItem item = (PresetItem) presetCombo.getItemAt(i);
                if (item.getNumber() == currentPreset) {
                    presetCombo.setSelectedIndex(i);
                    break;
                }
            }

            // If nothing selected, select first item
            if (presetCombo.getSelectedIndex() < 0 && presetCombo.getItemCount() > 0) {
                presetCombo.setSelectedIndex(0);
            }
        } catch (Exception e) {
            logger.error("Error populating presets: {}", e.getMessage());
        }
    }

    /**
     * Populate preset combo for a specific bank
     */
    private void populatePresetComboForBank(int bank) {
        if (isDrumChannel) {
            // For drum channel, populate drum names
            populateDrumCombo();
            return;
        }

        try {
            // Remember current selection
            Object currentSelection = presetCombo.getSelectedItem();
            int currentPresetNumber = -1;
            if (currentSelection instanceof PresetItem) {
                currentPresetNumber = ((PresetItem) currentSelection).getNumber();
            } else if (player.getPreset() != null) {
                currentPresetNumber = player.getPreset().intValue();
            }

            // Clear combo
            presetCombo.removeAllItems();

            // Get preset names for this soundbank and bank
            String soundbankName = null;
            if (soundbankCombo.getSelectedItem() != null) {
                soundbankName = (String) soundbankCombo.getSelectedItem();
            }

            // Initialize presetNames with empty list first to avoid "might not have been initialized" error
            List<String> presetNames = new ArrayList<>();

            if (soundbankName != null) {
                presetNames = InternalSynthManager.getInstance().getPresetNames(soundbankName, bank);
            }
            // Ensure we have a valid list even if the above call returned null
            if (presetNames == null) {
                presetNames = new ArrayList<>();
            }

            // Add presets to combo
            for (int i = 0; i < Math.min(128, presetNames.size()); i++) {
                String name = presetNames.get(i);
                if (name == null || name.isEmpty()) {
                    name = "Program " + i;
                }
                presetCombo.addItem(new PresetItem(i, i + ": " + name));
            }

            // If no presets were added, add default ones
            if (presetCombo.getItemCount() == 0) {
                for (int i = 0; i < 128; i++) {
                    presetCombo.addItem(new PresetItem(i, "Program " + i));
                }
            }

            // Try to select previous preset
            boolean found = false;
            if (currentPresetNumber >= 0) {
                for (int i = 0; i < presetCombo.getItemCount(); i++) {
                    if (presetCombo.getItemAt(i) instanceof PresetItem &&
                            ((PresetItem) presetCombo.getItemAt(i)).getNumber() == currentPresetNumber) {
                        presetCombo.setSelectedIndex(i);
                        found = true;
                        break;
                    }
                }
            }

            // Default to first item
            if (!found && presetCombo.getItemCount() > 0) {
                presetCombo.setSelectedIndex(0);
            }

            // Update player's instrument settings
            updateInstrumentPreset();

        } catch (Exception e) {
            logger.error("Error populating presets for bank {}: {}", bank, e.getMessage());
        }
    }

    /**
     * Populate drum combo for channel 9
     */
    private void populateDrumCombo() {
        try {
            // Remember current selection
            int currentNote = -1;
            if (presetCombo.getSelectedItem() instanceof DrumItem) {
                currentNote = ((DrumItem) presetCombo.getSelectedItem()).getNoteNumber();
            } else if (player.getRootNote() != null) {
                currentNote = player.getRootNote().intValue();
            } else {
                currentNote = 36; // Default to bass drum
            }

            // Clear combo
            presetCombo.removeAllItems();

            // Add standard drum kit sounds (GM drum map)
            for (int i = 35; i <= 81; i++) {
                String drumName = InternalSynthManager.getInstance().getDrumName(i);
                presetCombo.addItem(new DrumItem(i, i + ": " + drumName));
            }

            // Try to select the current note
            boolean found = false;
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                DrumItem item = (DrumItem) presetCombo.getItemAt(i);
                if (item.getNoteNumber() == currentNote) {
                    presetCombo.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }

            // Default to bass drum (36)
            if (!found) {
                for (int i = 0; i < presetCombo.getItemCount(); i++) {
                    DrumItem item = (DrumItem) presetCombo.getItemAt(i);
                    if (item.getNoteNumber() == 36) {
                        presetCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error populating drum combo: {}", e.getMessage());
        }
    }

    /**
     * Play a preview note with current settings
     */
    private void playPreviewNote() {
        try {
            // Get current settings
            int channel = ((Number) channelSpinner.getValue()).intValue();
            int bank = bankCombo.getSelectedItem() != null ? (Integer) bankCombo.getSelectedItem() : 0;
            int[] notes; // Array of notes for chord support
            int program = 0;

            // Set up note array based on channel type
            if (channel == 9) { // Drum channel
                if (presetCombo.getSelectedItem() instanceof DrumItem) {
                    notes = new int[]{((DrumItem) presetCombo.getSelectedItem()).getNoteNumber()};
                } else {
                    notes = new int[]{36}; // Default to bass drum
                }
                program = 0; // Standard drum kit
            } else {
                // For melodic instruments, play a chord
                notes = new int[]{60, 64, 67}; // C major chord

                // Get program number from UI
                if (presetCombo.getSelectedItem() instanceof PresetItem) {
                    program = ((PresetItem) presetCombo.getSelectedItem()).getNumber();
                }
            }

            // Get the instrument
            InstrumentWrapper instrument = player.getInstrument();
            if (instrument == null || instrument.getDevice() == null) {
                logger.error("Cannot play preview - no instrument or device available");
                return;
            }

            // Make sure instrument settings are up to date
            updateInstrumentPreset();

            // Make sure device is open
            if (!instrument.getDevice().isOpen()) {
                instrument.getDevice().open();
            }

            // Try direct device access if it's a synthesizer
            if (instrument.getDevice() instanceof Synthesizer synth) {
                MidiChannel[] channels = synth.getChannels();

                if (channels != null && channel < channels.length) {
                    // Apply bank and program change
                    channels[channel].controlChange(0, 0);  // Bank MSB
                    channels[channel].controlChange(32, bank); // Bank LSB
                    channels[channel].programChange(program);

                    // Play notes
                    for (int note : notes) {
                        channels[channel].noteOn(note, 100);
                    }

                    // Schedule note off
                    final MidiChannel mc = channels[channel];
                    final int[] finalNotes = notes;
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            for (int note : finalNotes) {
                                mc.noteOff(note);
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }).start();

                    return; // Success
                }
            }

            // If direct access failed, use the instrument API
            instrument.controlChange(0, 0);  // Bank MSB
            instrument.controlChange(32, bank); // Bank LSB
            instrument.programChange(program, 0);

            // Play notes
            for (int note : notes) {
                instrument.noteOn(note, 100);
            }

            // Schedule note off
            final int[] finalNotes = notes;
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    for (int note : finalNotes) {
                        instrument.noteOff(note, 0);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }).start();

        } catch (Exception e) {
            logger.error("Error playing preview: {}", e.getMessage());
        }
    }

    /**
     * Load a soundbank file
     */
    private void loadSoundbankFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Soundbank");
        chooser.setFileFilter(new FileNameExtensionFilter("Soundbank Files (*.sf2, *.dls)", "sf2", "dls"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (file == null || !file.exists()) {
            return;
        }

        try {
            // Load the soundbank - notice this returns a String, not a Soundbank
            String soundbankName = SoundbankManager.getInstance().loadSoundbankFile(file);
            if (soundbankName == null) {
                JOptionPane.showMessageDialog(this,
                        "Failed to load soundbank file",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            logger.info("Loaded soundbank: {}", soundbankName);

            // Update UI with the new soundbank list
            List<String> names = SoundbankManager.getInstance().getSoundbankNames();
            soundbankCombo.removeAllItems();
            for (String name : names) {
                soundbankCombo.addItem(name);
            }

            // Select the newly loaded soundbank
            for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                if (soundbankCombo.getItemAt(i).equals(soundbankName)) {
                    soundbankCombo.setSelectedIndex(i);
                    break;
                }
            }

            // Update instrument with new soundbank
            InstrumentWrapper instrument = player.getInstrument();
            if (instrument != null) {
                instrument.setSoundbankName(soundbankName);

                // Get the actual Soundbank object if needed for direct operations
                Soundbank soundbank = SoundbankManager.getInstance().getSoundbankByName(soundbankName);

                // Load soundbank into the instrument if it's a Synthesizer and we have the actual Soundbank
                if (instrument.getDevice() instanceof Synthesizer synth && soundbank != null) {

                    if (!synth.isOpen()) {
                        synth.open();
                    }

                    // Unload default soundbank first
                    synth.unloadAllInstruments(synth.getDefaultSoundbank());

                    // Try to load all instruments
                    boolean loaded = synth.loadAllInstruments(soundbank);

                    // Rest of the method remains the same...
                }
            }

            // Update banks combo for the new soundbank
            populateBanksCombo();

            // Play a preview note to demonstrate the new soundbank
            SwingUtilities.invokeLater(this::playPreviewNote);

            // Update the PlayerTable display
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, player);

        } catch (Exception e) {
            logger.error("Error loading soundbank: {}", e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error loading soundbank: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Update the player's instrument with current UI selections
     */
    private void updateInstrumentPreset() {
        InstrumentWrapper instrument = player.getInstrument();
        if (instrument == null) {
            return;
        }

        try {
            int channel = ((Number) channelSpinner.getValue()).intValue();

            // Update channel
            player.setDefaultChannel(channel);

            // Update soundbank settings for internal synths
            if (usingInternalSynth) {
                // Update soundbank name
                if (soundbankCombo.getSelectedItem() != null) {
                    String soundbankName = (String) soundbankCombo.getSelectedItem();
                    instrument.setSoundbankName(soundbankName);
                }

                // Update bank
                if (bankCombo.getSelectedItem() != null) {
                    Integer bank = (Integer) bankCombo.getSelectedItem();
                    instrument.setBankIndex(bank);
                }

                // Update preset/program based on channel type
                if (isDrumChannel) {
                    // For drums, store the note number in rootNote
                    if (presetCombo.getSelectedItem() instanceof DrumItem) {
                        int noteNumber = ((DrumItem) presetCombo.getSelectedItem()).getNoteNumber();
                        player.setRootNote(noteNumber);
                        instrument.setPreset(0); // Standard GM drum kit
                    }
                } else {
                    // For melodic instruments
                    if (presetCombo.getSelectedItem() instanceof PresetItem) {
                        int preset = ((PresetItem) presetCombo.getSelectedItem()).getNumber();
                        instrument.setPreset(preset);
                        player.getInstrument().setPreset(preset);
                    }
                }

                // Apply changes to the physical MIDI device
                if (instrument.getBankIndex() != null && instrument.getPreset() != null) {
                    instrument.applyBankAndProgram();
                }
            } else {
                // For external instruments, use spinner value
                int preset = ((Number) presetSpinner.getValue()).intValue();
                player.getInstrument().setPreset(preset);
            }

            // Notify that the player has been updated
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, player);

        } catch (Exception e) {
            logger.error("Error updating instrument preset: {}", e.getMessage());
        }
    }

    /**
     * Get the name entered in the field
     */
    public String getPlayerName() {
        return nameField.getText();
    }

    /**
     * Get the selected instrument
     */
    public InstrumentWrapper getSelectedInstrument() {
        return (InstrumentWrapper) instrumentCombo.getSelectedItem();
    }

    /**
     * Get the selected channel
     */
    public int getSelectedChannel() {
        return ((Number) channelSpinner.getValue()).intValue();
    }

    /**
     * Get the selected preset
     */
    public int getSelectedPreset() {
        if (usingInternalSynth) {
            if (isDrumChannel && presetCombo.getSelectedItem() instanceof DrumItem) {
                return 0; // For drums, preset is always 0 (standard GM kit)
            } else if (presetCombo.getSelectedItem() instanceof PresetItem) {
                return ((PresetItem) presetCombo.getSelectedItem()).getNumber();
            }
        }
        return ((Number) presetSpinner.getValue()).intValue();
    }

    /**
     * Get the selected drum note (for drum channel)
     */
    public int getSelectedDrumNote() {
        if (isDrumChannel && presetCombo.getSelectedItem() instanceof DrumItem) {
            return ((DrumItem) presetCombo.getSelectedItem()).getNoteNumber();
        }
        return 36; // Default to bass drum
    }

    /**
     * Apply current UI settings to the player
     */
    public void applyToPlayer() {
        // Update player model with UI settings
        player.setName(getPlayerName());
        player.setInstrument(getSelectedInstrument());
        player.setDefaultChannel(getSelectedChannel());
        player.getInstrument().setPreset(getSelectedPreset());
        if (isDrumChannel) {
            player.setRootNote(getSelectedDrumNote());
        }
    }

    /**
     * Listen for global instrument updates and refresh the instrument combo
     */
    private void registerForInstrumentUpdates() {
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) {
                    return;
                }

                // Listen for instrument changes
                if (Commands.INSTRUMENT_UPDATED.equals(action.getCommand())
                        || Commands.USER_CONFIG_LOADED.equals(action.getCommand())) {

                    // Refresh the instrument combo
                    SwingUtilities.invokeLater(() -> {
                        // Remember selected instrument
                        InstrumentWrapper selected = (InstrumentWrapper) instrumentCombo.getSelectedItem();

                        // Update combo with fresh instruments
                        instrumentCombo.removeAllItems();

                        // Get instruments from UserConfigManager
                        List<InstrumentWrapper> instruments = UserConfigManager.getInstance().getInstruments();

                        // Add internal synths
                        instruments.addAll(InternalSynthManager.getInstance().getInternalSynths());

                        if (instruments != null && !instruments.isEmpty()) {
                            instruments.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

                            for (InstrumentWrapper inst : instruments) {
                                if (inst.getAvailable() && Objects.nonNull(inst.getDevice())) {
                                    instrumentCombo.addItem(inst);
                                }
                            }

                            // Restore selection if possible
                            if (selected != null) {
                                for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                                    InstrumentWrapper item = instrumentCombo.getItemAt(i);
                                    if (item.getId().equals(selected.getId())) {
                                        instrumentCombo.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }
}
