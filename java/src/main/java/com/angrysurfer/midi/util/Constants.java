package com.angrysurfer.midi.util;

public class Constants {
    public static final double DEFAULT_BEAT_OFFSET = 1.0;
    public static int DEFAULT_PPQ = 24;
    public static int DEFAULT_BAR_COUNT = 4;
    public static int DEFAULT_BEATS_PER_BAR = 4;
    public static int DEFAULT_BEAT_DIVIDER = 16;
    public static float DEFAULT_BPM = 88;
    public static long DEFAULT_PART_LENGTH = 1L;
    public static int DEFAULT_MAX_TRACKS = 16;
    public static long DEFAULT_SONG_LENGTH = Long.MAX_VALUE;
    public static long DEFAULT_SWING = 25L;
    
    public static final int DEFAULT_LOOP_COUNT = 0;
    public static final int DEFAULT_PART_COUNT = 1;

    public static final String INSTRUMENT = "/instrument";
    public static final String INSTRUMENT_LIST = "/instruments/all";
    public static final String INSTRUMENT_INFO = "/instrument/info";
    public static final String INSTRUMENT_LOOKUP = "/instruments/lookup";
    public static final String INSTRUMENT_NAMES = "/instruments/names";

    public static final String SAVE_CONFIG = "/instruments/save";
    
    public static final String DEVICES_INFO = "/devices/info";
    public static final String DEVICE_NAMES = "/devices/names";

    public static final String SERVICE_RESET = "/service/reset";
    public static final String SERVICE_SELECT = "/service/select";
	
    public static final String RULES_FOR_PLAYER = "/player/rules";


    public static final String ALL_PLAYERS =  "/players/info";
    public static final String CLEAR_PLAYERS =  "/players/clear";
    public static final String CLEAR_PLAYERS_WITH_NO_RULES = "/players/clearnorules";
    public static final String ADD_PLAYER = "/players/add";
    public static final String ADD_PLAYER_FOR_NOTE = "/players/note";
    public static final String REMOVE_PLAYER = "/players/remove";
	public static final String UPDATE_PLAYER ="/player/update";
	public static final String MUTE_PLAYER = "/players/mute";

    public static final String ADD_STEP = "/steps/add";
    public static final String REMOVE_STEP = "/steps/remove";
	public static final String UPDATE_STEP ="/steps/update";
	public static final String MUTE_STEP = "/steps/mute";

    public static final String SONG_INFO ="/song/info";
    public static final String ADD_SONG ="/songs/new";
    public static final String DELETE_SONG = "/songs/remove";
	public static final String PREV_SONG = "/songs/previous";
    public static final String NEXT_SONG = "/songs/next";
    public static final String UPDATE_SONG = "/song/update";
    public static final String LOAD_SONG = "/song/load";
    public static final String NEW_SONG = "/song/new";
    public static final String REMOVE_SONG = "/song/remove";

    public static final String ADD_TICKER ="/ticker/new";
    public static final String DELETE_TICKER = "/rules/remove";
	public static final String PREV_TICKER = "/ticker/previous";
    public static final String NEXT_TICKER = "/ticker/next";
    public static final String UPDATE_TICKER = "/ticker/update";

    public static final String START_TICKER = "/ticker/start";
    public static final String STOP_TICKER = "/ticker/stop";
    public static final String PAUSE_TICKER = "/ticker/pause";
	public static final String LOAD_TICKER = "/ticker/load";
    public static final String TICKER_STATUS = "/ticker/status";
    public static final String TICKER_INFO = "/ticker/info";
    public static final String TICKER_LOG = "/ticker/log";

    public static final String ADD_RULE = "/rules/add";
    public static final String REMOVE_RULE = "/rules/remove";
	public static final String UPDATE_RULE = "/rule/update";
    public static final String SPECIFY_RULE = "/rules/specify";

    public static final String GET_INSTRUMENT_BY_CHANNEL = "/midi/instrument";
    public static final String GET_INSTRUMENT_BY_ID = "/instrument";

    public static final String SEND_MESSAGE = "/messages/send";
    public static final String SAVE_BEAT = "/beat/save";
	public static final String PLAY_DRUM_NOTE = "/drums/note";
    public static final String PLAY_SEQUENCE = "/sequence/play";
}


