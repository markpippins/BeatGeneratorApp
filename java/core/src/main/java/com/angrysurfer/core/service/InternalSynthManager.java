package com.angrysurfer.core.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.InstrumentWrapper;

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
    private Soundbank currentSoundbank;

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
        if (instrument == null) return false;
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
                    currentSoundbank = soundbank;
                    System.out.println("Loaded soundbank: " + soundbank.getName());
                    return true;
                } else {
                    System.err.println("Failed to load soundbank: " + soundbank.getName());
                }
            } else {
                // Default Java soundbank selected
                currentSoundbank = null;
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
     * Get preset names for the specified bank
     * 
     * @param bank The bank number
     * @return List of preset names (up to 128)
     */
    public List<String> getPresetNames(int bank) {
        List<String> presetNames = new ArrayList<>();
        
        try {
            // For bank 0, use General MIDI names as the default fallback
            List<String> fallbackNames = bank == 0 ? getGeneralMIDIPresetNames() : new ArrayList<>();
            
            // Initialize array with either GM names or empty strings
            for (int i = 0; i < 128; i++) {
                presetNames.add(i < fallbackNames.size() ? fallbackNames.get(i) : "");
            }
            
            // Try to get actual instrument names from the current soundbank if available
            if (currentSynthesizer != null && currentSynthesizer.isOpen() && currentSoundbank != null) {
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
                                System.out.println("Found instrument: " + instrument.getName() + 
                                                  " (Bank " + patch.getBank() + 
                                                  ", Program " + patch.getProgram() + ")");
                            }
                        }
                    }
                }
            }
            
            return presetNames;
        } catch (Exception e) {
            System.err.println("Error getting preset names: " + e.getMessage());
            e.printStackTrace();
            
            // Return General MIDI names for bank 0 as fallback
            if (bank == 0) {
                return getGeneralMIDIPresetNames();
            }
            return new ArrayList<>();
        }
    }
    
    /**
     * Get the standard General MIDI instrument names
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