package com.angrysurfer.midi.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Melody extends Player {
    private final boolean notesOfOnNext = true;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @ElementCollection
    private final List<Integer> notes = new ArrayList<>();

    public Melody(String name, Ticker ticker, MidiInstrument instrument) {
        super(name, ticker, instrument);
    }

    public Melody() {

    }

    @Override
    public void onTick(long tick, int bar) {

    }

    public void noteOn(int note, int velocity) {
//        try {
//            // clear notes queue
//            while (notesOfOnNext && !notes.isEmpty())
//                getInstrument().noteOff(notes.remove(0), 0);
//
//            // play new notes
//            getInstrument().noteOn(note, velocity);
//            if (notesOfOnNext)
//                notes.add(note);
//
//        } catch (InvalidMidiDataException | MidiUnavailableException e) {
//            throw new RuntimeException(e);
//        }
    }
}
