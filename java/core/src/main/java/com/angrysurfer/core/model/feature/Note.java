package com.angrysurfer.core.model.feature;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.TimingUpdate;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Note extends Player {
    // Note specific properties 
    private int rootNote = 60; // Default to middle C (MIDI note 60)
    private String name = "Melody";
    
    // Default constructor needed for JPA
    public Note() {
        super();
    }
    
    // Convenience constructor
    public Note(String name, int rootNote) {
        super();
        this.name = name;
        this.rootNote = rootNote;
    }

    @Override
    public void onTick(TimingUpdate timingUpdate) {
 
    }
    
    // Override to handle rootNote property
    @Override
    public Integer getRootNote() {
        return rootNote;
    }
    
    @Override 
    public void setRootNote(Integer rootNote) {
        this.rootNote = rootNote;
    }
}
