package com.angrysurfer.core.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.sequencer.DrumItem;

/**
 * Manager for internal synthesizer instruments and presets. This singleton
 * provides access to internal synthesizers and their preset information.
 */
public class InternalSynthManager {

    private static final Logger logger = LoggerFactory.getLogger(InternalSynthManager.class);
    private static InternalSynthManager instance;
    
    // Add synthesizer as a central instance
    private Synthesizer synthesizer;
    private int defaultMidiChannel = 15; // Default channel for melodic sounds

    // Map of synth IDs to preset information
    private final Map<Long, SynthData> synthDataMap = new HashMap<>();

    // Use LinkedHashMap to preserve insertion order
    private final LinkedHashMap<String, Soundbank> soundbanks = new LinkedHashMap<>();

    // Map to store available banks for each soundbank (by name)
    private Map<String, List<Integer>> availableBanksMap = new HashMap<>();

    // Add these fields for performance
    private MidiChannel[] cachedChannels;
    private final ScheduledExecutorService noteOffScheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * Get the singleton instance
     */
    public static synchronized InternalSynthManager getInstance() {
        if (instance == null) {
            instance = new InternalSynthManager();
        }
        return instance;
    }

    /**
     * Initialize the manager and register command listeners
     */
    private InternalSynthManager() {
        try {
            initializeSynthData();
            initializeSoundbanks();
            initializeSynthesizer(); // Add synthesizer initialization
        } catch (Exception e) {
            logger.error("Error initializing InternalSynthManager", e);
        }
    }

