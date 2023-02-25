package com.angrysurfer.midi.model;

import com.angrysurfer.midi.model.config.BeatGeneratorConfig;
import com.angrysurfer.midi.model.config.MidiInstrumentList;
import com.angrysurfer.midi.model.config.PlayerInfo;
import com.angrysurfer.midi.service.IMidiInstrument;
import com.angrysurfer.midi.service.MIDIService;
import com.angrysurfer.midi.service.MidiInstrument;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.angrysurfer.midi.model.condition.Comparison.EQUALS;
import static com.angrysurfer.midi.model.condition.Comparison.MODULO;
import static com.angrysurfer.midi.model.condition.Operator.BEAT;
import static com.angrysurfer.midi.model.condition.Operator.TICK;

@Getter
@Setter
public class BeatGenerator extends Ticker {
    static final Random rand = new Random();
    static final String RAZZ = "Razzmatazz";
    static final String MICROFREAK = "MicroFreak";
    static Logger logger = LoggerFactory.getLogger(BeatGenerator.class.getCanonicalName());
    static Integer[] notes = new Integer[]{27, 22, 27, 23};// {-1, 33 - 24, 26 - 24, 21 - 24};
    static List<Integer> microFreakParams = List.of(5, 9, 10, 12, 13, 23, 24, 28, 83, 91, 92, 93, 94, 102, 103);
    static List<Integer> fireballParams = List.of(40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60);
    static List<Integer> razParams = List.of(16, 17, 18, 19, 20, 21, 22, 23);
    static List<Integer> closedHatParams = List.of(24, 25, 26, 27, 28, 29, 30, 31);
    static List<Integer> kickParams = List.of(1, 2, 3, 4, 12, 13, 14, 15);
    static List<Integer> snarePrams = List.of(16, 17, 18, 19, 20, 21, 22, 23);
    static String deviceName = "mrcc";
    static MidiDevice device = getDevice();
    static ObjectMapper mapper = new ObjectMapper();
    static int KICK = 36;
    static int SNARE = 37;
    static int CLOSED_HAT = 38;
    static int OPEN_HAT = 39;
    private List<Strike> pads = new ArrayList<>();

    private List<Player> addList = new ArrayList<>();

    private List<Player> removeList = new ArrayList<>();
    private Map<String, IMidiInstrument> instrumentMap = new HashMap<>();

    public BeatGenerator(int songLength) {
        super(songLength);
        if (new MIDIService().select(device))
            setInstrumentMap(loadConfig());
    }

