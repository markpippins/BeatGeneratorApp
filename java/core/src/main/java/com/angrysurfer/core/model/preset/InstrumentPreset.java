package com.angrysurfer.core.model.preset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a MIDI instrument preset with name and program number
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentPreset {
    private String name;
    private Integer presetNumber;
    
    /**
     * Creates a new instrument preset with the specified name and number
     * @param name The preset name (e.g., "Piano")
     * @param presetNumber The MIDI program number (0-127)
     */
    public static InstrumentPreset of(String name, Integer presetNumber) {
        return new InstrumentPreset(name, presetNumber);
    }
    
    @Override
    public String toString() {
        return name + " (" + presetNumber + ")";
    }
}