// package com.angrysurfer.midi.model;

// import lombok.Getter;
// import lombok.Setter;

// import java.util.*;

// @Getter
// @Setter
// public class BeatGenerator extends Ticker {




//     public BeatGenerator() {
//         super();
//     }
//     @Override
//     public void onBarChange(int bar) {
//         if (!getRemoveList().isEmpty()) {
//     }

//     @Override
//     public void onBeatChange(long beat) {

//     }
// }


    // public MidiInstrument getInstrument(String name) {
    //     return getInstrumentMap().get(name);
    // }

    // public BeatGenerator(Map<String, MidiInstrument> instrumentMap) {
    //     super();
    //     setInstrumentMap(instrumentMap);
    // }

    //    public void makeBeats() {
//
//        MidiInstrument fireball = getInstrument("Fireball");
//        MidiInstrument zero = getInstrument("Zero");
//
//        getPlayers().clear();
//
//        Stream.of(new Strike("blackbox-kick", this, getInstrument("blackbox"), KICK, kickParams, 100, 125)
//                                .addRule(new Rule(BEAT, MODULO, 2.0)),
//                        new Strike("raz-kick", this, getInstrument(RAZZ), SNARE, snarePrams, 100, 125)
//                                .addRule(new Rule(BEAT, EQUALS, rand.nextInt(1, 4) * .25)),
//                        new Strike("raz-closed-hat", this, getInstrument(RAZZ), CLOSED_HAT, closedHatParams, 100, 125)
//                                .addRule(new Rule(BEAT, MODULO, 3.0)),
//                        new Strike("blackbox-open-hat", this, getInstrument("blackbox"), OPEN_HAT, closedHatParams, 100, 125)
//                                .addRule(new Rule(BEAT, EQUALS, rand.nextInt(4, 6) * .25))
//                                .addRule(new Rule(TICK, MODULO, 3.0)),
//                        new Strike("blackbox-snare", this, getInstrument("blackbox"), SNARE, snarePrams, 100, 125)
//                                .addRule(new Rule(BEAT, MODULO, rand.nextInt(1, 4) * .25)),
//                        new Strike("raz-snare", this, getInstrument(RAZZ), SNARE, snarePrams, 100, 125)
//                                .addRule(new Rule(BEAT, MODULO, rand.nextInt(1, 4) * .25)),
//                        new Strike("raz-closed-hat", this, getInstrument(RAZZ), CLOSED_HAT, closedHatParams, 100, 125)
//                                .addRule(new Rule(BEAT, EQUALS, rand.nextInt(2, 8) * .25)),
//                        new Strike("blackbox-open-hat", this, getInstrument("blackbox"), OPEN_HAT, closedHatParams, 100, 125)
//                                .addRule(new Rule(BEAT, MODULO, rand.nextInt(4, 6) * .25))
//                                .addRule(new Rule(TICK, EQUALS, rand.nextInt(6, 8) * .25))).filter(h -> rand.nextBoolean())
//                .forEach(getPlayers()::add);
//        getPlayers().forEach(p -> {
//            p.getRules().forEach(c -> c.setId(conditionsCounter.incrementAndGet()));
//        });
//
//        setBeatDivider(rand.nextInt(1, 4));
//
//        try {
//            int razPreset = rand.nextInt(126);
//            getInstrument(RAZZ).programChange(razPreset, 127);
//            getPlayers().stream().filter(p -> p.getInstrument().equals(getInstrument(RAZZ)))
//                    .forEach(p -> p.setPreset(razPreset));
//
//            int microFreakPreset = rand.nextInt(126);
//            getInstrument(MICROFREAK).programChange(microFreakPreset, 127);
//            getPlayers().stream().filter(p -> p.getInstrument().equals(getInstrument(MICROFREAK)))
//                    .forEach(p -> p.setPreset(microFreakPreset));
//        } catch (InvalidMidiDataException | MidiUnavailableException e) {
//            throw new RuntimeException(e);
//        }
//    }

