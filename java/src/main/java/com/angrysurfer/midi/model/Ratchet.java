package com.angrysurfer.midi.model;

import com.angrysurfer.midi.util.Comparison;
import com.angrysurfer.midi.util.Operator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ratchet extends Strike {
 
    private Strike parent;

    public Ratchet(Strike parent, long offset, long interval, int part) {
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
        Long ratchets = getTicker().getPlayers().stream().filter(p -> p instanceof Ratchet).count();
        setId(-1 - ratchets);
        setName(getParent().getName() + String.format("s", getParent().getTicker().getPlayers().size()));
        getRules().add(new Rule(Operator.TICK, Comparison.EQUALS, (double) interval * offset, part));

        synchronized (getTicker().getPlayers()) {
            synchronized (getTicker().getPlayers()) {
                getTicker().getPlayers().add(this);
                getTicker().getRemoveList().add(this);
            }
        }
    }

    public void onTick(long tick, long bar) {
        drumNoteOn(getNote(), rand.nextLong(getMinVelocity() > 0 ? getMinVelocity() : 100, getMaxVelocity() > getMinVelocity() ? getMaxVelocity() : 126));
    }
}
