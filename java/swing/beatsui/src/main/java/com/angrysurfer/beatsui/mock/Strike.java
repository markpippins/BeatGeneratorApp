package com.angrysurfer.beatsui.mock;

import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Strike extends AbstractPlayer implements IPlayer {

    static Logger logger = LoggerFactory.getLogger(Strike.class.getCanonicalName());

    static final Random rand = new Random();

    public static long KICK = 36;
    public static long SNARE = 37;
    public static long CLOSED_HAT = 38;
    public static long OPEN_HAT = 39;

    public static List<Integer> razParams = List.of(16, 17, 18, 19, 20, 21, 22, 23);
    public static List<Integer> closedHatParams = List.of(24, 25, 26, 27, 28, 29, 30, 31);
    public static List<Integer> kickParams = List.of(1, 2, 3, 4, 12, 13, 14, 15);
    public static List<Integer> snarePrams = List.of(16, 17, 18, 19, 20, 21, 22, 23);

    public Strike() {
        setNote(KICK);
    }

    public Strike(String name, Ticker ticker, Instrument instrument, long note,
            List<Integer> allowedControlMessages) {
        super(name, ticker, instrument, allowedControlMessages);
        setNote(note);
    }

    public Strike(String name, Ticker ticker, Instrument instrument, long note,
            List<Integer> allowableControlMessages, long minVelocity, long maxVelocity) {
        super(name, ticker, instrument, allowableControlMessages);
        setNote(note);
        setMinVelocity(minVelocity);
        setMaxVelocity(maxVelocity);
    }

    @Override
    public void onTick(long tick, long bar) {
        // logger.info(String.format("Tick: %s", tick));
        if (tick == 1 && getSubDivisions() > 1 && getBeatFraction() > 1) {
            double numberOfTicksToWait = getBeatFraction() * (getTicker().getTicksPerBeat() / getSubDivisions());
            new Ratchet(this, numberOfTicksToWait, getRatchetInterval(), 0);
            handleRachets();
        }

        else if (getSkipCycler().getLength() == 0 || getSkipCycler().atEnd()) {
            if (getSwing() > 0)
                handleSwing();
            if (getProbability().equals(100L) || rand.nextInt(100) > getProbability())
                drumNoteOn((long) (getNote() + getTicker().getNoteOffset()));
        }

        getSkipCycler().advance();
    }

    private void handleSwing() {
        double offset = getTicker().getBeatDuration() * rand.nextLong(getSwing()) * .01;
        try {
            Thread.sleep((long) offset);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // new Ratchet(this, getTicker().getTick() + (long) offset, 1, 0);
    }

    private void handleRachets() {

        double numberOfTicksToWait = getRatchetInterval() * (getTicker().getTicksPerBeat() / getSubDivisions());

        LongStream.range(1, getRatchetCount() + 1).forEach(i -> {
            new Ratchet(this, i * numberOfTicksToWait, getRatchetInterval(), 0);
        });
    }
}