    static MidiDevice getDevice() {
        try {
            return MidiSystem.getMidiDevice(Stream.of(MidiSystem.getMidiDeviceInfo()).filter(info -> info.getName().toLowerCase().contains(deviceName)).toList().get(0));
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        BeatGenerator gen = new BeatGenerator(64);
        if (new MIDIService().select(device))
            IntStream.range(0, 16).forEach(i -> gen.start());
    }

    @Override
    public PlayerInfo addPlayer(String instrument) {
        Strike player = new Strike(instrument.concat(Integer.toString(getPlayers().size())),
                this, getInstrument(instrument), KICK + getPlayers().size(), closedHatParams)
                .addCondition(BEAT, MODULO, 1.0);
        player.setId((long) getPlayers().size() + getWaitList().size() + 1);

        if (isPlaying())
            addList.add(player);
        else
            getPlayers().add(player);

        return PlayerInfo.fromPlayer(player);
    }

    @Override
    public void onBarChange(int bar) {
        if (!getRemoveList().isEmpty()) {
            getPlayers().removeAll(getRemoveList());
            getRemoveList().clear();
        }
        if (!getAddList().isEmpty()) {
            getPlayers().addAll(getAddList());
            getAddList().clear();
        }
    }

    public void loadBeat(String fileName) {

        try {
            String filepath = "resources/beats/" + toString() + ".json";
            File file = new File(filepath);
            if (!file.exists()) {
                file.createNewFile();
            }

            getPlayers().clear();
            BeatGeneratorConfig config = mapper.readValue(new File(filepath), BeatGeneratorConfig.class);
            config.setup(this);

            config.getPlayers().forEach(drumPadDef -> {
                Strike pad = new Strike();
                IMidiInstrument instrument = new MidiInstrument(null, getDevice(), drumPadDef.getChannel());
                pad.setInstrument(instrument);
                pad.setNote(drumPadDef.getNote());
                pad.setPreset(drumPadDef.getPreset());
                pad.setConditions(drumPadDef.getConditions());
                pad.setAllowedControlMessages(drumPadDef.getAllowedControlMessages());
                pad.setMaxVelocity(drumPadDef.getMaxVelocity());
                pad.setMinVelocity(drumPadDef.getMaxVelocity());
                pad.setTicker(this);
                getPlayers().add(pad);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, IMidiInstrument> loadConfig() {
        Map<String, IMidiInstrument> results = new HashMap<>();

        try {
            String filepath = "resources/config/midi.json";
            MidiInstrumentList config = mapper.readValue(new File(filepath), MidiInstrumentList.class);
            config.getInstruments().forEach(instrumentDef -> results.put(instrumentDef.getName(),
                    MidiInstrument.fromMidiInstrumentDef(getDevice(), instrumentDef)));
            return results;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveBeat(List<Strike> pads) {

        try {
            String beatFile = "resources/beats/" + toString() + ".json";
            File file = new File(beatFile);
            if (file.exists()) file.delete();
            Files.write(file.toPath(), Collections.singletonList(mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(new BeatGeneratorConfig(this, pads))), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveConfig() {
        try {
            String instruments = "resources/config/midi-bak.json";
            File file = new File(instruments);
            if (file.exists()) file.delete();
            Files.write(file.toPath(), Collections.singletonList(mapper.writerWithDefaultPrettyPrinter().
                    writeValueAsString(new MidiInstrumentList(getPlayers()))), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Double nextValue(int bound) {
        return rand.nextInt(bound * 4) * .25;
    }

    public void start() {
        saveConfig();
        run();
    }

    public void next() {
        stop();
        makeBeats();
        run();
    }

    public void save() {
        saveConfig();
    }

    public IMidiInstrument getInstrument(String name) {
        return getInstrumentMap().get(name);
    }

    public void makeBeats() {


        IMidiInstrument fireball = getInstrument("Fireball");
        IMidiInstrument zero = getInstrument("Zero");

        getPlayers().clear();

        Stream.of(new Strike("blackbox-kick", this, getInstrument("blackbox"), KICK, kickParams, 100, 125)
                                .addCondition(BEAT, MODULO, 2.0),
                        new Strike("raz-kick", this, getInstrument(RAZZ), SNARE, snarePrams, 100, 125)
                                .addCondition(BEAT, EQUALS, rand.nextInt(1, 4) * .25),
                        new Strike("raz-closed-hat", this, getInstrument(RAZZ), CLOSED_HAT, closedHatParams, 100, 125)
                                .addCondition(BEAT, MODULO, 3.0),
                        new Strike("blackbox-open-hat", this, getInstrument("blackbox"), OPEN_HAT, closedHatParams, 100, 125)
                                .addCondition(BEAT, EQUALS, rand.nextInt(4, 6) * .25)
                                .addCondition(TICK, MODULO, 3.0),
                        new Strike("blackbox-snare", this, getInstrument("blackbox"), SNARE, snarePrams, 100, 125)
                                .addCondition(BEAT, MODULO, rand.nextInt(1, 4) * .25),
                        new Strike("raz-snare", this, getInstrument(RAZZ), SNARE, snarePrams, 100, 125)
                                .addCondition(BEAT, MODULO, rand.nextInt(1, 4) * .25),
                        new Strike("raz-closed-hat", this, getInstrument(RAZZ), CLOSED_HAT, closedHatParams, 100, 125)
                                .addCondition(BEAT, EQUALS, rand.nextInt(2, 8) * .25),
                        new Strike("blackbox-open-hat", this, getInstrument("blackbox"), OPEN_HAT, closedHatParams, 100, 125)
                                .addCondition(BEAT, MODULO, rand.nextInt(4, 6) * .25)
                                .addCondition(TICK, EQUALS, rand.nextInt(6, 8) * .25)
                ).filter(h -> rand.nextBoolean())
                .forEach(getPlayers()::add);
        setBeatDivider(rand.nextInt(1, 4));

        try {
            int razPreset = rand.nextInt(127);
            getInstrument(RAZZ).programChange(razPreset, 127);
            getPlayers().stream().filter(p -> p.getInstrument().equals(getInstrument(RAZZ)))
                    .forEach(p -> p.setPreset(razPreset));

            int microFreakPreset = rand.nextInt(127);
            getInstrument(MICROFREAK).programChange(microFreakPreset, 127);
            getPlayers().stream().filter(p -> p.getInstrument().equals(getInstrument(MICROFREAK)))
                    .forEach(p -> p.setPreset(microFreakPreset));
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }
}

//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, CLOSED_HAT, closedHatParams, 100, 125)
//                    .addCondition(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, CLOSED_HAT, closedHatParams, 100, 125)
//                    .addCondition(TICK, MODULO, nextValue(4))
//                    .addCondition(BAR, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, SNARE, snarePrams, 100, 125)
//                    .addCondition(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, fireball, 51, fireballParams, 100, 125)
//                .addCondition(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, microfreak, 39, microFreakParams, 100, 125)
//                .addCondition(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, microfreak, 53, microFreakParams, 100, 125)
//                .addCondition(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, zero, 49, Collections.emptyList(), 100, 125)
//                .addCondition(TICK, MODULO, nextValue(4))
//                .addCondition(BEAT, LESS_THAN, nextValue(4)));

//        result.add(new DrumPad(ticker, raz, CLOSED_HAT, kickParams, 100, 125)
//                .addCondition(TICK, MODULO, 2.0));
//

//        IntStream.range(36, 44).forEach(drum -> {
//            IntStream.range(EQUALS, MODULO + 1).forEach(op -> {
//                IntStream.range(TICK, BAR + 1).forEach(div -> {
//                    while (rand.nextBoolean()) {
//                        DrumPad pad = new DrumPad(ticker, raz, drum, Collections.emptyList(), 100, 125);
//                        pad.addCondition(rand.nextInt(MODULO + 1), rand.nextInt(BAR + 1), nextValue(4));
//                        result.add(pad);
//                    }
//                });
//            });
//        });
//        if (rand.nextBoolean())
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, blackbox, KICK, kickParams, 100, 125)
//                    .addCondition(BEAT, GREATER_THAN, nextValue(4))
//                    .addCondition(BEAT, LESS_THAN, nextValue(4))
//                    .addCondition(BAR, MODULO, nextValue(4))
//                    .addCondition(TICK, MODULO, nextValue(4)));
////
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, KICK, kickParams, 100, 125)
//                .addCondition(BAR, LESS_THAN, nextValue(4))
//                .addCondition(BAR, GREATER_THAN, nextValue(4))
//                .addCondition(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, blackbox, KICK, kickParams, 100, 125)
//                .addCondition(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, SNARE, snarePrams, 100, 125)
//                .addCondition(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, SNARE, snarePrams, 100, 125)
//                .addCondition(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, blackbox, SNARE, snarePrams, 100, 125)
//                .addCondition(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, CLOSED_HAT, closedHatParams, 100, 125)
//                .addCondition(TICK, MODULO, nextValue(4)));

//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, OPEN_HAT, List.of(24, 25, 26, 27, 28, 29, 30, 31), 100, 125)
//                    .addCondition(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, OPEN_HAT, List.of(32, 33, 34, 35, 36, 37, 38, 39), 100, 125)
//                    .addCondition(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, 41, razParams, 100, 125)
//                .addCondition(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, raz, CLOSED_HAT, razParams, 100, 125)
//                .addCondition(TICK, MODULO, nextValue(4)));
//
//        if (rand.nextBoolean())
//            result.add(new DrumPad(ticker, raz, OPEN_HAT, razParams, 100, 125).addCondition(TICK, MODULO, nextValue(4)));

//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, fireball, 51, fireballParams, 100, 125)
//                .addCondition(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, microfreak, 39, microFreakParams, 100, 125)
//                .addCondition(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, microfreak, 53, microFreakParams, 100, 125)
//                .addCondition(BEAT, EQUALS, nextValue(4)));
//
//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, zero, 49, Collections.emptyList(), 100, 125)
//                .addCondition(TICK, MODULO, nextValue(4))
//                .addCondition(BEAT, LESS_THAN, nextValue(4)));

//        if (rand.nextBoolean()) result.add(new DrumPad(ticker, zero, 51, Collections.emptyList(), 100, 125)
//                .addCondition(TICK, MODULO, nextValue(4)));

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