package com.angrysurfer.core.model;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.sequencer.TimingUpdate;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Getter
@Setter
public class Ratchet extends Strike {

    private static final Logger logger = LoggerFactory.getLogger(Ratchet.class);

    private Player parent;
    private double targetTick;

    public Ratchet(Player parent, double offset, long interval, int part) {
        super(parent.getName() + " Ratchet", parent.getSession(), parent.getInstrument(), parent.getRootNote(), parent.getAllowedControlMessages());
        logger.info("Creating new Ratchet - parent: {}, offset: {}, interval: {}, part: {}", parent.getName(), offset,
                interval, part);

        // Set parent and session first
        setParent(parent);
        setSession(parent.getSession());

        // Now we can safely use the session
        Long ratchets = getSession().getPlayers().stream().filter(p -> p instanceof Ratchet).count() + 1;

        setId(9000 + ratchets);
        setRootNote(getParent().getRootNote());
        setInstrument(getParent().getInstrument());
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
//        setPreset(getParent().getPreset());
        setEnabled(true);

        setName(getParent().getName() + " [R]");
        targetTick = getSession().getTickCount() + offset;
        logger.debug("Adding rule - tick: {}, part: {}", targetTick, part);
        addRule(new Rule(Comparison.TICK_COUNT, Operator.EQUALS, targetTick, part));

        synchronized (getSession().getPlayers()) {
            getSession().getPlayers().add(this);
            getSession().getRemoveList().add(this);

            // Explicitly publish that this player was added
            CommandBus.getInstance().publish(Commands.PLAYER_ADDED, this, this);
            logger.info("Published PLAYER_ADDED for Ratchet: {}", getName());
        }

        CommandBus.getInstance().register(this, new String[]{Commands.TIMING_UPDATE});

        logger.info("New Ratchet created: {}", this);
        TimingBus.getInstance().register(this);
        logger.info("New Ratchet registered with TimingBus: {}", this);

        // Publish a command that a new player was added
        CommandBus.getInstance().publish(Commands.PLAYER_ADDED, this, this);
    }

    @Override
    public void onTick(TimingUpdate timingUpdate) {

        if (isProbable()) {
            noteOn((getRootNote() + (Objects.nonNull(getSession()) ? getSession().getNoteOffset() : 0)),
                    getLevel());
        }

        if (timingUpdate.tickCount() > targetTick + 1) {
            // First publish that this player is being deleted
            CommandBus.getInstance().publish(Commands.PLAYER_DELETED, this, this);
            logger.info("Published PLAYER_DELETED for Ratchet: {}", getName());

            // Then remove from session
            getSession().getPlayers().remove(this);
            CommandBus.getInstance().unregister(this);
            TimingBus.getInstance().unregister(this);
        }
    }

    public boolean shouldPlay() {
        double tick = getSession().getTick();
        return tick >= targetTick && tick < targetTick + 1;
    }

    // @Override
    // public void onAction(Command action) {
    //     if (action.getCommand() == Commands.TIMING_TICK) {
    //         // if (shouldPlay()) {
    //         //     onTick(getSession().getTick(), getSession().getBeat(), getSession().getBar(),
    //         //             getSession().getPart(), getSession().getTickCount(), getSession().getBeatCount(), getSession().getBarCount(),
    //         //             getSession().getPartCount());
    //         // }
    //     }
    // }
}