//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, CLOSED_HAT, closedHatParams, 100, 125)
//                    .addRule(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, CLOSED_HAT, closedHatParams, 100, 125)
//                    .addRule(TICK, MODULO, nextValue(4))
//                    .addRule(BAR, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, SNARE, snarePrams, 100, 125)
//                    .addRule(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, fireball, 51, fireballParams, 100, 125)
//                .addRule(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, microfreak, 39, microFreakParams, 100, 125)
//                .addRule(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, microfreak, 53, microFreakParams, 100, 125)
//                .addRule(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, zero, 49, Collections.emptyList(), 100, 125)
//                .addRule(TICK, MODULO, nextValue(4))
//                .addRule(BEAT, LESS_THAN, nextValue(4)));

//        result.add(new DrumPad(ticker, raz, CLOSED_HAT, kickParams, 100, 125)
//                .addRule(TICK, MODULO, 2.0));
//

//        IntStream.range(36, 44).forEach(drum -> {
//            IntStream.range(EQUALS, MODULO + 1).forEach(op -> {
//                IntStream.range(TICK, BAR + 1).forEach(div -> {
//                    while (rand.nextBoolean()) {
//                        DrumPad pad = new DrumPad(ticker, raz, drum, Collections.emptyList(), 100, 125);
//                        pad.addRule(rand.nextInt(MODULO + 1), rand.nextInt(BAR + 1), nextValue(4));
//                        result.add(pad);
//                    }
//                });
//            });
//        });
//        if (rand.nextBoolean())
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, blackbox, KICK, kickParams, 100, 125)
//                    .addRule(BEAT, GREATER_THAN, nextValue(4))
//                    .addRule(BEAT, LESS_THAN, nextValue(4))
//                    .addRule(BAR, MODULO, nextValue(4))
//                    .addRule(TICK, MODULO, nextValue(4)));
////
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, KICK, kickParams, 100, 125)
//                .addRule(BAR, LESS_THAN, nextValue(4))
//                .addRule(BAR, GREATER_THAN, nextValue(4))
//                .addRule(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, blackbox, KICK, kickParams, 100, 125)
//                .addRule(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, SNARE, snarePrams, 100, 125)
//                .addRule(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, SNARE, snarePrams, 100, 125)
//                .addRule(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, blackbox, SNARE, snarePrams, 100, 125)
//                .addRule(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, CLOSED_HAT, closedHatParams, 100, 125)
//                .addRule(TICK, MODULO, nextValue(4)));

//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, OPEN_HAT, List.of(24, 25, 26, 27, 28, 29, 30, 31), 100, 125)
//                    .addRule(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, OPEN_HAT, List.of(32, 33, 34, 35, 36, 37, 38, 39), 100, 125)
//                    .addRule(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, 41, razParams, 100, 125)
//                .addRule(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, CLOSED_HAT, razParams, 100, 125)
//                .addRule(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, OPEN_HAT, razParams, 100, 125).addRule(TICK, MODULO, nextValue(4)));

//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, fireball, 51, fireballParams, 100, 125)
//                .addRule(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, microfreak, 39, microFreakParams, 100, 125)
//                .addRule(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, microfreak, 53, microFreakParams, 100, 125)
//                .addRule(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, zero, 49, Collections.emptyList(), 100, 125)
//                .addRule(TICK, MODULO, nextValue(4))
//                .addRule(BEAT, LESS_THAN, nextValue(4)));

//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, zero, 51, Collections.emptyList(), 100, 125)
//                .addRule(TICK, MODULO, nextValue(4)));

