package com.angrysurfer.core.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Instrument;

/**
 * Manager for internal synthesizer instruments and presets.
 * This singleton provides access to internal synthesizers and their preset information.
 */
public class InternalSynthManager {
    private static final Logger logger = LoggerFactory.getLogger(InternalSynthManager.class);
    private static InternalSynthManager instance;
    
    // Map of synth IDs to preset information
    private final Map<Long, SynthData> synthDataMap = new HashMap<>();
    
    private List<Soundbank> loadedSoundbanks = new ArrayList<>();
    private List<String> soundbankNames = new ArrayList<>();
    private Synthesizer currentSynthesizer;

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
     * Private constructor initializes internal synth data
     */
    private InternalSynthManager() {
        initializeSynthData();
    }
    
    /**
     * Initialize internal synthesizer data
     */
    private void initializeSynthData() {
        // Add GM synth
        SynthData gmSynth = new SynthData(1L, "General MIDI");
        gmSynth.addPresets(Arrays.asList(
            new PresetData(0, "Grand Piano"),
            new PresetData(1, "Bright Piano"),
            new PresetData(2, "Electric Grand Piano"),
            new PresetData(3, "Honky-tonk Piano"),
            new PresetData(4, "Electric Piano 1"),
            new PresetData(5, "Electric Piano 2"),
            new PresetData(6, "Harpsichord"),
            new PresetData(7, "Clavi"),
            // Add more GM presets as needed...
            new PresetData(8, "Celesta"),
            new PresetData(9, "Glockenspiel"),
            new PresetData(10, "Music Box"),
            new PresetData(11, "Vibraphone"),
            new PresetData(12, "Marimba"),
            new PresetData(13, "Xylophone"),
            new PresetData(14, "Tubular Bells"),
            new PresetData(15, "Dulcimer"),
            new PresetData(16, "Drawbar Organ"),
            new PresetData(17, "Percussive Organ"),
            new PresetData(18, "Rock Organ"),
            new PresetData(19, "Church Organ"),
            new PresetData(20, "Reed Organ"),
            new PresetData(21, "Accordion"),
            new PresetData(22, "Harmonica"),
            new PresetData(23, "Tango Accordion"),
            new PresetData(24, "Acoustic Guitar (nylon)"),
            new PresetData(25, "Acoustic Guitar (steel)")
            // Continue for all 128 GM presets
        ));
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
    public boolean isInternalSynth(Instrument instrument) {
        if (instrument == null) return false;
        return synthDataMap.containsKey(instrument.getId());
    }
    
    /**
     * Get a list of all internal synth instruments
     */
    public List<Instrument> getInternalSynths() {
        List<Instrument> result = new ArrayList<>();
        
        for (SynthData synthData : synthDataMap.values()) {
            Instrument instrument = new Instrument();
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
     * Get the drum name for a specific note on the GM drum channel (channel 9/10)
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
            case 35: return "Acoustic Bass Drum";
            case 36: return "Bass Drum 1";
            case 37: return "Side Stick";
            case 38: return "Acoustic Snare";
            case 39: return "Hand Clap";
            case 40: return "Electric Snare";
            case 41: return "Low Floor Tom";
            case 42: return "Closed Hi-Hat";
            case 43: return "High Floor Tom";
            case 44: return "Pedal Hi-Hat";
            case 45: return "Low Tom";
            case 46: return "Open Hi-Hat";
            case 47: return "Low-Mid Tom";
            case 48: return "Hi-Mid Tom";
            case 49: return "Crash Cymbal 1";
            case 50: return "High Tom";
            case 51: return "Ride Cymbal 1";
            case 52: return "Chinese Cymbal";
            case 53: return "Ride Bell";
            case 54: return "Tambourine";
            case 55: return "Splash Cymbal";
            case 56: return "Cowbell";
            case 57: return "Crash Cymbal 2";
            case 58: return "Vibraslap";
            case 59: return "Ride Cymbal 2";
            case 60: return "Hi Bongo";
            case 61: return "Low Bongo";
            case 62: return "Mute Hi Conga";
            case 63: return "Open Hi Conga";
            case 64: return "Low Conga";
            case 65: return "High Timbale";
            case 66: return "Low Timbale";
            case 67: return "High Agogo";
            case 68: return "Low Agogo";
            case 69: return "Cabasa";
            case 70: return "Maracas";
            case 71: return "Short Whistle";
            case 72: return "Long Whistle";
            case 73: return "Short Guiro";
            case 74: return "Long Guiro";
            case 75: return "Claves";
            case 76: return "Hi Wood Block";
            case 77: return "Low Wood Block";
            case 78: return "Mute Cuica";
            case 79: return "Open Cuica";
            case 80: return "Mute Triangle";
            case 81: return "Open Triangle";
            default: return "Percussion " + noteNumber;
        }
    }
    
    /**
     * Set the current synthesizer instance for soundbank operations
     * @param synthesizer The synthesizer to use
     */
    public void setCurrentSynthesizer(Synthesizer synthesizer) {
        this.currentSynthesizer = synthesizer;
    }

    /**
     * Initialize available soundbanks
     */
    public void initializeSoundbanks() {
        try {
            // Clear previous data
            loadedSoundbanks.clear();
            soundbankNames.clear();
            
            // Add the default Java soundbank
            loadedSoundbanks.add(null); // Placeholder for default soundbank
            soundbankNames.add("Java Internal Soundbank");
            
            // If synthesizer is using a soundbank already, add it
            if (currentSynthesizer != null && currentSynthesizer.getDefaultSoundbank() != null) {
                Soundbank defaultSoundbank = currentSynthesizer.getDefaultSoundbank();
                String name = defaultSoundbank.getName();
                if (name != null && !name.isEmpty() && !name.equals("Unknown")) {
                    loadedSoundbanks.add(defaultSoundbank);
                    soundbankNames.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing soundbanks: " + e.getMessage());
        }
    }

    /**
     * Load a soundbank file and add it to the available soundbanks
     * @param file The soundbank file to load
     * @return The loaded soundbank or null if loading failed
     */
    public Soundbank loadSoundbankFile(File file) {
        try {
            if (file != null && file.exists()) {
                System.out.println("Loading soundbank file: " + file.getAbsolutePath());
                
                // Load the soundbank
                Soundbank soundbank = MidiSystem.getSoundbank(file);
                
                if (soundbank != null) {
                    // Add to our list
                    String name = soundbank.getName();
                    if (name == null || name.isEmpty()) {
                        name = file.getName();
                    }
                    
                    loadedSoundbanks.add(soundbank);
                    soundbankNames.add(name);
                    
                    System.out.println("Loaded soundbank: " + name);
                    return soundbank;
                } else {
                    System.err.println("Failed to load soundbank from file");
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading soundbank: " + e.getMessage());
        }
        return null;
    }

    /**
     * Select and load a soundbank into the synthesizer
     * @param index Index of the soundbank to load
     * @return true if successful
     */
    public boolean selectSoundbank(int index) {
        try {
            if (index < 0 || index >= loadedSoundbanks.size() || currentSynthesizer == null) {
                return false;
            }
            
            Soundbank soundbank = loadedSoundbanks.get(index);
            
            // Load the soundbank into the synthesizer if it's not the default
            if (soundbank != null) {
                // First unload any current instruments
                currentSynthesizer.unloadAllInstruments(currentSynthesizer.getDefaultSoundbank());
                
                // Then load the new soundbank
                boolean loaded = currentSynthesizer.loadAllInstruments(soundbank);
                if (loaded) {
                    System.out.println("Loaded soundbank: " + soundbank.getName());
                    return true;
                } else {
                    System.err.println("Failed to load soundbank: " + soundbank.getName());
                }
            } else {
                // Default Java soundbank selected
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error selecting soundbank: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get available bank numbers for the current soundbank at given index
     * @param soundbankIndex Index of the soundbank
     * @return List of available bank numbers
     */
    public List<Integer> getAvailableBanks(int soundbankIndex) {
        List<Integer> banks = new ArrayList<>();
        
        // Always add bank 0 (GM sounds)
        banks.add(0);
        
        // Try to get the soundbank
        if (soundbankIndex >= 0 && soundbankIndex < loadedSoundbanks.size()) {
            Soundbank soundbank = loadedSoundbanks.get(soundbankIndex);
            
            if (soundbank != null) {
                // Check if this is a multi-bank soundfont
                boolean hasMultipleBanks = false;
                
                for (javax.sound.midi.Instrument instrument : soundbank.getInstruments()) {
                    javax.sound.midi.Patch patch = instrument.getPatch();
                    if (patch.getBank() > 0) {
                        hasMultipleBanks = true;
                        break;
                    }
                }
                
                if (hasMultipleBanks) {
                    // Add standard banks
                    for (int i = 1; i <= 15; i++) {
                        banks.add(i);
                    }
                }
            }
        }
        
        return banks;
    }

    /**
     * Get all loaded soundbank names
     * @return List of soundbank names
     */
    public List<String> getSoundbankNames() {
        return new ArrayList<>(soundbankNames);
    }

    /**
     * Get the number of loaded soundbanks
     * @return Count of available soundbanks
     */
    public int getSoundbankCount() {
        return loadedSoundbanks.size();
    }

    /**
     * Get a soundbank by index
     * @param index Index of the soundbank
     * @return The soundbank or null if index is invalid
     */
    public Soundbank getSoundbank(int index) {
        if (index >= 0 && index < loadedSoundbanks.size()) {
            return loadedSoundbanks.get(index);
        }
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
            presetsByNumber.put((long)preset.getNumber(), preset);
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
}