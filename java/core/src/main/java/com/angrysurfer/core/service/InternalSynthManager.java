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
     * Initialize internal synthesizer data
     */
    private void initializeSynthData() {
        // Add GM synth based on standardized preset names
        SynthData gmSynth = new SynthData(1L, "General MIDI");
        List<String> gmPresetNames = getGeneralMIDIPresetNames();

        // Add each GM preset by number and name
        for (int i = 0; i < gmPresetNames.size(); i++) {
            gmSynth.addPreset(new PresetData(i, gmPresetNames.get(i)));
        }

        synthDataMap.put(gmSynth.getId(), gmSynth);

        // Add other internal synths (example)
        SynthData fmSynth = new SynthData(2L, "FM Synth");
        fmSynth.addPresets(Arrays.asList(
                new PresetData(0, "Basic Sine"),
                new PresetData(1, "Electric Bell"),
                new PresetData(2, "Harmonic Lead"),
                new PresetData(3, "Bass Slap"),
                new PresetData(4, "Ambient Pad")
        ));
        synthDataMap.put(fmSynth.getId(), fmSynth);

        logger.info("Initialized InternalSynthManager with {} synthesizers", synthDataMap.size());
    }

    /**
     * Check if the given instrument is an internal synthesizer
     */
    public boolean isInternalSynth(InstrumentWrapper instrument) {
        if (instrument == null) {
            return false;
        }
        return synthDataMap.containsKey(instrument.getId());
    }

    /**
     * Get a list of all internal synth instruments
     */
    public List<InstrumentWrapper> getInternalSynths() {
        List<InstrumentWrapper> result = new ArrayList<>();

        for (SynthData synthData : synthDataMap.values()) {
            InstrumentWrapper instrument = new InstrumentWrapper();
            instrument.setId(synthData.getId());
            instrument.setName(synthData.getName());
            instrument.setAvailable(true);
            instrument.setInternal(true);
            result.add(instrument);
        }

        return result;
    }

    /**
     * Get a list of preset names for a specific instrument
     */
    public List<String> getPresetNames(long instrumentId) {
        SynthData synthData = synthDataMap.get(instrumentId);
        if (synthData == null) {
            return new ArrayList<>();
        }

        List<String> presetNames = new ArrayList<>();
        for (PresetData preset : synthData.getPresets()) {
            presetNames.add(preset.getName());
        }

        return presetNames;
    }

    /**
     * Get preset name for a specific instrument and preset number
     */
    public String getPresetName(long instrumentId, long presetNumber) {
        SynthData synthData = synthDataMap.get(instrumentId);
        if (synthData == null) {
            return "Program " + presetNumber;
        }

        PresetData preset = synthData.getPresetByNumber(presetNumber);
        if (preset == null) {
            return "Program " + presetNumber;
        }

        return preset.getName();
    }

    /**
     * Get the drum name for a specific note on the GM drum channel (channel
     * 9/10)
     *
     * @param noteNumber The MIDI note number (0-127)
     * @return The name of the percussion instrument
     */
    public String getDrumName(int noteNumber) {
        // Check if note is in valid range
        if (noteNumber < 0 || noteNumber > 127) {
            return "Unknown Drum";
        }

        // Standard General MIDI drum map
        switch (noteNumber) {
            case 35:
                return "Acoustic Bass Drum";
            case 36:
                return "Bass Drum 1";
            case 37:
                return "Side Stick";
            case 38:
                return "Acoustic Snare";
            case 39:
                return "Hand Clap";
            case 40:
                return "Electric Snare";
            case 41:
                return "Low Floor Tom";
            case 42:
                return "Closed Hi-Hat";
            case 43:
                return "High Floor Tom";
            case 44:
                return "Pedal Hi-Hat";
            case 45:
                return "Low Tom";
            case 46:
                return "Open Hi-Hat";
            case 47:
                return "Low-Mid Tom";
            case 48:
                return "Hi-Mid Tom";
            case 49:
                return "Crash Cymbal 1";
            case 50:
                return "High Tom";
            case 51:
                return "Ride Cymbal 1";
            case 52:
                return "Chinese Cymbal";
            case 53:
                return "Ride Bell";
            case 54:
                return "Tambourine";
            case 55:
                return "Splash Cymbal";
            case 56:
                return "Cowbell";
            case 57:
                return "Crash Cymbal 2";
            case 58:
                return "Vibraslap";
            case 59:
                return "Ride Cymbal 2";
            case 60:
                return "Hi Bongo";
            case 61:
                return "Low Bongo";
            case 62:
                return "Mute Hi Conga";
            case 63:
                return "Open Hi Conga";
            case 64:
                return "Low Conga";
            case 65:
                return "High Timbale";
            case 66:
                return "Low Timbale";
            case 67:
                return "High Agogo";
            case 68:
                return "Low Agogo";
            case 69:
                return "Cabasa";
            case 70:
                return "Maracas";
            case 71:
                return "Short Whistle";
            case 72:
                return "Long Whistle";
            case 73:
                return "Short Guiro";
            case 74:
                return "Long Guiro";
            case 75:
                return "Claves";
            case 76:
                return "Hi Wood Block";
            case 77:
                return "Low Wood Block";
            case 78:
                return "Mute Cuica";
            case 79:
                return "Open Cuica";
            case 80:
                return "Mute Triangle";
            case 81:
                return "Open Triangle";
            default:
                return "Percussion " + noteNumber;
        }
    }

    /**
     * Initialize available soundbanks
     */
    public void initializeSoundbanks() {
        try {
            // Clear existing maps
            soundbanks.clear();
            availableBanksMap.clear();

            // Add the default Java soundbank as the first entry
            soundbanks.put("Java Internal Soundbank", null);

            // Initialize default available banks
            availableBanksMap.put("Java Internal Soundbank", Arrays.asList(0));

            // Try to load soundbanks from standard locations
            File[] soundbankDirs = {
                new File(System.getProperty("user.home"), "soundfonts"),
                new File("soundfonts"),
                new File("soundbanks")
            };

            for (File dir : soundbankDirs) {
                if (dir.exists() && dir.isDirectory()) {
                    File[] soundbankFiles = dir.listFiles((d, name)
                            -> name.toLowerCase().endsWith(".sf2") || name.toLowerCase().endsWith(".dls"));

                    if (soundbankFiles != null) {
                        for (File file : soundbankFiles) {
                            try {
                                Soundbank soundbank = MidiSystem.getSoundbank(file);
                                if (soundbank != null && !soundbanks.containsKey(soundbank.getName())) {
                                    String sbName = soundbank.getName();
                                    soundbanks.put(sbName, soundbank);

                                    // Initialize available banks for this soundbank
                                    availableBanksMap.put(sbName, determineAvailableBanks(soundbank));

                                    logger.info("Loaded soundbank: {} from {}",
                                            sbName, file.getAbsolutePath());
                                }
                            } catch (Exception e) {
                                logger.error("Error loading soundbank {}: {}",
                                        file.getName(), e.getMessage());
                            }
                        }
                    }
                }
            }

            logger.info("Initialized {} soundbanks", soundbanks.size());
        } catch (Exception e) {
            logger.error("Error initializing soundbanks: " + e.getMessage(), e);
        }
    }

    /**
     * Determine available banks for a soundbank
     *
     * @param soundbank The soundbank to analyze
     * @return List of available bank numbers
     */
    private List<Integer> determineAvailableBanks(Soundbank soundbank) {
        List<Integer> banks = new ArrayList<>();
        banks.add(0); // Always add bank 0 (GM sounds)

        if (soundbank != null) {
            // Check for multi-bank support
            boolean hasMultipleBanks = false;

            for (Instrument instrument : soundbank.getInstruments()) {
                Patch patch = instrument.getPatch();
                if (patch.getBank() > 0) {
                    hasMultipleBanks = true;
                    break;
                }
            }

            if (hasMultipleBanks) {
                for (int i = 1; i <= 15; i++) {
                    banks.add(i);
                }
            }
        }

        return banks;
    }

    /**
     * Load a soundbank file and add it to the available soundbanks
     *
     * @param file The soundbank file to load
     * @return The loaded soundbank or null if loading failed
     */
    public Soundbank loadSoundbankFile(File file) {
        try {
            if (file != null && file.exists()) {
                logger.info("Loading soundbank file: " + file.getAbsolutePath());

                // Load the soundbank
                Soundbank soundbank = MidiSystem.getSoundbank(file);

                if (soundbank != null) {
                    // Add to our map
                    String name = soundbank.getName();
                    if (name == null || name.isEmpty()) {
                        name = file.getName();
                    }

                    soundbanks.put(name, soundbank);

                    // Initialize available banks for this soundbank
                    availableBanksMap.put(name, determineAvailableBanks(soundbank));

                    logger.info("Loaded soundbank: " + name);
                    return soundbank;
                } else {
                    logger.error("Failed to load soundbank from file");
                }
            }
        } catch (Exception e) {
            logger.error("Error loading soundbank: " + e.getMessage());
        }
        return null;
    }

    /**
     * DEPRECATED - Use loadSoundbankByName instead This method is maintained
     * for backward compatibility only
     */
    @Deprecated
    public boolean selectSoundbank(int index) {
        try {
            // Convert index to name
            List<String> names = new ArrayList<>(soundbanks.keySet());

            if (index < 0 || index >= names.size()) {
                logger.error("Invalid soundbank index: {}", index);
                return false;
            }

            String name = names.get(index);
            return loadSoundbankByName(name);
        } catch (Exception e) {
            logger.error("Error selecting soundbank by index: " + e.getMessage());
        }
        return false;
    }

    /**
     * Load a soundbank by name Note: This method no longer updates a "current"
     * soundbank
     *
     * @param name Name of the soundbank to select
     * @return true if the soundbank exists, false otherwise
     */
    public boolean loadSoundbankByName(String name) {
        try {
            if (!soundbanks.containsKey(name)) {
                logger.error("Soundbank not found: {}", name);
                return false;
            }

            logger.info("Soundbank exists: {}", name);
            return true;
        } catch (Exception e) {
            logger.error("Error checking soundbank by name: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get available bank numbers for the specified soundbank by name
     *
     * @param soundbankName Name of the soundbank
     * @return List of available bank numbers
     */
    public List<Integer> getAvailableBanksByName(String soundbankName) {
        // Return the cached list or compute it if not available
        List<Integer> banks = availableBanksMap.get(soundbankName);
        if (banks == null) {
            Soundbank soundbank = soundbanks.get(soundbankName);
            banks = determineAvailableBanks(soundbank);
            availableBanksMap.put(soundbankName, banks);
        }
        return new ArrayList<>(banks); // Return a copy to prevent modification
    }

    /**
     * DEPRECATED - Use getAvailableBanksByName instead This method is
     * maintained for backward compatibility only
     */
    @Deprecated
    public List<Integer> getAvailableBanks(int soundbankIndex) {
        // Convert index to name
        List<String> names = new ArrayList<>(soundbanks.keySet());
        if (soundbankIndex >= 0 && soundbankIndex < names.size()) {
            String name = names.get(soundbankIndex);
            return getAvailableBanksByName(name);
        }

        // Return default if invalid index
        List<Integer> defaultBanks = new ArrayList<>();
        defaultBanks.add(0);
        return defaultBanks;
    }

    /**
     * Get all loaded soundbank names
     *
     * @return List of soundbank names
     */
    public List<String> getSoundbankNames() {
        List<String> result = new ArrayList<>(soundbanks.keySet());
        // result = result.stream().filter(r -> r != null && !r.isEmpty()).toList();
        result.sort(String::compareToIgnoreCase);
        return result;
    }

    /**
     * Get the number of loaded soundbanks
     *
     * @return Count of available soundbanks
     */
    public int getSoundbankCount() {
        return soundbanks.size();
    }

    /**
     * DEPRECATED - Use getSoundbankByName instead This method is maintained for
     * backward compatibility only
     */
    @Deprecated
    public Soundbank getSoundbank(int index) {
        List<String> names = new ArrayList<>(soundbanks.keySet());
        if (index >= 0 && index < names.size()) {
            return soundbanks.get(names.get(index));
        }
        return null;
    }

    /**
     * Get a soundbank by name
     *
     * @param name Name of the soundbank
     * @return The soundbank or null if not found
     */
    public Soundbank getSoundbankByName(String name) {
        return soundbanks.get(name);
    }

    /**
     * Get preset names for the specified bank for a specific soundbank
     *
     * @param soundbankName Name of the soundbank to use
     * @param bank The bank number
     * @return List of preset names (up to 128)
     */
    public List<String> getPresetNames(String soundbankName, int bank) {
        List<String> presetNames = new ArrayList<>();

        try {
            // For bank 0, use General MIDI names as the default fallback
            List<String> fallbackNames = bank == 0 ? getGeneralMIDIPresetNames() : new ArrayList<>();

            // Initialize array with either GM names or empty strings
            for (int i = 0; i < 128; i++) {
                presetNames.add(i < fallbackNames.size() ? fallbackNames.get(i) : "");
            }

            // Try to get actual instrument names from the specified soundbank if available
            if (soundbankName != null) {
                Soundbank currentSoundbank = soundbanks.get(soundbankName);
                if (currentSoundbank != null) {
                    // Get all instruments for this soundbank
                    Instrument[] instruments = currentSoundbank.getInstruments();

                    if (instruments != null) {
                        // Populate with instrument names that match the bank
                        for (Instrument instrument : instruments) {
                            Patch patch = instrument.getPatch();

                            // Only include instruments from the requested bank
                            if (patch != null && patch.getBank() == bank) {
                                int program = patch.getProgram();

                                // Make sure program is in valid range
                                if (program >= 0 && program < 128) {
                                    presetNames.set(program, instrument.getName());
                                    logger.info("Found instrument: {} (Bank {}, Program {})",
                                            instrument.getName(), patch.getBank(), patch.getProgram());
                                }
                            }
                        }
                    }
                }
            }

            return presetNames;
        } catch (Exception e) {
            logger.error("Error getting preset names: " + e.getMessage());

            // Return General MIDI names for bank 0 as fallback
            if (bank == 0) {
                return getGeneralMIDIPresetNames();
            }
            return new ArrayList<>();
        }
    }

    // /**
    //  * DEPRECATED - Use getPresetNames(String, int) instead
    //  */
    // @Deprecated
    // public List<String> getPresetNames(int bank) {
    //     logger.warn("Using deprecated getPresetNames(int) method - should specify soundbank name");
    //     // Return General MIDI names for backward compatibility
    //     if (bank == 0) {
    //         return getGeneralMIDIPresetNames();
    //     }
    //     return new ArrayList<>();
    // }

    /**
     * Get preset names for a specific soundbank and bank (already had the
     * correct signature)
     *
     * @param soundbankName Name of the soundbank
     * @param bank The bank number
     * @return List of preset names (up to 128)
     */
    public List<String> getPresetNamesByBank(String soundbankName, int bank) {
        return getPresetNames(soundbankName, bank);
    }

    /**
     * Get the standard General MIDI instrument names
     *
     * @return List of all 128 General MIDI instrument names
     */
    public List<String> getGeneralMIDIPresetNames() {
        List<String> names = new ArrayList<>(128);

        // Piano (1-8)
        names.add("Acoustic Grand Piano");
        names.add("Bright Acoustic Piano");
        names.add("Electric Grand Piano");
        names.add("Honky-tonk Piano");
        names.add("Electric Piano 1");
        names.add("Electric Piano 2");
        names.add("Harpsichord");
        names.add("Clavinet");

        // Chromatic Percussion (9-16)
        names.add("Celesta");
        names.add("Glockenspiel");
        names.add("Music Box");
        names.add("Vibraphone");
        names.add("Marimba");
        names.add("Xylophone");
        names.add("Tubular Bells");
        names.add("Dulcimer");

        // Organ (17-24)
        names.add("Drawbar Organ");
        names.add("Percussive Organ");
        names.add("Rock Organ");
        names.add("Church Organ");
        names.add("Reed Organ");
        names.add("Accordion");
        names.add("Harmonica");
        names.add("Tango Accordion");

        // Guitar (25-32)
        names.add("Acoustic Guitar (nylon)");
        names.add("Acoustic Guitar (steel)");
        names.add("Electric Guitar (jazz)");
        names.add("Electric Guitar (clean)");
        names.add("Electric Guitar (muted)");
        names.add("Overdriven Guitar");
        names.add("Distortion Guitar");
        names.add("Guitar harmonics");

        // Bass (33-40)
        names.add("Acoustic Bass");
        names.add("Electric Bass (finger)");
        names.add("Electric Bass (pick)");
        names.add("Fretless Bass");
        names.add("Slap Bass 1");
        names.add("Slap Bass 2");
        names.add("Synth Bass 1");
        names.add("Synth Bass 2");

        // Strings (41-48)
        names.add("Violin");
        names.add("Viola");
        names.add("Cello");
        names.add("Contrabass");
        names.add("Tremolo Strings");
        names.add("Pizzicato Strings");
        names.add("Orchestral Harp");
        names.add("Timpani");

        // Ensemble (49-56)
        names.add("String Ensemble 1");
        names.add("String Ensemble 2");
        names.add("Synth Strings 1");
        names.add("Synth Strings 2");
        names.add("Choir Aahs");
        names.add("Voice Oohs");
        names.add("Synth Voice");
        names.add("Orchestra Hit");

        // Brass (57-64)
        names.add("Trumpet");
        names.add("Trombone");
        names.add("Tuba");
        names.add("Muted Trumpet");
        names.add("French Horn");
        names.add("Brass Section");
        names.add("Synth Brass 1");
        names.add("Synth Brass 2");

        // Reed (65-72)
        names.add("Soprano Sax");
        names.add("Alto Sax");
        names.add("Tenor Sax");
        names.add("Baritone Sax");
        names.add("Oboe");
        names.add("English Horn");
        names.add("Bassoon");
        names.add("Clarinet");

        // Pipe (73-80)
        names.add("Piccolo");
        names.add("Flute");
        names.add("Recorder");
        names.add("Pan Flute");
        names.add("Blown Bottle");
        names.add("Shakuhachi");
        names.add("Whistle");
        names.add("Ocarina");

        // Synth Lead (81-88)
        names.add("Lead 1 (square)");
        names.add("Lead 2 (sawtooth)");
        names.add("Lead 3 (calliope)");
        names.add("Lead 4 (chiff)");
        names.add("Lead 5 (charang)");
        names.add("Lead 6 (voice)");
        names.add("Lead 7 (fifths)");
        names.add("Lead 8 (bass + lead)");

        // Synth Pad (89-96)
        names.add("Pad 1 (new age)");
        names.add("Pad 2 (warm)");
        names.add("Pad 3 (polysynth)");
        names.add("Pad 4 (choir)");
        names.add("Pad 5 (bowed)");
        names.add("Pad 6 (metallic)");
        names.add("Pad 7 (halo)");
        names.add("Pad 8 (sweep)");

        // Synth Effects (97-104)
        names.add("FX 1 (rain)");
        names.add("FX 2 (soundtrack)");
        names.add("FX 3 (crystal)");
        names.add("FX 4 (atmosphere)");
        names.add("FX 5 (brightness)");
        names.add("FX 6 (goblins)");
        names.add("FX 7 (echoes)");
        names.add("FX 8 (sci-fi)");

        // Ethnic (105-112)
        names.add("Sitar");
        names.add("Banjo");
        names.add("Shamisen");
        names.add("Koto");
        names.add("Kalimba");
        names.add("Bag pipe");
        names.add("Fiddle");
        names.add("Shanai");

        // Percussive (113-120)
        names.add("Tinkle Bell");
        names.add("Agogo");
        names.add("Steel Drums");
        names.add("Woodblock");
        names.add("Taiko Drum");
        names.add("Melodic Tom");
        names.add("Synth Drum");
        names.add("Reverse Cymbal");

        // Sound effects (121-128)
        names.add("Guitar Fret Noise");
        names.add("Breath Noise");
        names.add("Seashore");
        names.add("Bird Tweet");
        names.add("Telephone Ring");
        names.add("Helicopter");
        names.add("Applause");
        names.add("Gunshot");

        return names;
    }

    /**
     * Play a test note on the specified synthesizer
     *
     * @param synth The synthesizer to play on
     * @param channel MIDI channel (0-15)
     * @param note Note number (0-127)
     * @param velocity Velocity (0-127)
     * @param preset Optional preset number to select before playing (-1 to use
     * current preset)
     * @param soundbankName Optional name of the soundbank to use for bank
     * selection
     */
    public void playTestNote(Synthesizer synth, int channel, int note, int velocity, int preset, String soundbankName) {
        try {
            if (synth == null || !synth.isOpen()) {
                logger.error("Cannot play test note - synthesizer is null or not open");
                return;
            }

            // Ensure channel is in range - store in a new final variable
            final int safeChannel = Math.max(0, Math.min(15, channel));

            // If preset is specified, change to that preset first
            if (preset >= 0) {
                // If soundbank is known, send proper bank select messages
                if (soundbankName != null) {
                    // Get the first bank from available banks for this soundbank
                    List<Integer> banks = availableBanksMap.get(soundbankName);
                    Integer bankToUse = (banks != null && !banks.isEmpty()) ? banks.get(0) : 0;

                    // Send bank select MSB (CC 0)
                    synth.getChannels()[safeChannel].controlChange(0, 0);

                    // Send bank select LSB (CC 32) 
                    synth.getChannels()[safeChannel].controlChange(32, bankToUse);

                    logger.info("Using bank {} for soundbank {}", bankToUse, soundbankName);
                }

                // Send program change
                synth.getChannels()[safeChannel].programChange(preset);
                logger.info("Changed to preset {} on channel {}", preset, safeChannel);
            }

            // Play the note
            synth.getChannels()[safeChannel].noteOn(note, velocity);

            // Schedule note off after 500ms - using safeChannel which is effectively final
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    if (synth != null && synth.isOpen()) {
                        synth.getChannels()[safeChannel].noteOff(note);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }).start();

            logger.info("Played test note {} on channel {}", note, safeChannel);
        } catch (Exception e) {
            logger.error("Error playing test note: " + e.getMessage());
        }
    }

    /**
     * DEPRECATED - Use playTestNote with soundbankName parameter
     */
    @Deprecated
    public void playTestNote(Synthesizer synth, int channel, int note, int velocity, int preset) {
        // Call the new method with null soundbankName
        playTestNote(synth, channel, note, velocity, preset, null);
    }

    /**
     * Get a soundbank by name (renamed from getCurrentSoundbank)
     *
     * @param soundbankName Name of the soundbank to get
     * @return The soundbank or null if not found
     */
    public Soundbank getSoundbank(String soundbankName) {
        return soundbankName != null ? soundbanks.get(soundbankName) : null;
    }

    /**
     * DEPRECATED - Use getSoundbank(String) or getSoundbankByName(String)
     * instead
     */
    @Deprecated
    public Soundbank getCurrentSoundbank() {
        logger.warn("Using deprecated getCurrentSoundbank() method - should specify soundbank name");
        return null;
    }

    /**
     * DEPRECATED - Methods should specify which soundbank they want
     */
    @Deprecated
    public String getCurrentSoundbankName() {
        logger.warn("Using deprecated getCurrentSoundbankName() method - no current soundbank is tracked");
        return null;
    }

    /**
     * Class to represent internal synth data
     */
    private static class SynthData {

        private final long id;
        private final String name;
        private final List<PresetData> presets = new ArrayList<>();
        private final Map<Long, PresetData> presetsByNumber = new HashMap<>();

        public SynthData(long id, String name) {
            this.id = id;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void addPreset(PresetData preset) {
            presets.add(preset);
            presetsByNumber.put((long) preset.getNumber(), preset);
        }

        public void addPresets(List<PresetData> presetList) {
            for (PresetData preset : presetList) {
                addPreset(preset);
            }
        }

        public List<PresetData> getPresets() {
            return presets;
        }

        public PresetData getPresetByNumber(long number) {
            return presetsByNumber.get(number);
        }
    }

    /**
     * Class to represent preset data
     */
    private static class PresetData {

        private final int number;
        private final String name;

        public PresetData(int number, String name) {
            this.number = number;
            this.name = name;
        }

        public int getNumber() {
            return number;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Get a list of available drum sounds for channel 9
     *
     * @return List of DrumItems
     */
    public List<DrumItem> getDrumItems() {
        List<DrumItem> items = new ArrayList<>();

        // Add standard GM drum sounds (35-81)
        for (int note = 35; note <= 81; note++) {
            String drumName = getDrumName(note);
            items.add(new DrumItem(note, note + ": " + drumName));
        }

        return items;
    }

    /**
     * Apply preset change to the specified instrument
     *
     * @param instrument The instrument to apply changes to
     * @param preset The preset number to apply
     */
    public void applyPresetChange(InstrumentWrapper instrument, int channel, int preset) {
        if (instrument == null) {
            return;
        }

        try {
            // Get channel, bank and other required values
            // int channel = instrument.getChannel();
            Integer bankIndex = instrument.getBankIndex();

            if (bankIndex == null) {
                bankIndex = 0; // Default to bank 0
            }

            // For standard MIDI, bank is split into MSB/LSB controller values
            int bankMSB = (bankIndex >> 7) & 0x7F;  // Controller 0
            int bankLSB = bankIndex & 0x7F;         // Controller 32

            // Send bank select messages
            instrument.controlChange(channel, 0, bankMSB);   // Bank select MSB
            instrument.controlChange(channel, 32, bankLSB);  // Bank select LSB

            // Send program change
            instrument.programChange(channel, 0, preset);

            logger.info("Applied preset change: channel={}, bank={}, preset={}",
                    channel, bankIndex, preset);

        } catch (Exception e) {
            logger.error("Error applying preset change: {}", e.getMessage());
        }
    }

    /**
     * Play a preview note using the specified instrument and preset
     *
     * @param instrument The instrument to use
     * @param preset The preset number to play
     */
    public void playPreviewNote(InstrumentWrapper instrument, int channel, int preset) {
        if (instrument == null) {
            return;
        }

        try {
            // Make sure device is open
            if (!instrument.getDevice().isOpen()) {
                instrument.getDevice().open();
            }

            // Get channel
            // int channel = instrument.getChannel();
            // Apply the preset first
            applyPresetChange(instrument, channel, preset);

            // Play a C major chord (C4, E4, G4)
            instrument.noteOn(channel, 60, 100); // C4
            instrument.noteOn(channel, 64, 100); // E4
            instrument.noteOn(channel, 67, 100); // G4

            // Schedule note off after 500ms
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    instrument.noteOff(channel, 60, 0);
                    instrument.noteOff(channel, 64, 0);
                    instrument.noteOff(channel, 67, 0);
                } catch (Exception e) {
                    // Ignore interruption
                }
            }).start();

        } catch (Exception e) {
            logger.error("Error playing preview: {}", e.getMessage());
        }
    }

    /**
     * Play a drum sound preview on channel 9
     *
     * @param instrument The instrument to use
     * @param noteNumber The drum note number to play
     */
    public void playDrumPreview(InstrumentWrapper instrument, int noteNumber) {
        if (instrument == null) {
            return;
        }

        try {
            // Make sure device is open
            if (!instrument.getDevice().isOpen()) {
                instrument.getDevice().open();
            }

            // For drum channel (always 9)
            int drumChannel = 9;

            // Apply standard drum kit (bank 0, program 0)
            instrument.controlChange(drumChannel, 0, 0);   // Bank MSB
            instrument.controlChange(drumChannel, 32, 0);  // Bank LSB
            instrument.programChange(drumChannel, 0, 0);   // Program 0

            // Play the drum sound
            instrument.noteOn(drumChannel, noteNumber, 100);

            // No need to schedule noteOff for percussion sounds
        } catch (Exception e) {
            logger.error("Error playing drum preview: {}", e.getMessage());
        }
    }

    /**
     * Load a soundbank from a file
     *
     * @param file The soundbank file to load
     * @return The name of the loaded soundbank
     */
    public String loadSoundbank(File file) {
        try {
            // Load the soundbank
            Soundbank soundbank = loadSoundbankFile(file);

            if (soundbank != null) {
                // Add to soundbank map
                String name = soundbank.getName();
                soundbanks.put(name, soundbank);

                // Determine available banks
                availableBanksMap.put(name, determineAvailableBanks(soundbank));

                logger.info("Loaded soundbank: {} from {}", name, file.getAbsolutePath());
                return name;
            }
        } catch (Exception e) {
            logger.error("Error loading soundbank: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Delete a soundbank by name
     *
     * @param name The name of the soundbank to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteSoundbank(String name) {
        if (name == null || name.isEmpty() || !soundbanks.containsKey(name)) {
            return false;
        }

        try {
            // Remove from soundbanks map
            soundbanks.remove(name);

            // Remove from available banks map
            availableBanksMap.remove(name);

            // Log the deletion
            logger.info("Deleted soundbank: {}", name);

            return true;
        } catch (Exception e) {
            logger.error("Error deleting soundbank {}: {}", name, e.getMessage());
            return false;
        }
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        if (noteOffScheduler != null) {
            noteOffScheduler.shutdown();
        }
    }
}