//                        new DrumPad(ticker, raz, 37, List.of(16, 17, 18, 19, 20, 21, 22, 23)) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (rand.nextBoolean() && tick % 8 == 0 && bar % 2 == 0)
//                                    getInstrument().randomize(getAllowedControlMessages());
//
//                                if (bar > 8 && bar < 24 || bar > 48)
//                                    if (((tick % 16 == 0))
//                                            || (bar % 7 == 0 && rand.nextBoolean() && tick % 12 == 0 && tick % 36 != 0))
//                                        drumNoteOn(getNote(), rand.nextInt(90, 110));
//                            }
//                        },
// open hi-hat
//                        new DrumPad(ticker, raz, 38, List.of(24, 25, 26, 27, 28, 29, 30, 31)) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (rand.nextBoolean() && tick % 8 == 0 && bar % 2 == 0)
//                                    getInstrument().randomize(getAllowedControlMessages());
//
//                                if (tick % 4 == 0 || (rand.nextBoolean() && tick % 3 == 0))
//                                    drumNoteOn(getNote(), rand.nextInt(75, 127));
//                            }
//                        },
//                        // closed hi-hat
//                        new DrumPad(ticker, raz, 39, List.of(32, 33, 34, 35, 36, 37, 38, 39)) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (rand.nextBoolean() && tick % 8 == 0 && bar % 2 == 0)
//                                    getInstrument().randomize(getAllowedControlMessages());
//
//                                if (bar > 32 && tick % 16 == 0 && rand.nextBoolean())
//                                    drumNoteOn(getNote(), rand.nextInt(120, 127));
//                            }
//                        },
//
//                        new SamplePlayer(ticker, blackbox, 40, 52),
//
//                        new NotePlayer(ticker, new MidiInstrument("Zero", device, 4)) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (getPosition() == notes.length)
//                                    setPosition(0);
//
//                                if (bar > 4)
//                                    if (getBeat() % .25 == 0) {
//                          t              int note = notes[getPosition()];
//                                        noteOn(note, 127);
//                                    }
//                            }
//                        },
//
//                        new NotePlayer(ticker, new MidiInstrument("West Pest", device, 5)) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (getPosition() == notes.length)
//                                    setPosition(0);
//
//                                if (tick % 2 == 0) {
//                                    int note = notes[incrementAndGetPosition()] - 24;
//                                    noteOn(note, 127);
//                                }
//                            }
//                        },
//
//                        new NotePlayer(ticker, new MicroFreak(device, 2)) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (getPosition() == notes.length)
//                                    setPosition(0);
//
//                                if (rand.nextBoolean()) {
//                                    try {
//                                        getInstrument().programChange(rand.nextInt(), 127);
//                                    } catch (InvalidMidiDataException | MidiUnavailableException e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                }
//                                if (getPosition() == notes.length)
//                                    setPosition(0);
//
//                                if (tick % 2 == 0) {
//                                    int note = notes[incrementAndGetPosition()] - 24;
//                                    noteOn(note, 127);
//                                }
//                            }
//                        },
//
//                        // fireball
//                        new NotePlayer(ticker, new Fireball(device, 3)) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (rand.nextBoolean() && tick % 8 == 0 && bar % 2 == 0)
//                                    getInstrument().randomize(getAllowedControlMessages());
//
//                                if (getBeat() % 2 == 0)
//                                    noteOn(33 + (rand.nextBoolean() ? 12 * rand.nextInt(0, 2) : 0), 127);
//                            }
//                        },
//
//                        new DrumPad(ticker, blackbox, 36, List.of()) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (tick % 8 == 0 || (rand.nextBoolean() && (tick % 6 == 0 || tick % 14 == 0)))
//                                    drumNoteOn(getNote(), rand.nextInt(100, 110));
//                            }
//                        },
//
//                        new DrumPad(ticker, blackbox, 37, List.of()) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (bar < 8 || (bar > 24 && bar < 32) || bar > 48)
//                                    if ((tick % 16 == 0)
//                                            || (bar % 7 == 0 && rand.nextBoolean() && tick % 12 == 0 && tick % 36 != 0))
//                                        drumNoteOn(getNote(), rand.nextInt(90, 110));
//                            }
//                        },
//
//                        new DrumPad(ticker, blackbox, 39, List.of()) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (bar > 8 && bar < 17 || bar > 32 && bar < 48 && tick % 16 == 0 && rand.nextBoolean())
//                                    drumNoteOn(getNote(), rand.nextInt(120, 127));
//                            }
//                        },
//
//                        new DrumPad(ticker, blackbox, 38, List.of()) {
//                            @Override
//                            public void onTick(int tick, int bar) {
//                                if (tick % 4 == 0 || (rand.nextBoolean() && tick % 3 == 0))
//                                    drumNoteOn(getNote(), rand.nextInt(75, 127));
//                            }
//                        }

//    @Override
//    public double getBeatDivision() {
//        return rand.nextBoolean() ? getBeatDivision() :
//                rand.nextBoolean() ? .125 :
//                        rand.nextBoolean() ? .25 :
//                                rand.nextBoolean() ? .5 :
//                                        rand.nextBoolean() ? .75 : 1.0;
//    }

