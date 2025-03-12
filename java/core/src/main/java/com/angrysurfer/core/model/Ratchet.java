package com.angrysurfer.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.util.Operator;
import com.angrysurfer.core.util.Comparison;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ratchet extends Strike {

    private static final Logger logger = LoggerFactory.getLogger(Ratchet.class);

    private Player parent;

    public Ratchet(Player parent, double offset, long interval, int part) {

        logger.info("Creating new Ratchet - parent: %s, offset: %d, interval: %d, part: %d",
                parent.getName(), offset, interval, part);

        setParent(parent);
        setSession(getParent().getSession());
        setNote(getParent().getNote());
        setInstrument(getParent().getInstrument());
        setChannel(getParent().getChannel());
        setSubDivisions(getParent().getSubDivisions());
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

        Long ratchets = ((Session) getSession()).getPlayers().stream().filter(p -> p instanceof Ratchet)
                .count();
        setId(-1 - ratchets);
        setName(getParent().getName()
                + String.format("s", ((Session) getParent().getSession()).getPlayers().size()));
        double tick = getSession().getTickCount() + offset;
        logger.debug("Adding rule - tick: {}, part: {}", tick, part);
        getRules().add(new Rule(Comparison.TICK_COUNT, Operator.EQUALS, tick, part));

        synchronized (((Session) getSession()).getPlayers()) {
            synchronized (((Session) getSession()).getPlayers()) {
                ((Session) getSession()).getPlayers().add(this);
                ((Session) getSession()).getRemoveList().add(this);
            }
        }
    }

    @Override
    public void onTick(long tick, long bar) {
        logger.debug("onTick() - tick: {}, bar: {}", tick, bar);
        if (isProbable())
            drumNoteOn((long) (getNote() + getSession().getNoteOffset()));
    }
}
