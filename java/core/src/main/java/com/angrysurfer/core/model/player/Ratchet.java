package com.angrysurfer.core.model.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.util.Comparison;
import com.angrysurfer.core.util.Operator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ratchet extends Strike {

    private static final Logger logger = LoggerFactory.getLogger(Ratchet.class);

    private Strike parent;

    public Ratchet(Strike parent, double offset, long interval, int part) {
        logger.info("Creating new Ratchet - parent: {}, offset: {}, interval: {}, part: {}", 
            parent.getName(), offset, interval, part);
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
        logger.debug("Adding rule - tick: {}, part: {}", tick, part);
        getRules().add(new Rule(Operator.TICK_COUNT, Comparison.EQUALS, tick, part));

        synchronized (getTicker().getPlayers()) {
            synchronized (getTicker().getPlayers()) {
                getTicker().getPlayers().add(this);
                getTicker().getRemoveList().add(this);
            }
        }
    }

    @Override
    public void onTick(long tick, long bar) {
        logger.debug("onTick() - tick: {}, bar: {}", tick, bar);
        if (isProbable())
            drumNoteOn((long) (getNote() + getTicker().getNoteOffset()));
    }
}
