//package com.angrysurfer.core.service;
//
//import com.angrysurfer.core.model.InstrumentInfo;
//import com.angrysurfer.core.model.InstrumentWrapper;
//import com.angrysurfer.core.model.Player;
//import com.angrysurfer.core.model.preset.DrumItem;
//import com.angrysurfer.core.model.preset.SynthData;
//import com.angrysurfer.core.sequencer.SequencerConstants;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.sound.midi.*;
//import java.io.File;
//import java.util.*;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
/// **
// * Manager for internal synthesizer instruments and presets. This singleton
// * provides access to internal synthesizers and their preset information.
// */
//public class InternalSynthManager {
//
//    private static final Logger logger = LoggerFactory.getLogger(InternalSynthManager.class);
//    private static final int DEFAULT_SYNTH_ID = 0; // Added for default/reference synthesizer
//    private static InternalSynthManager instance;
//    // Map of synth IDs to Synthesizer instances
//    private final Map<Integer, Synthesizer> synthesizers = new HashMap<>();
//    // Map of synth IDs to their cached MidiChannels
//    private final Map<Integer, MidiChannel[]> cachedChannelsMap = new HashMap<>();
//
//    // Map of soundbank names to preset information
//    private final Map<String, SynthData> synthDataMap = new HashMap<>();
//    // Default channel for melodic sounds
//    // Use LinkedHashMap to preserve insertion order
//    private final LinkedHashMap<String, Soundbank> soundbanks = new LinkedHashMap<>();
//    private final ScheduledExecutorService noteOffScheduler = Executors.newScheduledThreadPool(2);
//    // Map to store available banks for each soundbank (by name)
//    private final Map<String, List<Integer>> availableBanksMap = new HashMap<>();
//    // Remove single synthesizer instance field: private Synthesizer synthesizer;
//    // Remove single cachedChannels field: private MidiChannel[] cachedChannels;
//
//    /**
//     * Initialize the manager.
//     */
//    private InternalSynthManager() {
//        try {
//            // Synthesizers are now created on demand via getOrCreateSynthesizer(id).
//            // No global initialization here to avoid circular dependencies.
//            logger.info("InternalSynthManager initialized. Synthesizers will be created on demand.");
//        } catch (Exception e) {
//            logger.error("Error initializing InternalSynthManager", e);
//        }
//    }
//
//    /**
//     * Get the singleton instance
//     */
//    public static synchronized InternalSynthManager getInstance() {
//        if (instance == null) {
//            instance = new InternalSynthManager();
//        }
//        return instance;
//    }
//
//    /**
//     * Gets or creates a Synthesizer instance for the given ID.
//     * This is the central method for obtaining a managed synthesizer.
//     *
//     * @param id The ID for the synthesizer (e.g., Sequencer ID).
//     * @return The Synthesizer instance, or null if creation fails.
//     */
//    public synchronized Synthesizer getOrCreateSynthesizer() {
//
//        try {
//            // Try to find Gervill synthesizer first
//            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
//            MidiDevice.Info gervillInfo = null;
//            for (MidiDevice.Info info : infos) {
//                if (info.getName().contains(SequencerConstants.GERVILL)) {
//                    gervillInfo = info;
//                    break;
//                }
//            }
//
//            if (gervillInfo == null)
//                return null;
//
//            Synthesizer synth = MidiSystem.getSynthesizer();
//
//            if (synth != null) {
//                if (!synth.isOpen()) {
//                    synth.open();
//                }
//                if (synth.isOpen()) {
//                    synthesizers.put(id, synth);
//                    cachedChannelsMap.put(id, synth.getChannels());
//                    logger.info("Synthesizer for ID {} initialized: {}", id, synth.getDeviceInfo().getName());
//                    return synth;
//                } else {
//                    logger.error("Failed to open synthesizer for ID {}: {}", id, synth.getDeviceInfo().getName());
//                }
//            } else {
//                logger.error("Could not obtain any synthesizer for ID {}", id);
//            }
//        } catch (Exception e) {
//            logger.error("Error creating or opening synthesizer for ID {}: {}", id, e.getMessage(), e);
//        }
//        return null; // Return null if creation or opening failed
//    }
//
//    /**
//     * Initialize and load available soundbanks
//     * This combines functionality that was split between managers
//     */
//    public boolean initializeSoundbanks() {
//        try {
//            logger.info("Initializing soundbanks...");
//
//            // Clear existing collections first
//            soundbanks.clear();
//            availableBanksMap.clear();
//
//            // Ensure at least the default synthesizer is available for soundbank operations
//            Synthesizer defaultSynth = getOrCreateSynthesizer(DEFAULT_SYNTH_ID);
//            if (defaultSynth == null) {
//                logger.warn("Default synthesizer (ID: {}) could not be initialized. Cannot load soundbanks.", DEFAULT_SYNTH_ID);
//                return false;
//            }
//
//
//            // Add default Java soundbank first
//            Soundbank defaultSoundbank = defaultSynth.getDefaultSoundbank();
//
//            if (defaultSoundbank != null) {
//                String sbName = "Java Internal Soundbank";
//                soundbanks.put(sbName, defaultSoundbank);
//                analyzeAvailableBanks(sbName, defaultSoundbank);
//            } else {
//                logger.warn("No default soundbank found in the default synthesizer (ID: {})", DEFAULT_SYNTH_ID);
//            }
//
//            // Load user soundbanks from the soundbank directory
//            File userDir = getUserSoundbankDirectory(); // Ensure this method exists and works
//            if (userDir.exists() && userDir.isDirectory()) {
//                File[] files = userDir.listFiles((dir, name) ->
//                        name.toLowerCase().endsWith(".sf2") || name.toLowerCase().endsWith(".dls"));
//
//                if (files != null) {
//                    for (File file : files) {
//                        try {
//                            Soundbank sb = MidiSystem.getSoundbank(file);
//                            if (sb != null) {
//                                String name = file.getName();
//                                soundbanks.put(name, sb);
//                                analyzeAvailableBanks(name, sb);
//                                logger.info("Loaded soundbank: {} with {} instruments",
//                                        name, sb.getInstruments().length);
//                            }
//                        } catch (Exception e) {
//                            logger.warn("Failed to load soundbank file {}: {}", file.getName(), e.getMessage());
//                        }
//                    }
//                }
//            }
//
//            logger.info("Initialized {} soundbanks", soundbanks.size());
//            return true;
//        } catch (Exception e) {
//            logger.error("Error initializing soundbanks", e);
//            return false;
//        }
//    }
//
//    /**
//     * Initialize synthesizer data structures
//     */
//    public void initializeSynthData() {
//        try {
//            // We need a reference synthesizer instance to get the default soundbank
//            Synthesizer refSynth = getOrCreateSynthesizer(DEFAULT_SYNTH_ID);
//
//            if (refSynth != null) {
//                Soundbank defaultSoundbank = refSynth.getDefaultSoundbank();
//                if (defaultSoundbank != null) {
//                    String sbName = "Java Internal Soundbank"; // Standard name
//
//                    // Ensure synthDataMap is initialized for this soundbank
//                    SynthData synthData = synthDataMap.computeIfAbsent(sbName, k -> new SynthData(k));
//
//                    for (Instrument instrument : defaultSoundbank.getInstruments()) {
//                        synthData.addInstrument(instrument);
//                    }
//
//                    if (!soundbanks.containsKey(sbName)) {
//                        soundbanks.put(sbName, defaultSoundbank);
//                    }
//
//                    if (!availableBanksMap.containsKey(sbName)) {
//                        analyzeAvailableBanks(sbName, defaultSoundbank);
//                    } else {
//                        List<Integer> currentBanks = synthData.getAvailableBanks();
//                        availableBanksMap.put(sbName, currentBanks);
//                    }
//
//                    logger.info("Initialized/updated synthesizer data for '{}' with {} instruments",
//                            sbName, synthData.getInstruments().size());
//                } else {
//                    logger.warn("No default soundbank available in reference synthesizer (ID: {})", DEFAULT_SYNTH_ID);
//                }
//            } else {
//                logger.warn("No reference synthesizer (ID: {}) available for initializing synth data", DEFAULT_SYNTH_ID);
//            }
//        } catch (Exception e) {
//            logger.error("Error initializing synth data: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Get the internal synthesizer device for a specific ID.
//     *
//     * @param synthesizerId The ID of the synthesizer.
//     * @return The internal synthesizer device.
//     * @throws MidiUnavailableException if the synthesizer is unavailable.
//     */
//    public MidiDevice getInternalSynthDevice(int synthesizerId) throws MidiUnavailableException {
//        Synthesizer synth = getOrCreateSynthesizer(synthesizerId);
//        if (synth == null) {
//            throw new MidiUnavailableException("Synthesizer for ID " + synthesizerId + " could not be initialized.");
//        }
//        return synth;
//    }
//
//    /**
//     * Creates and initializes an instrument for the internal synth on a specific
//     * channel
//     * This is the single entry point for all internal synth instrument creation
//     *
//     * @param synthesizerId The ID of the synthesizer
//     * @param channel       The MIDI channel to use
//     * @param isDrumChannel Whether this is a drum channel (9)
//     * @param name          Optional custom name (will be generated if null)
//     * @return A fully configured InstrumentWrapper for the internal synth
//     */
//    public InstrumentWrapper createInternalInstrument(int synthesizerId, int channel, boolean isDrumChannel, String name) {
//        Synthesizer synthInstance = getOrCreateSynthesizer(synthesizerId);
//        if (synthInstance == null) {
//            logger.error("Failed to get or create synthesizer for ID: {}", synthesizerId);
//            return null;
//        }
//
//        // Generate a name if none provided
//        String instrumentName = name != null ? name : isDrumChannel ? "Internal Drums" : "Internal Synth " + channel;
//
//        try {
//            Receiver receiver = synthInstance.getReceiver();
//            if (receiver == null) {
//                logger.error("Failed to get receiver for synthesizer ID: {}", synthesizerId);
//                return null;
//            }
//
//            InstrumentInfo info = new InstrumentInfo(instrumentName, synthesizerId, channel, SequencerConstants.GERVILL, synthInstance, receiver);
//            // Create the instrument wrapper
//            InstrumentWrapper instrument = new InstrumentWrapper(info);
//
//            // Configure instrument properties
//            instrument.setSoundBank("Java Internal Soundbank"); // Default, can be changed
//            instrument.setBankIndex(0);
//
//            // Default to appropriate preset based on channel
//            instrument.setPreset(0); // 0 is grand piano for melodic channels
//
//            // Generate ID consistently if needed, or manage externally
//            // For now, InstrumentManager handles ID assignment.
//            // instrument.setId(9985L + channel + (long)synthesizerId * 1000); // Example of unique ID
//
//            // Initialize the instrument MIDI state
//            initializeInstrumentState(instrument); // This uses instrument's receiver
//
//            logger.info("Created internal instrument: {} for channel {} on synthesizer ID {}", instrumentName, channel, synthesizerId);
//            return instrument;
//        } catch (Exception e) {
//            logger.error("Error creating internal instrument for synthesizer ID {}: {}", synthesizerId, e.getMessage(), e);
//            return null;
//        }
//    }
//
//    /**
//     * Initialize the MIDI state for an internal instrument
//     * Sends the necessary MIDI commands to set up the instrument
//     *
//     * @param instrument The instrument to initialize
//     */
//    public void initializeInstrumentState(InstrumentWrapper instrument) {
//        if (instrument == null)
//            return;
//
//        try {
//            // Apply bank selection
//            int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
//            int channel = instrument.getChannel();
//
//            // Send bank select commands
//            // int bankMSB = (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON; // POLY_MODE_ON is 127, likely meant for masking to 7 bits
//            // int bankLSB = bankIndex & MidiControlMessageEnum.POLY_MODE_ON;
//            int bankMSB = (bankIndex >> 7) & 0x7F;
//            int bankLSB = bankIndex & 0x7F;
//
//            instrument.controlChange(0, bankMSB); // Bank MSB (CC 0)
//            instrument.controlChange(32, bankLSB); // Bank LSB (CC 32)
//
//            // Send program change
//            int preset = instrument.getPreset() != null ? instrument.getPreset() : 0;
//            instrument.programChange(preset, 0); // Corrected: programChange now takes only one int argument
//
//            logger.debug("Initialized MIDI state for instrument {} (channel: {}, bank: {}, preset: {})",
//                    instrument.getName(), channel, bankIndex, preset);
//        } catch (Exception e) {
//            logger.error("Failed to initialize instrument state for {}: {}", instrument.getName(), e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Update an instrument's preset with proper MIDI commands
//     *
//     * @param instrument The instrument to update
//     * @param bankIndex  The bank index
//     * @param preset     The program/preset number
//     * @return true if preset was applied successfully, false otherwise
//     */
//    public boolean updateInstrumentPreset(InstrumentWrapper instrument, Integer bankIndex, Integer preset) {
//        if (instrument == null)
//            return false;
//
//        // The instrument's receiver is tied to a specific synthesizer.
//        // We don't strictly need synthesizerId here if the instrument's receiver is correctly set up.
//        // However, for logging and clarity, it might be useful if we could get it.
//        // For now, we assume the instrument's receiver is the correct target.
//
//        try {
//            // Update instrument properties
//            if (bankIndex != null) {
//                instrument.setBankIndex(bankIndex);
//            }
//
//            if (preset != null) {
//                instrument.setPreset(preset);
//            }
//
//            // Make sure we have the correct channel
//            int channel = instrument.getChannel(); // Now primitive, no null check needed
//            // Use current values from instrument, which might have been updated above
//            int currentBankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
//            int currentPreset = instrument.getPreset() != null ? instrument.getPreset() : 0;
//
//
//            logger.info("InternalSynthManager updating: channel={}, bank={}, program={}",
//                    channel, currentBankIndex, currentPreset);
//
//            boolean success = false;
//            Receiver receiver = instrument.getReceiver();
//
//            if (receiver != null) {
//                try {
//                    // Bank select MSB
//                    ShortMessage bankMSB = new ShortMessage();
//                    bankMSB.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0, (currentBankIndex >> 7) & 0x7F);
//                    receiver.send(bankMSB, -1);
//
//                    // Bank select LSB
//                    ShortMessage bankLSB = new ShortMessage();
//                    bankLSB.setMessage(ShortMessage.CONTROL_CHANGE, channel, 32, currentBankIndex & 0x7F);
//                    receiver.send(bankLSB, -1);
//
//                    // Program change
//                    ShortMessage pc = new ShortMessage();
//                    pc.setMessage(ShortMessage.PROGRAM_CHANGE, channel, currentPreset, 0);
//                    receiver.send(pc, -1);
//
//                    logger.info("Applied preset via instrument receiver: ch={}, bank={}, program={}",
//                            channel, currentBankIndex, currentPreset);
//                    success = true;
//                } catch (InvalidMidiDataException e) {
//                    logger.warn("Invalid MIDI data for preset change on instrument {}: {}", instrument.getName(), e.getMessage());
//                } catch (Exception e) { // Catch other potential runtime errors from receiver.send
//                    logger.warn("Error sending MIDI messages for preset change on instrument {}: {}", instrument.getName(), e.getMessage(), e);
//                }
//            } else {
//                logger.warn("No receiver available for instrument {} to apply preset change.", instrument.getName());
//                return false;
//            }
//
//            if (success) {
//                logger.debug("Updated preset for instrument {} to bank {}, program {}",
//                        instrument.getName(),
//                        instrument.getBankIndex(), // Log the potentially null value for clarity
//                        instrument.getPreset());   // Log the potentially null value for clarity
//            }
//            return success;
//        } catch (Exception e) { // Catch-all for unexpected errors
//            logger.error("Failed to update instrument preset for {}: {}", instrument.getName(), e.getMessage(), e);
//            return false;
//        }
//    }
//
//    /**
//     * Simple test to check if an instrument belongs to the internal synth
//     *
//     * @param instrument The instrument to check
//     * @return true if this is an internal synth instrument
//     */
//    public boolean isInternalSynthInstrument(InstrumentWrapper instrument) {
//        if (instrument == null) {
//            return false;
//        }
//
//        String deviceName = instrument.getDeviceName();
//        if (deviceName != null &&
//                (SequencerConstants.GERVILL.equals(deviceName) ||
//                        deviceName.contains("Java Sound Synthesizer") ||
//                        deviceName.equalsIgnoreCase(SequencerConstants.GERVILL))) {
//            return true;
//        }
//
//        Receiver instrumentReceiver = instrument.getReceiver();
//        if (instrumentReceiver != null) {
//            for (Synthesizer synth : synthesizers.values()) {
//                if (synth != null && synth.isOpen()) {
//                    try {
//                        if (instrumentReceiver == synth.getReceiver()) {
//                            return true;
//                        }
//                    } catch (MidiUnavailableException e) {
//                        logger.warn("Could not get receiver for synthesizer {} while checking instrument {}: {}",
//                                synth.getDeviceInfo() != null ? synth.getDeviceInfo().getName() : "Unknown Synth",
//                                instrument.getName(),
//                                e.getMessage());
//                    }
//                }
//            }
//        }
//        return false;
//    }
//
//    /**
//     * Create a drum kit instrument (always on channel 9).
//     *
//     * @param synthesizerId The ID of the synthesizer to create the drum kit on.
//     * @return A fully configured drum kit instrument or null if creation fails.
//     */
//    public InstrumentWrapper createDrumKitInstrument(int synthesizerId) {
//        return createInternalInstrument(synthesizerId, SequencerConstants.MIDI_DRUM_CHANNEL, true, "Internal Drum Kit SynthID " + synthesizerId);
//    }
//
//    /**
//     * Overloaded version for convenience, uses DEFAULT_SYNTH_ID.
//     *
//     * @return A fully configured drum kit instrument on the default synthesizer or null if creation fails.
//     */
//    public InstrumentWrapper createDrumKitInstrument() {
//        return createInternalInstrument(DEFAULT_SYNTH_ID, SequencerConstants.MIDI_DRUM_CHANNEL, true, "Internal Drum Kit (Default Synth)");
//    }
//
//    /**
//     * Play a note through the internal synth (optimized path)
//     *
//     * @param synthesizerId The ID of the synthesizer
//     * @param note          MIDI note number
//     * @param velocity      Velocity (0-127)
//     * @param durationMs    Duration in milliseconds
//     * @param channel       MIDI channel
//     */
//    public void playNote(int synthesizerId, int note, int velocity, int durationMs, int channel) {
//        Synthesizer synthInstance = getOrCreateSynthesizer(synthesizerId);
//        if (synthInstance == null || !synthInstance.isOpen()) {
//            logger.warn("Synthesizer ID {} not available for playNote", synthesizerId);
//            return;
//        }
//
//        try {
//            MidiChannel midiChannel = getChannel(synthesizerId, channel);
//            if (midiChannel != null) {
//                midiChannel.noteOn(note, velocity);
//                // Ensure noteOffScheduler is used, it was marked as unused previously
//                noteOffScheduler.schedule(() -> {
//                    try {
//                        // Re-fetch channel in case it became invalid or synth was closed/reopened
//                        MidiChannel currentChannel = getChannel(synthesizerId, channel);
//                        if (currentChannel != null) {
//                            currentChannel.noteOff(note);
//                        }
//                    } catch (Exception e) {
//                        logger.trace("Error in scheduled noteOff for note {} on channel {} (Synth ID {}): {}", note, channel, synthesizerId, e.getMessage());
//                    }
//                }, durationMs, TimeUnit.MILLISECONDS);
//            } else {
//                logger.warn("Could not get MIDI channel {} for synthesizer ID {} to play note", channel, synthesizerId);
//            }
//        } catch (Exception e) {
//            logger.error("Error playing note {} on synthesizer ID {}: {}", note, synthesizerId, e.getMessage(), e);
//        }
//    }
//
//    /**
//     * All notes off for a specific channel on a specific synthesizer.
//     *
//     * @param synthesizerId The ID of the synthesizer.
//     * @param channel       The MIDI channel.
//     */
//    public void allNotesOff(int synthesizerId, int channel) {
//        Synthesizer synthInstance = getOrCreateSynthesizer(synthesizerId);
//        if (synthInstance == null || !synthInstance.isOpen()) {
//            logger.warn("Synthesizer ID {} not available for allNotesOff", synthesizerId);
//            return;
//        }
//
//        try {
//            MidiChannel midiChannel = getChannel(synthesizerId, channel);
//            if (midiChannel != null) {
//                midiChannel.controlChange(123, 0); // All Notes Off CC
//                logger.debug("Sent All Notes Off to channel {} on synthesizer ID {}", channel, synthesizerId);
//            } else {
//                logger.warn("Could not get MIDI channel {} for allNotesOff on synthesizer ID {}", channel, synthesizerId);
//            }
//        } catch (Exception e) {
//            logger.error("Error sending all notes off on synthesizer ID {}: {}", synthesizerId, e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Load the default soundbank and any custom soundbanks
//     */
//    public void loadDefaultSoundbank() {
//        // Ensure the default synthesizer is attempted to be created if none exist yet.
//        if (synthesizers.isEmpty()) {
//            getOrCreateSynthesizer(DEFAULT_SYNTH_ID); // Attempt to initialize the default synth
//        }
//
//        if (synthesizers.isEmpty()) {
//            logger.error("No synthesizers available (even after attempting to init default) to load default soundbank");
//            return;
//        }
//
//        // Iterate over a copy of values if modification within loop is possible, though not here.
//        for (Synthesizer synth : new ArrayList<>(synthesizers.values())) { // Iterate over a copy
//            if (synth == null || !synth.isOpen()) continue;
//            try {
//                Soundbank defaultSoundbank = synth.getDefaultSoundbank();
//                if (defaultSoundbank != null) {
//                    String sbName = "Java Internal Soundbank";
//                    soundbanks.put(sbName, defaultSoundbank);
//                    analyzeAvailableBanks(sbName, defaultSoundbank);
//                    // Log which synth this default soundbank came from
//                    logger.info("Loaded default soundbank from synthesizer: {}", synth.getDeviceInfo().getName());
//                    // Break if we only need one default soundbank across all synths
//                    // Or continue if each synth might have a different default to be registered
//                    // For now, assume one shared "Java Internal Soundbank" is sufficient.
//                    break;
//                } else {
//                    logger.warn("Synthesizer {} does not have a default soundbank.", synth.getDeviceInfo().getName());
//                }
//            } catch (Exception e) {
//                logger.error("Error loading default soundbank from synthesizer {}: {}",
//                        synth.getDeviceInfo() != null ? synth.getDeviceInfo().getName() : "Unknown Synth",
//                        e.getMessage(), e);
//            }
//        }
//
//        if (soundbanks.containsKey("Java Internal Soundbank")) {
//            logger.info("Default soundbank 'Java Internal Soundbank' loaded. Total soundbanks available: {}", soundbanks.size());
//        } else {
//            logger.warn("Default soundbank could not be loaded from any available synthesizer.");
//        }
//    }
//
//    /**
//     * Get available soundbank names
//     */
//    public List<String> getSoundbankNames() {
//        return SoundbankManager.getInstance().getSoundbankNames();
//    }
//
//    /**
//     * Get banks for a soundbank
//     */
//    public List<Integer> getAvailableBanksByName(String soundbankName) {
//        return SoundbankManager.getInstance().getAvailableBanksByName(soundbankName);
//    }
//
//    /**
//     * Get preset names for a soundbank and bank
//     */
//    public List<String> getPresetNames(String soundbankName, int bankIndex) {
//        return SoundbankManager.getInstance().getPresetNames(soundbankName, bankIndex);
//    }
//
//    /**
//     * Get General MIDI preset names
//     */
//    public List<String> getGeneralMIDIPresetNames() {
//        return SoundbankManager.getInstance().getGeneralMIDIPresetNames();
//    }
//
//    /**
//     * Get drum items
//     */
//    public List<DrumItem> getDrumItems() {
//        return SoundbankManager.getInstance().getDrumItems();
//    }
//
//    /**
//     * Get drum name for a note
//     */
//    public String getDrumName(int noteNumber) {
//        return SoundbankManager.getInstance().getDrumName(noteNumber);
//    }
//
//    /**
//     * Load a soundbank file
//     */
//    public String loadSoundbank(File file) {
//        return SoundbankManager.getInstance().loadSoundbank(file);
//    }
//
//    /**
//     * Apply a soundbank to an instrument
//     * This centralizes soundbank application functionality previously distributed
//     * between SoundbankManager and PlayerManager
//     *
//     * @param synthesizerId The ID of the synthesizer
//     * @param instrument    The instrument to apply the soundbank to
//     * @param soundbankName The name of the soundbank to apply
//     * @return True if the soundbank was successfully applied
//     */
//    public boolean applySoundbank(InstrumentWrapper instrument, String soundbankName) {
//        if (instrument == null) {
//            logger.warn("Cannot apply soundbank - instrument is null");
//            return false;
//        }
//
//        Synthesizer synthInstance = getOrCreateSynthesizer(synthesizerId);
//        if (synthInstance == null || !synthInstance.isOpen()) {
//            logger.error("Cannot apply soundbank - synthesizer ID {} is not available", synthesizerId);
//            return false;
//        }
//
//        try {
//            instrument.setSoundBank(soundbankName);
//            Soundbank soundbank = soundbanks.get(soundbankName);
//            if (soundbank == null) {
//                logger.warn("Soundbank not found: {}. Attempting to initialize soundbanks.", soundbankName);
//                initializeSoundbanks(); // Try to load it if not found
//                soundbank = soundbanks.get(soundbankName);
//                if (soundbank == null) {
//                    logger.error("Soundbank {} still not found after re-initialization.", soundbankName);
//                    return false;
//                }
//            }
//
//            // Ensure the correct synthesizer instance is used for soundbank operations
//            Soundbank defaultSoundbank = synthInstance.getDefaultSoundbank();
//            if (defaultSoundbank != null) {
//                // Check if the soundbank is already loaded on this specific synth instance.
//                // This is a simplification; true soundbank management per synth is complex.
//                // For now, assume unloading/loading affects the given synth instance.
//                synthInstance.unloadAllInstruments(defaultSoundbank); // Unload default from this specific synth
//            } else {
//                logger.warn("Synthesizer ID {} does not have a default soundbank to unload.", synthesizerId);
//            }
//
//            boolean loaded = synthInstance.loadAllInstruments(soundbank);
//
//            if (loaded) {
//                logger.info("Successfully applied soundbank {} to instrument {} on synthesizer ID {}",
//                        soundbankName, instrument.getName(), synthesizerId);
//
//                boolean presetUpdated = updateInstrumentPreset(instrument, instrument.getBankIndex(), instrument.getPreset());
//                return presetUpdated;
//            } else {
//                logger.warn("Failed to load instruments from soundbank {} on synthesizer ID {}", soundbankName, synthesizerId);
//                return false;
//            }
//
//        } catch (Exception e) {
//            logger.error("Error applying soundbank {} on synthesizer ID {}: {}", soundbankName, synthesizerId, e.getMessage(), e);
//            return false;
//        }
//    }
//
//    public void setControlChange(int synthesizerId, int channel, int ccNumber, int value) {
//        Synthesizer synthInstance = getOrCreateSynthesizer(synthesizerId);
//        if (synthInstance == null || !synthInstance.isOpen()) {
//            logger.warn("Synthesizer ID {} not available for setControlChange", synthesizerId);
//            return;
//        }
//
//        try {
//            MidiChannel midiChannel = getChannel(synthesizerId, channel);
//            if (midiChannel != null) {
//                midiChannel.controlChange(ccNumber, value);
//            } else {
//                logger.warn("Could not get MIDI channel {} for setControlChange on synthesizer ID {}", channel, synthesizerId);
//            }
//        } catch (Exception e) {
//            logger.error("Error setting control change on synthesizer ID {}: {}", synthesizerId, e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Check if an instrument is from the internal synthesizer
//     *
//     * @param instrument The instrument to check
//     * @return true if the instrument is from the internal synthesizer
//     */
//    public boolean isInternalSynth(InstrumentWrapper instrument) {
//        // This method was a duplicate or simpler version of isInternalSynthInstrument.
//        // Consolidating to use the more robust version.
//        if (instrument == null) {
//            return false;
//        }
//        return isInternalSynthInstrument(instrument);
//    }
//
//    /**
//     * Play a test note on a player's instrument with the current settings
//     * This method is flexible and can handle either velocity or duration as the second parameter
//     *
//     * @param player          The player to test
//     * @param valueOrDuration Either the velocity (0-127) or the duration in milliseconds
//     */
//    public void playPreviewNote(Player player, int synthesizerId, int valueOrDuration) {
//        // Assuming Player class has getInstrument() and getChannel()
//        // If Player class structure is different, this needs to be adapted.
//        // For now, proceeding with the assumption based on previous context.
//        // If 'com.angrysurfer.core.model.Player' is not the correct type or does not have these methods,
//        // this will fail at compile time and will need to be addressed.
//
//        if (player == null || player.getInstrument() == null) { // Assuming getInstrument() exists
//            logger.warn("Cannot play test note - player or instrument is null");
//            return;
//        }
//
//        InstrumentWrapper instrument = player.getInstrument(); // Assuming getInstrument() exists
//        if (!isInternalSynthInstrument(instrument)) {
//            logger.warn("Cannot play test note with InternalSynthManager - not an internal synth instrument: {} (Device: {})",
//                    instrument.getName(), instrument.getDeviceName());
//            // Adding a recheck based on receiver, similar to previous logic
//            boolean recheck = false;
//            if (instrument.getReceiver() != null) {
//                for (Map.Entry<Integer, Synthesizer> entry : synthesizers.entrySet()) {
//                    try {
//                        if (entry.getValue() != null && entry.getValue().isOpen() && entry.getValue().getReceiver() == instrument.getReceiver()) {
//                            logger.info("Re-confirmed instrument {} is internal to synth ID {} via receiver for preview.", instrument.getName(), entry.getKey());
//                            // If reconfirmed, we should use the synthId associated with this receiver
//                            synthesizerId = entry.getKey(); // Update synthesizerId to the one owning the receiver
//                            recheck = true;
//                            break;
//                        }
//                    } catch (MidiUnavailableException e) {
//                        logger.warn("Error getting receiver for synth ID {} during preview recheck", entry.getKey());
//                    }
//                }
//            }
//            if (!recheck) {
//                return;
//            }
//        }
//
//        Synthesizer synthInstance = getOrCreateSynthesizer(synthesizerId); // Use potentially updated synthesizerId
//        if (synthInstance == null || !synthInstance.isOpen()) {
//            logger.warn("Synthesizer ID {} not available for playPreviewNote", synthesizerId);
//            return;
//        }
//
//        try {
//            String soundbankName = instrument.getSoundBank();
//            Integer bankIndex = instrument.getBankIndex();
//            Integer preset = instrument.getPreset();
//
//            if (soundbankName != null && !soundbankName.isEmpty()) {
//                // Apply soundbank to the specific synthesizer instance
//                boolean applied = applySoundbank(instrument, soundbankName);
//                if (!applied) {
//                    logger.warn("Failed to apply soundbank '{}' to synthesizer ID {} before preview note", soundbankName, synthesizerId);
//                }
//            }
//
//            if (bankIndex != null && preset != null) {
//                // updateInstrumentPreset uses the instrument's receiver, which should be tied to the synth.
//                updateInstrumentPreset(instrument, bankIndex, preset);
//            }
//
//            int channel = player.getChannel(); // Assuming getChannel() exists on Player
//            final int note = (channel == SequencerConstants.MIDI_DRUM_CHANNEL) ? 36 : 60; // Default notes
//
//            MidiChannel midiChannel = getChannel(synthesizerId, channel);
//            if (midiChannel != null) {
//                int velocity = 100;
//                int durationMs = 500;
//                if (valueOrDuration <= 127 && valueOrDuration >= 0) { // Check for valid velocity range
//                    velocity = valueOrDuration;
//                } else if (valueOrDuration > 0) { // Check for positive duration
//                    durationMs = valueOrDuration;
//                }
//
//                midiChannel.noteOn(note, velocity);
//                noteOffScheduler.schedule(() -> {
//                    try {
//                        midiChannel.noteOff(note);
//                    } catch (Exception e) { /* Log or ignore */ }
//                }, durationMs, TimeUnit.MILLISECONDS);
//                logger.debug("Playing test note {} on channel {} (synth ID {}) with velocity {} for {}ms", note, channel, synthesizerId, velocity, durationMs);
//            } else {
//                logger.warn("Cannot get MIDI channel {} for test note on synthesizer ID {}", channel, synthesizerId);
//            }
//        } catch (Exception e) {
//            logger.error("Error playing preview note on synthesizer ID {}: {}", synthesizerId, e.getMessage(), e);
//        }
//    }
//
//    public void playPreviewNote(int synthesizerId, Player player, int durationMs, boolean isDuration) {
//        // This overload assumes isDuration is true if provided.
//        // For simplicity, we'll call the other overload, ensuring duration is treated as duration.
//        // If durationMs could also be a velocity, this logic would need to be more complex.
//        int valueOrDuration = isDuration ? durationMs : 100; // if not duration, use default velocity
//        if (isDuration && durationMs <= 127) { // If it's meant to be duration but looks like velocity
//            // To ensure it's treated as duration by the other method if it's ambiguous,
//            // we might need a different strategy or rely on the other method's heuristic.
//        }
//        playPreviewNote(player, synthesizerId, valueOrDuration);
//    }
//
//    /**
//     * Get a MIDI channel by index from a specific synthesizer, handling initialization if needed.
//     */
//    private MidiChannel getChannel(int synthesizerId, int channelIndex) {
//        // Ensure cachedChannelsMap is used, it was marked as unused previously
//        MidiChannel[] channels = cachedChannelsMap.get(synthesizerId);
//        if (channels == null || channelIndex < 0 || channelIndex >= channels.length) {
//            Synthesizer synth = getOrCreateSynthesizer(synthesizerId); // This also caches channels
//            if (synth != null && synth.isOpen()) {
//                channels = cachedChannelsMap.get(synthesizerId); // Re-fetch from cache after synth creation
//            }
//        }
//
//        if (channels != null && channelIndex >= 0 && channelIndex < channels.length) {
//            return channels[channelIndex];
//        }
//        logger.warn("Could not retrieve MidiChannel {} for synthesizer ID {}. Channels array null or index out of bounds.", channelIndex, synthesizerId);
//        return null;
//    }
//
//    /**
//     * Check if any internal synthesizer is available.
//     */
//    public boolean checkInternalSynthAvailable() {
//        if (!synthesizers.isEmpty()) {
//            for (Synthesizer synth : synthesizers.values()) {
//                if (synth != null && synth.isOpen()) {
//                    return true; // At least one is available
//                }
//            }
//        }
//        // If map is empty or no synth is open, try to get/create a default one.
//        // This ensures that "available" means one can be made available.
//        Synthesizer defaultSynth = getOrCreateSynthesizer(DEFAULT_SYNTH_ID);
//        return defaultSynth != null && defaultSynth.isOpen();
//    }
//
//    /**
//     * Analyze a soundbank to find its available banks
//     * This method creates a mapping of the available bank numbers in the soundbank
//     *
//     * @param soundbankName The name of the soundbank
//     * @param soundbank     The soundbank object to analyze
//     */
//    private void analyzeAvailableBanks(String soundbankName, Soundbank soundbank) {
//        try {
//            if (soundbank == null) {
//                logger.warn("Cannot analyze null soundbank: {}", soundbankName);
//                return;
//            }
//
//            // Ensure synthDataMap is initialized for this soundbank
//            SynthData synthData = synthDataMap.computeIfAbsent(soundbankName, k -> new SynthData(k));
//
//            Instrument[] instruments = soundbank.getInstruments();
//            if (instruments != null) {
//                for (Instrument instrument : instruments) {
//                    synthData.addInstrument(instrument); // SynthData should handle duplicates if any
//                }
//                logger.debug("Added/updated {} instruments from soundbank {} in SynthData", instruments.length, soundbankName);
//            }
//
//            // Update the available banks map from SynthData
//            List<Integer> availableBanks = synthData.getAvailableBanks();
//            availableBanksMap.put(soundbankName, availableBanks);
//
//            logger.info("Analyzed soundbank: {} - found {} available banks", soundbankName, availableBanks.size());
//            if (logger.isDebugEnabled()) { // Guard for loop
//                for (Integer bank : availableBanks) {
//                    logger.debug("Bank {} in soundbank {} has {} presets", bank, soundbankName, synthData.getPresetsForBank(bank).size());
//                }
//            }
//        } catch (Exception e) {
//            logger.error("Error analyzing soundbank {}: {}", soundbankName, e.getMessage(), e);
//        }
//    }
//
//    // ++++++++++ ADDED METHOD ++++++++++
//
//    /**
//     * Gets the user-specific directory for soundbanks.
//     * TODO: Make this path configurable through application settings.
//     *
//     * @return File object representing the soundbank directory.
//     */
//    private File getUserSoundbankDirectory() {
//        String homeDir = System.getProperty("user.home");
//        File soundbankDir = new File(homeDir, "soundbanks"); // Example: C:\Users\YourUser\soundbanks
//
//        if (!soundbankDir.exists()) {
//            logger.warn("User soundbank directory does not exist: {}. Creating it now.", soundbankDir.getAbsolutePath());
//            if (soundbankDir.mkdirs()) {
//                logger.info("Successfully created user soundbank directory: {}", soundbankDir.getAbsolutePath());
//            } else {
//                logger.error("Failed to create user soundbank directory: {}. Custom soundbanks may not load.", soundbankDir.getAbsolutePath());
//            }
//        } else if (!soundbankDir.isDirectory()) {
//            logger.error("User soundbank path exists but is not a directory: {}. Custom soundbanks may not load.", soundbankDir.getAbsolutePath());
//        }
//        return soundbankDir;
//    }
//
//}
