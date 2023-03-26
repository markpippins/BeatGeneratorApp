package com.angrysurfer.midi.model;

import com.angrysurfer.midi.util.Comparison;
import com.angrysurfer.midi.util.Operator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ratchet extends Strike {
 
    private Strike parent;

    public Ratchet(Strike parent, long offset, int interval, int part) {
        setParent(parent);
        setTicker(getParent().getTicker());
        setNote(getParent().getNote());
        setInstrument(getParent().getInstrument());
        setAllowedControlMessages(getParent().getAllowedControlMessages());
        setPads(getParent().getPads());
        setLevel(getParent().getLevel());
        setMaxVelocity(getParent().getMaxVelocity());
        setMinVelocity(getParent().getMinVelocity());
        setMuted(getParent().isMuted());
        setId((long) 1000 + getTicker().getPlayers().size());
        setName(getParent().getName() + String.format("s", getParent().getTicker().getPlayers().size()));
        synchronized (getTicker().getPlayers()) {
            getTicker().getPlayers().add(this);
        }
        getRules().add(new Rule(Operator.TICK, Comparison.EQUALS, (double) interval * offset, part));
    }

    public void onTick(long tick, long bar) {
        drumNoteOn(getNote(), rand.nextInt(getMinVelocity(), getMaxVelocity()));
        synchronized (getTicker().getPlayers()) {
            getTicker().getPlayers().remove(this);
        }
    }
}