// package com.angrysurfer.midi.model;

// import jakarta.persistence.*;
// import lombok.Getter;
// import lombok.Setter;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import java.util.ArrayList;
// import java.util.List;

// @Getter
// @Setter
// @Entity
// public class TickerInfo {
//     static Logger logger = LoggerFactory.getLogger(TickerInfo.class.getCanonicalName());
//     public boolean done;
//     //    @OneToMany(fetch = FetchType.EAGER)
// //    @JoinTable(name = "ticker_player", joinColumns = {@JoinColumn(name = "ticker_id")}, inverseJoinColumns = {
// //            @JoinColumn(name = "player_id")})
//     @Transient
//     private List<PlayerInfo> players = new ArrayList<>();
//     @Id
//     @GeneratedValue(strategy = GenerationType.SEQUENCE)
//     @Column(name = "id", nullable = false)
//     private Long id;
//     private int bar;
//     private long tick;
//     private int ticksPerBeat;
//     private int beatsPerBar;
//     private int beat;
//     private double beatDivider;
//     private float tempoInBPM;
//     private int partLength;
//     private int maxTracks;
//     private int songLength;
//     private int swing;
//     private boolean playing;
//     private boolean stopped;
//     @Transient
//     private MuteGroupList muteGroups;

//     public TickerInfo() {
//         setBeat(1);
//         setBar(1);
//         setTick(1);
//         setDone(false);
//         setPlaying(false);
//         setStopped(false);
//         setId(null);
//         setSwing(Constants.DEFAULT_SWING);
//         setMaxTracks(Constants.DEFAULT_MAX_TRACKS);
//         setPartLength(Constants.DEFAULT_PART_LENGTH);
//         setSongLength(Constants.DEFAULT_SONG_LENGTH);
//         setTempoInBPM(Constants.DEFAULT_BPM);
//         setBeatDivider(Constants.DEFAULT_BEAT_DIVIDER);
//         setBeatsPerBar(Constants.DEFAULT_BEATS_PER_BAR);
//         setTicksPerBeat(Constants.DEFAULT_PPQ);
// //        setMuteGroups();
//     }

//     public static void copyToTicker(TickerInfo info, Ticker ticker) {
//         ticker.setId(info.getId());
//         ticker.setBar(info.getBar());
//         ticker.setBeat(info.getBeat());
//         ticker.setDone(info.isDone());
//         ticker.setBeatDivider(info.getBeatDivider());
//         ticker.setMaxTracks(info.getMaxTracks());
//         ticker.setSwing(info.getSwing());
//         ticker.setSongLength(info.getSongLength());
//         ticker.setPartLength(info.getPartLength());
//         ticker.setTicksPerBeat(info.getTicksPerBeat());
//         ticker.setPartLength(info.getPartLength());
//         ticker.setBeatsPerBar(info.getBeatsPerBar());
//     }

//     public static void copyFromTicker(Ticker ticker, TickerInfo info, List<PlayerInfo> players) {
//         info.setId(ticker.getId());
//         info.setTick(ticker.getTick());
//         info.setBeat((int) ticker.getBeat());
//         info.setBar(ticker.getBar());
//         info.setDone(ticker.isDone());
//         info.setTempoInBPM(ticker.getTempoInBPM());
//         info.setBeatDivider(ticker.getBeatDivider());
//         info.setMaxTracks(ticker.getMaxTracks());
//         info.setPlaying(ticker.isPlaying());
//         info.setSwing(ticker.getSwing());
//         info.setStopped(ticker.isStopped());
//         info.setSongLength(ticker.getSongLength());
//         info.setPartLength(ticker.getPartLength());
//         info.setTicksPerBeat(ticker.getTicksPerBeat());
//         info.setPartLength(ticker.getPartLength());
//         info.setBeatsPerBar(ticker.getBeatsPerBar());
//         info.setPlayers(players);
//     }

//     public static TickerInfo fromTicker(Ticker ticker, List<PlayerInfo> players) {
//         TickerInfo info = new TickerInfo();
//         copyFromTicker(ticker, info, players);
//         return info;
//     }
// }
