package com.angrysurfer.midi.model;

import com.angrysurfer.midi.service.IMidiInstrument;
import lombok.Getter;
import lombok.Setter;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import java.util.PriorityQueue;
import java.util.Queue;

@Getter
@Setter
public class Melody extends Player {
    private final boolean notesOfOnNext = true;
    private final Queue<Integer> notes = new PriorityQueue<>();

    public Melody(String name, Ticker ticker, IMidiInstrument instrument) {
        super(name, ticker, instrument);
    }

    @Override
    public void onTick(int tick, int bar) {

    }

    public void noteOn(int note, int velocity) {
        try {
            // clear notes queue
            while (notesOfOnNext && !notes.isEmpty())
                getInstrument().noteOff(notes.remove(), 0);

            // play new notes
            getInstrument().noteOn(note, velocity);
            if (notesOfOnNext)
                notes.add(note);

        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }
}
