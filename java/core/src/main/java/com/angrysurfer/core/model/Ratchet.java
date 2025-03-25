package com.angrysurfer.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.service.MidiClockSource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ratchet extends Strike {

    private static final Logger logger = LoggerFactory.getLogger(Ratchet.class);

    private Player parent;
    private double targetTick;

    public Ratchet(Player parent, double offset, long interval, int part) {
        logger.info("Creating new Ratchet - parent: {}, offset: {}, interval: {}, part: {}",
                parent.getName(), offset, interval, part);

        // Set parent and session first
        setParent(parent);
        setSession(parent.getSession());

        // Now we can safely use the session
        Long ratchets = getSession().getPlayers().stream()
                .filter(p -> p instanceof Ratchet)
                .count() + 1;
                
        setId(9000 + ratchets);
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
        setEnabled(true);

        setName(getParent().getName()
                + String.format(getParent().getPlayerClassName(),
                        getSession().getPlayers().size()));
        targetTick = getSession().getTickCount() + offset;
        logger.debug("Adding rule - tick: {}, part: {}", targetTick, part);
        getRules().add(new Rule(Comparison.TICK_COUNT, Operator.EQUALS, targetTick, part));

        synchronized (getSession().getPlayers()) {
            getSession().getPlayers().add(this);
            getSession().getRemoveList().add(this);
        }

        CommandBus.getInstance().register(this);
        logger.info("New Ratchet created: {}", this);
        TimingBus.getInstance().register(this);
        logger.info("New Ratchet registered with TimingBus: {}", this);
    }

    @Override
    public void onTick(long tick, long bar) {
        logger.debug("onTick() - tick: {}, bar: {}", tick, bar);
        // if (isProbable())
        drumNoteOn((long) (getNote() + getSession().getNoteOffset()));

        if (tick > targetTick + 1) {
            // Remove this ratchet after it has been played
            getSession().getPlayers().remove(this);
            CommandBus.getInstance().unregister(this);
            TimingBus.getInstance().unregister(this);
        }
    }

    public boolean shouldPlay() {
        double tick = getSession().getTick();
        return tick >= targetTick && tick < targetTick + 1;
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == Commands.TIME_TICK) {
            if (shouldPlay()) {
                onTick(getSession().getTick(), getSession().getBar());
            }
        }
    }
}
