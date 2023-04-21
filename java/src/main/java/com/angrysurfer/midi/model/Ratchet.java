package com.angrysurfer.midi.model;

import com.angrysurfer.midi.util.Comparison;
import com.angrysurfer.midi.util.Operator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ratchet extends Strike {

    private Strike parent;

    public Ratchet(Strike parent, double offset, long interval, int part) {
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
        setProbability(getParent().getProbability());
        setPanPosition(getParent().getPanPosition());
        setRandomDegree(getParent().getRandomDegree());
        setFadeIn(getParent().getFadeIn());
        setFadeOut(getParent().getFadeOut());
        setPreset(getParent().getPreset());

        Long ratchets = getTicker().getPlayers().stream().filter(p -> p instanceof Ratchet).count();
        setId(-1 - ratchets);
        setName(getParent().getName() + String.format("s", getParent().getTicker().getPlayers().size()));
        double tick = getTicker().getTickCount() + offset;
        getRules().add(new Rule(Operator.TICK_COUNT, Comparison.EQUALS, tick, part));

        synchronized (getTicker().getPlayers()) {
            synchronized (getTicker().getPlayers()) {
                getTicker().getPlayers().add(this);
                getTicker().getRemoveList().add(this);
            }
        }
    }

    public void onTick(long tick, long bar) {
        if (isProbable())
            drumNoteOn(getNote());
    }
}
