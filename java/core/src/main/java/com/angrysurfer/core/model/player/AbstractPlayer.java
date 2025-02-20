package com.angrysurfer.core.model.player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.IPlayer;
import com.angrysurfer.core.model.Instrument;
import com.angrysurfer.core.model.Pad;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.util.Comparison;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.core.util.Cycler;
import com.angrysurfer.core.util.Operator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter

@MappedSuperclass
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AbstractPlayer implements Serializable, IPlayer {

    static final Random rand = new Random();

    static Logger logger = LoggerFactory.getLogger(AbstractPlayer.class.getCanonicalName());

    @Transient
    private Set<Pad> pads = new HashSet<>();

    private String name;
    private int channel;
    private Long swing = 0L;
    private Long level = 100L;
    private Long note = 60L;
    private Long minVelocity = 100L;
    private Long maxVelocity = 110L;
    private Long preset = 1L;
    private Boolean stickyPreset = false;
    private Long probability = 100L;
    private Long randomDegree = 0L;
    private Long ratchetCount = 0L;
    private Long ratchetInterval = 1L;
    private Integer internalBars  = Constants.DEFAULT_BAR_COUNT;
    private Integer internalBeats = Constants.DEFAULT_BEATS_PER_BAR;
    private Boolean useInternalBeats = false;
    private Boolean useInternalBars = false;
    private Long panPosition = 63L;
    private Boolean preserveOnPurge = false;
    private double sparse;

    // private Set<Long> tickRange = new HashSet<>();

    @JsonIgnore
    @Transient
    private Cycler skipCycler = new Cycler(0);

    @JsonIgnore
    @Transient
    private Cycler subCycler = new Cycler(16);

    @JsonIgnore
    @Transient
    private Cycler beatCycler = new Cycler(16);

    @JsonIgnore
    @Transient
    private Cycler barCycler = new Cycler(16);

    @Transient
    private boolean solo = false;

    @Transient
    private boolean muted = false;

    @Transient
    private Long position;

    @Transient
    private Long lastTick = 0L;

    @Transient
    private Long lastPlayedTick = 0L;

    @Transient
    private Long lastPlayedBar;

    @Transient
    private Long skips = 0L;

    @Transient
    private double lastPlayedBeat;

    private Long subDivisions = 4L;

    private Long beatFraction = 1L;

    private Long fadeOut = 0L;

    private Long fadeIn = 0L;

    private Boolean accent = false;

    @JsonIgnore
    @Transient
    private boolean unsaved = false;

    @JsonIgnore
    @Transient
    private Boolean armForNextTick = false;

    @Transient
    private Set<Rule> rules = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "allowedControlMessages")
    private List<Integer> allowedControlMessages = new ArrayList<>();

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    public AbstractPlayer() {

    }

    public AbstractPlayer(String name, Ticker ticker, Instrument instrument) {
        setName(name);
        setInstrument(instrument);
        setTicker(ticker);
    }

    public AbstractPlayer(String name, Ticker ticker, Instrument instrument, List<Integer> allowedControlMessages) {
        this(name, ticker, instrument);
        setAllowedControlMessages(allowedControlMessages);
    }

    // public Long getNote() {
    // Long result = randomDegree == 0L ? note : rand.nextBoolean() ? note +
    // rand.nextLong(randomDegree) : note - rand.nextLong(randomDegree);

    // if (result > getInstrument().getHighestNote())
    // result = getInstrument().getLowestNote() + rand.nextLong(randomDegree);

    // if (result < getInstrument().getLowestNote())
    // result = getInstrument().getHighestNote() - rand.nextLong(randomDegree);

    // return (long) (result + getTicker().getNoteOffset());
    // }

    @Override
    public String getPlayerClass() {
        return getClass().getSimpleName();
    }

    @Override
    public Long getSubPosition() {
        return getSubCycler().get();
    }

    // @Override
    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    @Override
    public abstract void onTick(long tick, long bar);

    @Override
    public Long getInstrumentId() {
        return (Objects.nonNull(getInstrument()) ? getInstrument().getId() : null);
    }

    // @Override
    public Rule getRule(Long ruleId) {
        return getRules().stream().filter(r -> r.getId().equals(ruleId)).findAny().orElseThrow();
    }

    // public int getChannel() {
    // return Objects.nonNull(instrument) ? instrument.getChannel() : 0;
    // }

    @Override
    public void drumNoteOn(long note) {
        logger.debug("drumNoteOn() - note: {}", note);
        long velocity = (long) ((getLevel() * 0.01)
                * rand.nextLong(getMinVelocity() > 0 ? getMinVelocity() : 100,
                        getMaxVelocity() > getMinVelocity() ? getMaxVelocity() : 126));
        noteOn(note, velocity);
        try {
            Thread.sleep(2500);
            noteOff(note, velocity);
        } catch (InterruptedException e) {
            logger.error("Error in drumNoteOn: {}", e.getMessage(), e);
        }
    }

    @Override
    public void noteOn(long note, long velocity) {
        logger.debug("noteOn() - note: {}, velocity: {}", note, velocity);
        try {
            getInstrument().noteOn(getChannel(), note, velocity);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            logger.error("Error in noteOn: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void noteOff(long note, long velocity) {
        logger.debug("noteOff() - note: {}, velocity: {}", note, velocity);
        try {
            getInstrument().noteOff(getChannel(), note, velocity);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            logger.error("Error in noteOff: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean call() {
        logger.debug("call() - lastTick: {}, currentTick: {}", getLastTick(), getTicker().getTick());
        if (getLastTick() == getTicker().getTick())
            return Boolean.FALSE;
        // && !muteGroupPartnerSoundedOnThisTick()
        // && strikeHasNoMuteGroupConflict()
        var solo = getTicker().hasSolos();

        if ((!solo && !isMuted()) || (solo && isSolo()) && shouldPlay()) {
            logger.debug("Player {} will play on tick {}", getName(), getTicker().getTick());
            getTicker().getActivePlayerIds().add(getId());
            setLastPlayedBar(getTicker().getBar());
            setLastPlayedBeat(getTicker().getBeat());
            setLastPlayedTick(getTicker().getTick());
            onTick(getTicker().getTick(), getTicker().getBar());
        }

        setLastTick(getTicker().getTick());
        // logger.info(String.format("%s not playing tick %s, beat %s, bar %s",
        // getName(), tick, getTicker().getBeat(), getTicker().getBar()));
        return Boolean.TRUE;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isProbable() {
        long test = rand.nextLong(101);
        long probable = getProbability();
        boolean result = test < probable;
        return result;
    }

    private boolean strikeHasNoMuteGroupConflict() {
        return getTicker().getMuteGroups().stream().noneMatch(g -> g.getPlayers()
                .stream().filter(e -> e.getLastPlayedTick() == getTicker().getTick())
                .toList().size() == 0);
    }

    private Set<Rule> filterByPart(Set<Rule> rules, boolean includeNoPart) {
        return rules.stream()
                .filter(r -> r.getPart() == 0
                        || (includeNoPart && ((long) r.getPart()) == getTicker().getPart()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean shouldPlay() {
        logger.debug("shouldPlay() - evaluating rules for player: {}", getName());
        Set<Rule> applicable = filterByPart(getRules(), true);

        AtomicBoolean play = new AtomicBoolean(true);
        AtomicBoolean hasTick = new AtomicBoolean(false);
        AtomicBoolean hasBeat = new AtomicBoolean(false);
        AtomicBoolean hasBar = new AtomicBoolean(false);

        long tick = getTicker().getTick();
        long bar = getTicker().getBar();
        double beat = getTicker().getBeat();
        long fractionLength = getTicker().getTicksPerBeat() / getSubDivisions();
        AtomicLong beatFraction = new AtomicLong(0L);
        if (getBeatFraction() > 1)
            LongStream.range(1L, getBeatFraction()).forEach(f -> beatFraction.addAndGet(fractionLength));

        applicable.forEach(rule -> {
            switch (rule.getOperator()) {
                case Operator.TICK -> {
                    if (Comparison.evaluate(rule.getComparison(), tick, rule.getValue()))
                        hasTick.set(true);
                    // logger.info(String.format("HasTick: %s", hasTick.get()));
                    break;
                }

                case Operator.BEAT -> {
                    if (Comparison.evaluate(rule.getComparison(), beat, rule.getValue()))
                        hasBeat.set(true);
                    // logger.info(String.format("HasBeat: %s", hasBeat.get()));
                    break;
                }

                case Operator.BAR -> {
                    if (Comparison.evaluate(rule.getComparison(), bar, rule.getValue()))
                        hasBar.set(true);
                    // logger.info(String.format("HasBar: %s", hasBar.get()));
                    // play.set(false);
                    break;
                }

                // case Operator.PART -> {
                // if (!Comparison.evaluate(rule.getComparison(), getTicker().getPart(),
                // rule.getValue()))
                // play.set(false);
                // }

                case Operator.BEAT_DURATION -> {
                    if (!Comparison.evaluate(rule.getComparison(), beat, rule.getValue()))
                        play.set(false);
                    break;
                }

                case Operator.TICK_COUNT -> {
                    if (!Comparison.evaluate(rule.getComparison(), getTicker().getTickCounter().get(),
                            rule.getValue()))
                        play.set(false);
                }

                case Operator.BEAT_COUNT -> {
                    if (!Comparison.evaluate(rule.getComparison(), getTicker().getBeatCounter().get(),
                            rule.getValue()))
                        play.set(false);
                }

                case Operator.BAR_COUNT -> {
                    if (!Comparison.evaluate(rule.getComparison(), getTicker().getBarCounter().get(),
                            rule.getValue()))
                        play.set(false);
                }

                case Operator.PART_COUNT -> {
                    if (!Comparison.evaluate(rule.getComparison(), getTicker().getPartCounter().get(),
                            rule.getValue()))
                        play.set(false);
                }
            }
        });

        getSkipCycler().advance();
        getSubCycler().advance();
        boolean result = (hasTick.get() && hasBeat.get() && hasBar.get() && play.get())
                || (tick == 1 && (hasTick.get() || hasBeat.get()));
        logger.debug("shouldPlay() result: {} for player: {}", result, getName());
        return result;
    }

}
