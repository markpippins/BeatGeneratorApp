package com.angrysurfer.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;

public class SynthData {
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