package com.angrysurfer.midi.model;

import com.angrysurfer.midi.model.condition.Condition;
import com.angrysurfer.midi.model.condition.Operator;
import com.angrysurfer.midi.service.IMidiInstrument;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@MappedSuperclass
public abstract class Player implements Callable<Boolean>, Serializable {

    static final Random rand = new Random();
    @Transient
    List<Condition> conditions = new ArrayList<>();
    private int note;
    private int minVelocity = 110;
    private int maxVelocity = 127;
    @JsonIgnore
    private boolean even = true;

    private boolean muted = false;
    @JsonIgnore
    @Transient
    private Double beat = 1.0;
    @JsonIgnore
    @Transient
    private int position = 0;
    @JsonIgnore
    @Transient
    private AtomicInteger lastTick = new AtomicInteger(0);
    private int preset;
    @JsonIgnore
    @Transient
    private IMidiInstrument instrument;
    @JsonIgnore
    @Transient
    private Ticker ticker;
    @Transient
    private List<Integer> allowedControlMessages = new ArrayList<>();
    @JsonIgnore
    @Transient
    private int lastPlayedTick;
    @JsonIgnore
    @Transient
    private int lastPlayedBar;
    @JsonIgnore
    @Transient
    private Double lastPlayedBeat;
    private String name;

    public Player() {

    }

    public Player(String name, Ticker ticker, IMidiInstrument instrument) {
        setName(name);
        setInstrument(instrument);
        setTicker(ticker);
    }

    public Player(String name, Ticker ticker, IMidiInstrument instrument, List<Integer> allowedControlMessages) {
        this(name, ticker, instrument);
        setAllowedControlMessages(allowedControlMessages);
    }

    public abstract Long getId();

    public abstract void onTick(int tick, int bar);

    @Override
    public Boolean call() {
        setEven(!even);
        if (getLastTick().get() == getTicker().getTick())
            return Boolean.FALSE;
        int tick = getTicker().getTick();
        int bar = getTicker().getBar();
        double beatOffset = 1.0;
        getLastTick().set(tick);

        if (shouldFire(tick, bar) &&
                !isMuted() &&
                getTicker().getMuteGroups().stream().noneMatch(g -> g.getPlayers()
                        .stream().filter(e -> e.getLastPlayedTick() == tick)
                        .toList().size() > 0))
            onTick(tick, bar);

        setBeat(getBeat() + getTicker().getBeatDivision());
        if (getBeat() >= getTicker().getBeatsPerBar() + beatOffset)
            setBeat(beatOffset);

        return Boolean.TRUE;
    }

    public void drumNoteOn(int note, int velocity) {
        noteOn(note, velocity);
        noteOff(note, velocity);
    }

    public void noteOn(int note, int velocity) {
        try {
            getInstrument().noteOn(note, velocity);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void noteOff(int note, int velocity) {
        try {
            getInstrument().noteOff(note, velocity);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    protected int incrementAndGetPosition() {
        return ++position;
    }

    @JsonIgnore
    public String getInstrumentName() {
        return getInstrument().getName();
    }

    @JsonIgnore
    public Integer getChannel() {
        return getInstrument().getChannel();
    }

    @JsonIgnore
    public boolean shouldFire(int tick, int bar) {
        AtomicBoolean play = new AtomicBoolean(getConditions().size() > 0);
        getConditions().forEach(condition -> {
            switch (condition.getOperator()) {
                case TICK -> {
                    if (!condition.getComparison().evaluate(tick, condition.getValue()))
                        play.set(false);
                }
                case BEAT -> {
                    if (!condition.getComparison().evaluate(getBeat(), condition.getValue()))
                        play.set(false);
                }
                case BAR -> {
                    if (!condition.getComparison().evaluate(bar, condition.getValue()))
                        play.set(false);
                }
                case POSITION -> {
                    if (!condition.getComparison().evaluate(getPosition(), condition.getValue()))
                        play.set(false);
                }
            }
        });
        return play.get();
    }
}
