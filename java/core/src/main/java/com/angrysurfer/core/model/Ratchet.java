package com.angrysurfer.core.model;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.util.Operator;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.service.PlayerExecutor;
import com.angrysurfer.core.service.SequencerManager;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.util.Comparison;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ratchet extends Strike {

    private static final Logger logger = LoggerFactory.getLogger(Ratchet.class);

    private Player parent;
    private double targetTick;

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
        setEnabled(true);

        Long ratchets = ((Session) getSession()).getPlayers().stream().filter(p -> p instanceof Ratchet)
                .count();
        setId(-1 - ratchets);
        setName(getParent().getName()
                + String.format(getParent().getPlayerClassName(),
                        ((Session) getParent().getSession()).getPlayers().size()));
        targetTick = getSession().getTickCount() + offset;
        logger.debug("Adding rule - tick: {}, part: {}", targetTick, part);
        getRules().add(new Rule(Comparison.TICK_COUNT, Operator.EQUALS, targetTick, part));

        synchronized (((Session) getSession()).getPlayers()) {
            synchronized (((Session) getSession()).getPlayers()) {
                ((Session) getSession()).getPlayers().add(this);
                ((Session) getSession()).getRemoveList().add(this);
            }
        }

        CommandBus.getInstance().register(this);
        logger.info("New Ratchet created: {}", this);
        TimingBus.getInstance().register(this);
        logger.info("New Ratchet registered with TimingBus: {}", this);
    }

    @Override
    public void onTick(long tick, long bar) {
        logger.debug("onTick() - tick: {}, bar: {}", tick, bar);
        if (isProbable())
            drumNoteOn((long) (getNote() + getSession().getNoteOffset()));
    }

    public boolean shouldPlay() {
        double tick = SequencerManager.getInstance().getCurrentTick();
        boolean result = tick >= targetTick;
        if (result) {
            // Remove this ratchet from the session
            // SessionManager.getInstance().getCurrentSession().getPlayers().remove(this);
            // setEnabled(false);
            // TimingBus.getInstance().unregister(this);
            // CommandBus.getInstance().unregister(this);
        }
        return result;
    }
}
