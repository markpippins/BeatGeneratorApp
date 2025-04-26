package com.angrysurfer.core.model;

import java.util.List;

import com.angrysurfer.core.sequencer.TimingUpdate;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Note extends Player {
    // Note specific properties
    private String name = "Note";

    /**
     * Default constructor for JPA
     */
    // public Note() {
    // initialize();
    // }

    /**
     * Main constructor for Note with basic parameters
     */
    public Note(String name, Session session, InstrumentWrapper instrument, int note,
            List<Integer> allowedControlMessages) {
        initialize(name, session, instrument, allowedControlMessages);
        setRootNote(note);
    }

    /**
     * Extended constructor with velocity parameters
     */
    public Note(String name, Session session, InstrumentWrapper instrument, int note,
            List<Integer> allowableControlMessages, int minVelocity, int maxVelocity) {
        initialize(name, session, instrument, allowableControlMessages);
        setRootNote(note);
        setMinVelocity(minVelocity);
        setMaxVelocity(maxVelocity);
    }

    /**
     * Simplified constructor for quick note creation
     */
    // public Note(String name, int rootNote) {
    // initialize();
    // this.name = name;
    // setRootNote(rootNote);
    // }

    @Override
    public void onTick(TimingUpdate timingUpdate) {
        // Implementation details
    }
}