    /**
     * Initialize synthesizer data structures
     */
    private void initializeSynthData() {
        try {
            // Clear existing data first
            synthDataMap.clear();
            
            // We need a synthesizer instance
            if (synthesizer == null) {
                initializeSynthesizer();
            }
            
            if (synthesizer != null) {
                // Create entry for the default soundbank
                Soundbank defaultSoundbank = synthesizer.getDefaultSoundbank();
                if (defaultSoundbank != null) {
                    String sbName = "Java Internal Soundbank";
                    SynthData synthData = new SynthData(sbName);
                    
                    // Add all instruments from default soundbank
                    for (Instrument instrument : defaultSoundbank.getInstruments()) {
                        synthData.addInstrument(instrument);
                    }
                    
                    // Store in map with the synthesizer ID
                    long synthId = System.identityHashCode(synthesizer);
                    synthDataMap.put(synthId, synthData);
                    
                    // Also add to soundbanks collection
                    soundbanks.put(sbName, defaultSoundbank);
                    
                    // Cache available banks
                    availableBanksMap.put(sbName, synthData.getAvailableBanks());
                    
                    logger.info("Initialized synthesizer data with {} instruments", 
                        synthData.getInstruments().size());
                } else {
                    logger.warn("No default soundbank available in synthesizer");
                }
            } else {
                logger.warn("No synthesizer available for initialization");
            }
        } catch (Exception e) {
            logger.error("Error initializing synth data: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize the synthesizer
     * This should be called during startup
     */
    public void initializeSynthesizer() {
        try {
            // Try to find Gervill synthesizer first
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            MidiDevice.Info gervillInfo = null;

            for (MidiDevice.Info info : infos) {
                if (info.getName().contains("Gervill")) {
                    gervillInfo = info;
                    break;
                }
            }

            if (gervillInfo != null) {
                synthesizer = (Synthesizer) MidiSystem.getMidiDevice(gervillInfo);
            }

            // If Gervill not found, get default synthesizer
            if (synthesizer == null) {
                synthesizer = MidiSystem.getSynthesizer();
            }

            if (synthesizer != null && !synthesizer.isOpen()) {
                synthesizer.open();
            }

            if (synthesizer != null && synthesizer.isOpen()) {
                logger.info("Synthesizer initialized: {}", synthesizer.getDeviceInfo().getName());
            }
        } catch (Exception e) {
            logger.error("Error initializing synthesizer: " + e.getMessage(), e);
        }
    }

    /**
     * Get the synthesizer instance
     * @return The MIDI synthesizer
     */
    public Synthesizer getSynthesizer() {
        if (synthesizer == null || !synthesizer.isOpen()) {
            initializeSynthesizer();
        }
        return synthesizer;
    }

    /**
     * Get the internal synthesizer device
     * @return The internal synthesizer device
     * @throws MidiUnavailableException if the synthesizer is unavailable
     */
    public MidiDevice getInternalSynthDevice() throws MidiUnavailableException {
        if (synthesizer == null) {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            cachedChannels = synthesizer.getChannels();
        }
        
        return synthesizer;
    }

    /**
     * Play a note on the synthesizer
     * 
     * @param note MIDI note number
     * @param velocity Note velocity (0-127)
     * @param durationMs Duration in milliseconds
     * @param channel MIDI channel to use
     */
    public void playNote(int note, int velocity, int durationMs, int channel) {
        if (synthesizer == null) {
            initializeSynthesizer();
        }
        
        if (synthesizer == null || !synthesizer.isOpen()) {
            return;
        }
        
        try {
            // Cache channels for performance
            if (cachedChannels == null) {
                cachedChannels = synthesizer.getChannels();
            }
            
            // Safety bounds check
            if (channel < 0 || channel >= cachedChannels.length) {
                return;
            }
            
            final MidiChannel midiChannel = cachedChannels[channel];
            if (midiChannel != null) {
                // Direct method call - much faster than creating threads
                midiChannel.noteOn(note, velocity);
                
                // Schedule note off with the shared executor
                noteOffScheduler.schedule(() -> {
                    try {
                        midiChannel.noteOff(note);
                    } catch (Exception e) {
                        // Ignore errors in note-off
                    }
                }, durationMs, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // Just log at trace level - don't slow down playback
            logger.trace("Error playing note: {}", e.getMessage());
        }
    }

    /**
     * Play a note on the default melodic channel
     */
    public void playNote(int note, int velocity, int durationMs) {
        playNote(note, velocity, durationMs, defaultMidiChannel);
    }

    /**
     * Play a drum note on channel 9 (drum channel)
     * 
     * @param note MIDI note number
     * @param velocity Note velocity (0-127)
     */
    public void playDrumNote(int note, int velocity) {
        if (synthesizer == null || !synthesizer.isOpen()) {
            logger.warn("Cannot play drum note - synthesizer is not available");
            return;
        }
        
        try {
            // Drum channel is 9 (10 in human terms)
            final MidiChannel channel = synthesizer.getChannels()[9];
            if (channel != null) {
                channel.noteOn(note, velocity);
            }
        } catch (Exception e) {
            logger.error("Error playing drum note: " + e.getMessage(), e);
        }
    }

    /**
     * Set MIDI Control Change on a specific channel
     */
    public void setControlChange(int channel, int ccNumber, int value) {
        if (synthesizer == null || !synthesizer.isOpen()) {
            logger.warn("Cannot send CC - synthesizer is not available");
            return;
        }
        
        try {
            MidiChannel midiChannel = synthesizer.getChannels()[channel];
            if (midiChannel != null) {
                midiChannel.controlChange(ccNumber, value);
            }
        } catch (Exception e) {
            logger.error("Error setting control change: " + e.getMessage(), e);
        }
    }

    /**
     * Check if an instrument is from the internal synthesizer
     * 
     * @param instrument The instrument to check
     * @return true if the instrument is from the internal synthesizer
     */
    public boolean isInternalSynth(InstrumentWrapper instrument) {
        if (instrument == null) {
            return false;
        }
        
        // Simple name check - fastest approach
        if (instrument.getDeviceName() != null && 
            (instrument.getDeviceName().equals("Gervill") || 
             instrument.getDeviceName().contains("Java Sound Synthesizer"))) {
            return true;
        }
        
        // Direct device instance comparison if available
        if (synthesizer != null && instrument.getDevice() == synthesizer) {
            return true;
        }
        
        // Last resort - check device info name
        if (instrument.getDevice() != null && 
            instrument.getDevice().getDeviceInfo() != null) {
            String deviceName = instrument.getDevice().getDeviceInfo().getName();
            return deviceName.equals("Gervill") || 
                   deviceName.contains("Java Sound Synthesizer");
        }
        
        return false;
    }

    /**
     * Check if the internal synthesizer is available and ready for use
     * 
     * @return true if the synthesizer is available and open
     */
    public boolean checkInternalSynthAvailable() {
        // Try to ensure the synthesizer is initialized
        if (synthesizer == null) {
            initializeSynthesizer();
        }
        
        // Check if synthesizer is available and open
        boolean isAvailable = synthesizer != null && synthesizer.isOpen();
        
        // Optional: Check if we can actually play notes
        if (isAvailable) {
            try {
                // Access channels as a simple test
                MidiChannel[] channels = synthesizer.getChannels();
                isAvailable = channels != null && channels.length > 0;
            } catch (Exception e) {
                logger.warn("Synthesizer appears to be open but is not functioning properly: {}", e.getMessage());
                isAvailable = false;
            }
        }
        
        return isAvailable;
    }

    /**
     * Initialize available soundbanks
     * @return true if initialization was successful
     */
    public boolean initializeSoundbanks() {
        try {
            logger.info("Initializing soundbanks...");
            
            // Clear existing collections first
            soundbanks.clear();
            availableBanksMap.clear();
            
            // Make sure we have a synthesizer
            if (synthesizer == null || !synthesizer.isOpen()) {
                initializeSynthesizer();
                if (synthesizer == null) {
                    logger.error("Failed to initialize synthesizer");
                    return false;
                }
            }
            
            // Add default Java soundbank
            Soundbank defaultSoundbank = synthesizer.getDefaultSoundbank();
            if (defaultSoundbank != null) {
                String sbName = "Java Internal Soundbank";
                soundbanks.put(sbName, defaultSoundbank);
                
                // Get or create SynthData for this soundbank
                long synthId = System.identityHashCode(synthesizer);
                SynthData synthData = synthDataMap.get(synthId);
                if (synthData == null) {
                    // This should have been initialized in initializeSynthData
                    // but in case it wasn't, do it now
                    synthData = new SynthData(sbName);
                    for (Instrument instrument : defaultSoundbank.getInstruments()) {
                        synthData.addInstrument(instrument);
                    }
                    synthDataMap.put(synthId, synthData);
                }
                
                // Update available banks map
                availableBanksMap.put(sbName, synthData.getAvailableBanks());
                
                logger.info("Added default soundbank with {} instruments", 
                    defaultSoundbank.getInstruments().length);
            } else {
                logger.warn("No default soundbank available in synthesizer");
            }
            
            // Check for additional user soundbanks in the app's data directory
            File soundbankDir = getUserSoundbankDirectory();
            if (soundbankDir.exists() && soundbankDir.isDirectory()) {
                File[] files = soundbankDir.listFiles((dir, name) -> 
                    name.toLowerCase().endsWith(".sf2") || name.toLowerCase().endsWith(".dls"));
                
                if (files != null) {
                    for (File file : files) {
                        try {
                            // Try to load the soundbank
                            Soundbank sb = MidiSystem.getSoundbank(file);
                            if (sb != null) {
                                String name = file.getName();
                                soundbanks.put(name, sb);
                                
                                // Create SynthData for this soundbank
                                SynthData sbData = new SynthData(name);
                                for (Instrument instrument : sb.getInstruments()) {
                                    sbData.addInstrument(instrument);
                                }
                                
                                // Store with unique ID
                                synthDataMap.put((long) sb.hashCode(), sbData);
                                
                                // Update available banks
                                availableBanksMap.put(name, sbData.getAvailableBanks());
                                
                                logger.info("Loaded soundbank from file: {} with {} instruments", 
                                    name, sb.getInstruments().length);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to load soundbank from file: {}", file.getName(), e);
                        }
                    }
                }
            }
            
            logger.info("Soundbank initialization complete. Total soundbanks: {}", soundbanks.size());
            return true;
        } catch (Exception e) {
            logger.error("Error initializing soundbanks: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the directory for user soundbank files
     */
    private File getUserSoundbankDirectory() {
        // First try user home
        String userHome = System.getProperty("user.home");
        File soundbanksDir = new File(userHome, ".beatsapp/soundbanks");
        
        // Create directory if it doesn't exist
        if (!soundbanksDir.exists()) {
            soundbanksDir.mkdirs();
        }
        
        return soundbanksDir;
    }

    /**
     * Load a soundbank from a file
     * 
     * @param file The soundbank file (.sf2 or .dls)
     * @return The name of the loaded soundbank, or null if loading failed
     */
    public String loadSoundbank(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            logger.error("Invalid soundbank file: {}", file);
            return null;
        }
        
        try {
            // Load the soundbank
            Soundbank soundbank = MidiSystem.getSoundbank(file);
            if (soundbank == null) {
                logger.error("Failed to load soundbank from file: {}", file);
                return null;
            }
            
            // Use the filename as the soundbank name
            String name = file.getName();
            
            // Add to collections
            soundbanks.put(name, soundbank);
            
            // Create SynthData for this soundbank
            SynthData sbData = new SynthData(name);
            for (Instrument instrument : soundbank.getInstruments()) {
                sbData.addInstrument(instrument);
            }
            
            // Store with unique ID
            synthDataMap.put((long) soundbank.hashCode(), sbData);
            
            // Update available banks
            availableBanksMap.put(name, sbData.getAvailableBanks());
            
            // Copy file to user soundbank directory for persistence
            File userDir = getUserSoundbankDirectory();
            File destFile = new File(userDir, file.getName());
            if (!destFile.equals(file)) {
                java.nio.file.Files.copy(
                    file.toPath(), 
                    destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
            
            logger.info("Loaded soundbank: {} with {} instruments", 
                name, soundbank.getInstruments().length);
            
            return name;
        } catch (Exception e) {
            logger.error("Error loading soundbank: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Load a soundbank from a file (alias for loadSoundbank)
     * 
     * @param file The soundbank file (.sf2 or .dls)
     * @return The name of the loaded soundbank, or null if loading failed
     */
    public String loadSoundbankFile(File file) {
        return loadSoundbank(file);
    }

    /**
     * Delete a soundbank and its associated file
     * 
     * @param name The name of the soundbank to delete
     * @return true if deletion was successful
     */
    public boolean deleteSoundbank(String name) {
        if ("Java Internal Soundbank".equals(name)) {
            logger.warn("Cannot delete the default Java soundbank");
            return false;
        }
        
        try {
            // Remove from collections
            Soundbank removed = soundbanks.remove(name);
            if (removed != null) {
                // Also remove from bank maps
                availableBanksMap.remove(name);
                
                // Find and remove associated SynthData
                long removeKey = -1;
                for (Map.Entry<Long, SynthData> entry : synthDataMap.entrySet()) {
                    if (entry.getValue().getName().equals(name)) {
                        removeKey = entry.getKey();
                        break;
                    }
                }
                if (removeKey >= 0) {
                    synthDataMap.remove(removeKey);
                }
                
                // Try to delete the file if it exists
                File userDir = getUserSoundbankDirectory();
                File soundbankFile = new File(userDir, name);
                if (soundbankFile.exists()) {
                    boolean deleted = soundbankFile.delete();
                    if (!deleted) {
                        logger.warn("Could not delete soundbank file: {}", soundbankFile);
                    }
                }
                
                logger.info("Deleted soundbank: {}", name);
                return true;
            } else {
                logger.warn("Soundbank not found: {}", name);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error deleting soundbank: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get a list of available soundbank names
     */
    public List<String> getSoundbankNames() {
        return new ArrayList<>(soundbanks.keySet());
    }

    /**
     * Get a soundbank by name
     */
    public Soundbank getSoundbank(String name) {
        return soundbanks.get(name);
    }

    /**
     * Get a soundbank by name (alias for getSoundbank)
     * 
     * @param name The name of the soundbank to retrieve
     * @return The Soundbank object, or null if not found
     */
    public Soundbank getSoundbankByName(String name) {
        return getSoundbank(name);
    }

    /**
     * Get available banks for a soundbank by name
     */
    public List<Integer> getAvailableBanksByName(String soundbankName) {
        return availableBanksMap.getOrDefault(soundbankName, new ArrayList<>());
    }

    /**
     * Get preset names for a specific soundbank and bank
     */
    public List<String> getPresetNames(String soundbankName, int bankIndex) {
        // Find the SynthData for this soundbank
        for (SynthData synthData : synthDataMap.values()) {
            if (synthData.getName().equals(soundbankName)) {
                return synthData.getPresetNamesForBank(bankIndex);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Get preset names for a specific instrument ID
     * 
     * @param instrumentId The instrument ID to look up presets for
     * @return List of preset names for the instrument
     */
    public List<String> getPresetNames(Long instrumentId) {
        if (instrumentId == null) {
            return getGeneralMIDIPresetNames(); // Fall back to standard GM names
        }
        
        try {
            // Get the instrument from InstrumentManager
            InstrumentWrapper instrument = InstrumentManager.getInstance().getInstrumentById(instrumentId);
            if (instrument == null) {
                logger.warn("Instrument not found for ID: {}", instrumentId);
                return getGeneralMIDIPresetNames();
            }
            
            // Get soundbank name from instrument
            String soundbankName = instrument.getSoundbankName();
            if (soundbankName == null || soundbankName.isEmpty()) {
                soundbankName = "Java Internal Soundbank";
            }
            
            // Get bank index from instrument
            int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
            
            // Call the original method with the extracted parameters
            return getPresetNames(soundbankName, bankIndex);
        } catch (Exception e) {
            logger.error("Error getting preset names for instrument {}: {}", 
                        instrumentId, e.getMessage());
            return getGeneralMIDIPresetNames(); // Fall back to standard GM names
        }
    }

    /**
     * Get a preset name for an instrument and preset number
     * 
     * @param instrumentId The ID of the instrument (used to find the right soundbank)
     * @param presetNumber The preset number to look up
     * @return The name of the preset, or a default name if not found
     */
    public String getPresetName(Long instrumentId, long presetNumber) {
        if (instrumentId == null || presetNumber < 0 || presetNumber > 127) {
            return "Unknown Preset";
        }
        
        try {
            // First, get the instrument from InstrumentManager
            InstrumentWrapper instrument = InstrumentManager.getInstance().getInstrumentById(instrumentId);
            if (instrument == null) {
                return "Program " + presetNumber;
            }
            
            // Get the soundbank name from the instrument
            String soundbankName = instrument.getSoundbankName();
            if (soundbankName == null || soundbankName.isEmpty()) {
                // Fall back to default soundbank
                soundbankName = "Java Internal Soundbank";
            }
            
            // Get the bank index from the instrument or default to 0
            int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
            
            // Find the SynthData for this soundbank
            for (SynthData synthData : synthDataMap.values()) {
                if (synthData.getName().equals(soundbankName)) {
                    // Get the presets for this bank
                    Map<Integer, String> presets = synthData.getPresetsForBank(bankIndex);
                    
                    // Look up the preset name
                    String presetName = presets.get((int)presetNumber);
                    if (presetName != null && !presetName.isEmpty()) {
                        return presetName;
                    } else {
                        // Return a default name with the number if not found
                        return "Program " + presetNumber;
                    }
                }
            }
            
            // If we get here, the soundbank wasn't found
            return "Program " + presetNumber;
        } catch (Exception e) {
            logger.error("Error getting preset name: {}", e.getMessage());
            return "Program " + presetNumber;
        }
    }

    /**
     * Get a list of standard General MIDI drum items
     * 
     * @return List of DrumItem objects for all standard GM drum sounds
     */
    public List<DrumItem> getDrumItems() {
        List<DrumItem> drums = new ArrayList<>();
        
        // Standard General MIDI drum mappings
        drums.add(new DrumItem(35, "Acoustic Bass Drum"));
        drums.add(new DrumItem(36, "Bass Drum 1"));
        drums.add(new DrumItem(37, "Side Stick"));
        drums.add(new DrumItem(38, "Acoustic Snare"));
        drums.add(new DrumItem(39, "Hand Clap"));
        drums.add(new DrumItem(40, "Electric Snare"));
        drums.add(new DrumItem(41, "Low Floor Tom"));
        drums.add(new DrumItem(42, "Closed Hi-Hat"));
        drums.add(new DrumItem(43, "High Floor Tom"));
        drums.add(new DrumItem(44, "Pedal Hi-Hat"));
        drums.add(new DrumItem(45, "Low Tom"));
        drums.add(new DrumItem(46, "Open Hi-Hat"));
        drums.add(new DrumItem(47, "Low-Mid Tom"));
        drums.add(new DrumItem(48, "Hi-Mid Tom"));
        drums.add(new DrumItem(49, "Crash Cymbal 1"));
        drums.add(new DrumItem(50, "High Tom"));
        drums.add(new DrumItem(51, "Ride Cymbal 1"));
        drums.add(new DrumItem(52, "Chinese Cymbal"));
        drums.add(new DrumItem(53, "Ride Bell"));
        drums.add(new DrumItem(54, "Tambourine"));
        drums.add(new DrumItem(55, "Splash Cymbal"));
        drums.add(new DrumItem(56, "Cowbell"));
        drums.add(new DrumItem(57, "Crash Cymbal 2"));
        drums.add(new DrumItem(58, "Vibraslap"));
        drums.add(new DrumItem(59, "Ride Cymbal 2"));
        drums.add(new DrumItem(60, "Hi Bongo"));
        drums.add(new DrumItem(61, "Low Bongo"));
        drums.add(new DrumItem(62, "Mute Hi Conga"));
        drums.add(new DrumItem(63, "Open Hi Conga"));
        drums.add(new DrumItem(64, "Low Conga"));
        drums.add(new DrumItem(65, "High Timbale"));
        drums.add(new DrumItem(66, "Low Timbale"));
        drums.add(new DrumItem(67, "High Agogo"));
        drums.add(new DrumItem(68, "Low Agogo"));
        drums.add(new DrumItem(69, "Cabasa"));
        drums.add(new DrumItem(70, "Maracas"));
        drums.add(new DrumItem(71, "Short Whistle"));
        drums.add(new DrumItem(72, "Long Whistle"));
        drums.add(new DrumItem(73, "Short Guiro"));
        drums.add(new DrumItem(74, "Long Guiro"));
        drums.add(new DrumItem(75, "Claves"));
        drums.add(new DrumItem(76, "Hi Wood Block"));
        drums.add(new DrumItem(77, "Low Wood Block"));
        drums.add(new DrumItem(78, "Mute Cuica"));
        drums.add(new DrumItem(79, "Open Cuica"));
        drums.add(new DrumItem(80, "Mute Triangle"));
        drums.add(new DrumItem(81, "Open Triangle"));
        
        return drums;
    }

    /**
     * Get the name of a drum sound by its MIDI note number
     * 
     * @param noteNumber The MIDI note number to look up
     * @return The name of the drum sound, or "Unknown Drum" if not found
     */
    public String getDrumName(int noteNumber) {
        // Get all drum items
        List<DrumItem> drums = getDrumItems();
        
        // Find the matching drum by note number
        for (DrumItem drum : drums) {
            if (drum.getNoteNumber() == noteNumber) {
                return drum.getName();
            }
        }
        
        // Return a placeholder for unknown drums
        return "Unknown Drum (" + noteNumber + ")";
    }

    /**
     * Get the standard General MIDI instrument preset names
     * 
     * @return List of 128 preset names as defined by the General MIDI specification
     */
    public List<String> getGeneralMIDIPresetNames() {
        List<String> presets = new ArrayList<>(128);
        
        // Piano Family (0-7)
        presets.add("Acoustic Grand Piano");
        presets.add("Bright Acoustic Piano");
        presets.add("Electric Grand Piano");
        presets.add("Honky-tonk Piano");
        presets.add("Electric Piano 1");
        presets.add("Electric Piano 2");
        presets.add("Harpsichord");
        presets.add("Clavinet");
        
        // Chromatic Percussion (8-15)
        presets.add("Celesta");
        presets.add("Glockenspiel");
        presets.add("Music Box");
        presets.add("Vibraphone");
        presets.add("Marimba");
        presets.add("Xylophone");
        presets.add("Tubular Bells");
        presets.add("Dulcimer");
        
        // Organ (16-23)
        presets.add("Drawbar Organ");
        presets.add("Percussive Organ");
        presets.add("Rock Organ");
        presets.add("Church Organ");
        presets.add("Reed Organ");
        presets.add("Accordion");
        presets.add("Harmonica");
        presets.add("Tango Accordion");
        
        // Guitar (24-31)
        presets.add("Acoustic Guitar (nylon)");
        presets.add("Acoustic Guitar (steel)");
        presets.add("Electric Guitar (jazz)");
        presets.add("Electric Guitar (clean)");
        presets.add("Electric Guitar (muted)");
        presets.add("Overdriven Guitar");
        presets.add("Distortion Guitar");
        presets.add("Guitar Harmonics");
        
        // Bass (32-39)
        presets.add("Acoustic Bass");
        presets.add("Electric Bass (finger)");
        presets.add("Electric Bass (pick)");
        presets.add("Fretless Bass");
        presets.add("Slap Bass 1");
        presets.add("Slap Bass 2");
        presets.add("Synth Bass 1");
        presets.add("Synth Bass 2");
        
        // Strings (40-47)
        presets.add("Violin");
        presets.add("Viola");
        presets.add("Cello");
        presets.add("Contrabass");
        presets.add("Tremolo Strings");
        presets.add("Pizzicato Strings");
        presets.add("Orchestral Harp");
        presets.add("Timpani");
        
        // Ensemble (48-55)
        presets.add("String Ensemble 1");
        presets.add("String Ensemble 2");
        presets.add("Synth Strings 1");
        presets.add("Synth Strings 2");
        presets.add("Choir Aahs");
        presets.add("Voice Oohs");
        presets.add("Synth Choir");
        presets.add("Orchestra Hit");
        
        // Brass (56-63)
        presets.add("Trumpet");
        presets.add("Trombone");
        presets.add("Tuba");
        presets.add("Muted Trumpet");
        presets.add("French Horn");
        presets.add("Brass Section");
        presets.add("Synth Brass 1");
        presets.add("Synth Brass 2");
        
        // Reed (64-71)
        presets.add("Soprano Sax");
        presets.add("Alto Sax");
        presets.add("Tenor Sax");
        presets.add("Baritone Sax");
        presets.add("Oboe");
        presets.add("English Horn");
        presets.add("Bassoon");
        presets.add("Clarinet");
        
        // Pipe (72-79)
        presets.add("Piccolo");
        presets.add("Flute");
        presets.add("Recorder");
        presets.add("Pan Flute");
        presets.add("Blown Bottle");
        presets.add("Shakuhachi");
        presets.add("Whistle");
        presets.add("Ocarina");
        
        // Synth Lead (80-87)
        presets.add("Lead 1 (square)");
        presets.add("Lead 2 (sawtooth)");
        presets.add("Lead 3 (calliope)");
        presets.add("Lead 4 (chiff)");
        presets.add("Lead 5 (charang)");
        presets.add("Lead 6 (voice)");
        presets.add("Lead 7 (fifths)");
        presets.add("Lead 8 (bass + lead)");
        
        // Synth Pad (88-95)
        presets.add("Pad 1 (new age)");
        presets.add("Pad 2 (warm)");
        presets.add("Pad 3 (polysynth)");
        presets.add("Pad 4 (choir)");
        presets.add("Pad 5 (bowed)");
        presets.add("Pad 6 (metallic)");
        presets.add("Pad 7 (halo)");
        presets.add("Pad 8 (sweep)");
        
        // Synth Effects (96-103)
        presets.add("FX 1 (rain)");
        presets.add("FX 2 (soundtrack)");
        presets.add("FX 3 (crystal)");
        presets.add("FX 4 (atmosphere)");
        presets.add("FX 5 (brightness)");
        presets.add("FX 6 (goblins)");
        presets.add("FX 7 (echoes)");
        presets.add("FX 8 (sci-fi)");
        
        // Ethnic (104-111)
        presets.add("Sitar");
        presets.add("Banjo");
        presets.add("Shamisen");
        presets.add("Koto");
        presets.add("Kalimba");
        presets.add("Bagpipe");
        presets.add("Fiddle");
        presets.add("Shanai");
        
        // Percussive (112-119)
        presets.add("Tinkle Bell");
        presets.add("Agogo");
        presets.add("Steel Drums");
        presets.add("Woodblock");
        presets.add("Taiko Drum");
        presets.add("Melodic Tom");
        presets.add("Synth Drum");
        presets.add("Reverse Cymbal");
        
        // Sound Effects (120-127)
        presets.add("Guitar Fret Noise");
        presets.add("Breath Noise");
        presets.add("Seashore");
        presets.add("Bird Tweet");
        presets.add("Telephone Ring");
        presets.add("Helicopter");
        presets.add("Applause");
        presets.add("Gunshot");
        
        return presets;
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
     * Apply a preset change to an instrument
     * 
     * @param instrument The instrument to apply the preset to
     * @param bankIndex The bank index to select
     * @param presetNumber The preset number to select
     */
    public void applyPresetChange(InstrumentWrapper instrument, int bankIndex, int presetNumber) {
        if (instrument == null) {
            return;
        }
        
        try {
            // Store the settings in the instrument
            instrument.setBankIndex(bankIndex);
            instrument.setCurrentPreset(presetNumber);
            
            // Get the MIDI channel from the instrument or default to 0
            int channel = 0;
            if (instrument.getChannels() != null && instrument.getChannels().length > 0) {
                channel = instrument.getChannels()[0];
            }
            
            // Apply the bank and program change to the instrument's device
            if (instrument.getDevice() instanceof Synthesizer) {
                Synthesizer synth = (Synthesizer) instrument.getDevice();
                if (!synth.isOpen()) {
                    synth.open();
                }
                
                // Get the MIDI channel
                MidiChannel[] channels = synth.getChannels();
                if (channels != null && channel < channels.length) {
                    // Calculate bank MSB and LSB
                    int bankMSB = (bankIndex >> 7) & 0x7F;
                    int bankLSB = bankIndex & 0x7F;
                    
                    // Send bank select and program change
                    channels[channel].controlChange(0, bankMSB);
                    channels[channel].controlChange(32, bankLSB);
                    channels[channel].programChange(presetNumber);
                    
                    logger.debug("Applied preset change to synthesizer: bank={}, preset={}, channel={}",
                            bankIndex, presetNumber, channel);
                    return;
                }
            }
            
            // Fall back to standard MIDI messages via InstrumentWrapper
            instrument.controlChange(channel, 0, (bankIndex >> 7) & 0x7F);  // Bank MSB
            instrument.controlChange(channel, 32, bankIndex & 0x7F);        // Bank LSB
            instrument.programChange(channel, presetNumber, 0);
            
        } catch (Exception e) {
            logger.error("Failed to apply preset change: {}", e.getMessage(), e);
        }
    }

    /**
     * Inner class to store synthesizer data
     */
    private static class SynthData {
        private final String name;
        private final Map<Integer, Map<Integer, String>> bankPresetMap = new HashMap<>();
        private final List<Instrument> instruments = new ArrayList<>();
        
        public SynthData(String name) {
            this.name = name;
        }
        
        public void addInstrument(Instrument instrument) {
            if (instrument != null) {
                instruments.add(instrument);
                
                // Extract bank and program information
                Patch patch = instrument.getPatch();
                int bank = patch.getBank();
                int program = patch.getProgram();
                
                // Store the instrument name by bank and program
                Map<Integer, String> presetMap = bankPresetMap.computeIfAbsent(bank, k -> new HashMap<>());
                presetMap.put(program, instrument.getName());
            }
        }
        
        public Map<Integer, String> getPresetsForBank(int bank) {
            return bankPresetMap.getOrDefault(bank, new HashMap<>());
        }
        
        public List<Integer> getAvailableBanks() {
            List<Integer> banks = new ArrayList<>(bankPresetMap.keySet());
            java.util.Collections.sort(banks);
            return banks;
        }
        
        public List<String> getPresetNamesForBank(int bank) {
            Map<Integer, String> presetMap = getPresetsForBank(bank);
            List<String> names = new ArrayList<>(128); // Fixed size for MIDI programs
            
            // Initialize with empty strings
            for (int i = 0; i < 128; i++) {
                names.add("");
            }
            
            // Fill in names from the map
            for (Map.Entry<Integer, String> entry : presetMap.entrySet()) {
                int program = entry.getKey();
                if (program >= 0 && program < 128) {
                    names.set(program, entry.getValue());
                }
            }
            
            return names;
        }
        
        public String getName() {
            return name;
        }
        
        public List<Instrument> getInstruments() {
            return instruments;
        }
    }
}
