package com.angrysurfer.core.model.feature;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.TimingUpdate;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class Note extends Player {
    // Note specific properties 
    private String name = "Melody";
    
    // Default constructor needed for JPA
    public Note() {
        super();
    }
    
    // Convenience constructor
    public Note(String name, int rootNote) {
        super();
        this.name = name;
        setRootNote(rootNote);
    }

    @Override
    public void onTick(TimingUpdate timingUpdate) {
 
    }
    
}
