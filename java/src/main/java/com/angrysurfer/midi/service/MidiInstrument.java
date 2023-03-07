package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.ControlCode;
import com.angrysurfer.midi.model.MidiInstrumentInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import java.io.Serializable;
import java.util.*;

@Slf4j
@Getter
@Setter
//@Entity
public class MidiInstrument implements Serializable {
    static final Random rand = new Random();
    static Logger logger = LoggerFactory.getLogger(MidiInstrument.class.getCanonicalName());
    private List<ControlCode> controlCodes = new ArrayList<>();
    private Map<Integer, String> assignments = new HashMap<>();
    private Map<Integer, Integer[]> boundaries = new HashMap<>();
    @JsonIgnore
    private MidiDevice device;
    private String name;
    private int channel;
    private int lowestNote;
    private int highestNote;
    private int highestPreset;
    private int preferredPreset;
    private int pads;
    private boolean hasAssignments;

    public MidiInstrument() {

    }

    public MidiInstrument(String name, MidiDevice device, int channel) {
        setName(Objects.isNull(name) ? device.getDeviceInfo().getName() : name);
        setDevice(device);
        setChannel(channel);
        logger.info(String.join(" ", getName(), "created on channel", Integer.toString(getChannel())));
    }

    public static MidiInstrument fromMidiInstrumentDef(MidiDevice device, MidiInstrumentInfo instrumentDef) {
        MidiInstrument instrument = new MidiInstrument(instrumentDef.getName(), device, instrumentDef.getChannel());
        instrument.setHighestNote(instrumentDef.getHighestNote());
        instrument.setLowestNote(instrumentDef.getLowestNote());
        instrument.setAssignments(instrumentDef.getAssignments());
        instrument.setBoundaries(instrumentDef.getBoundaries());
        instrument.setHasAssignments(instrumentDef.getAssignments().size() > 0);
        instrument.setControlCodes(instrumentDef.getControlCodes());
        return instrument;
    }

    public String assignedControl(int cc) {
        return assignments.getOrDefault(cc, "NONE");
    }


    public void channelPressure(int data1, int data2) throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.CHANNEL_PRESSURE, getChannel(), data1, data2));
    }


    public void controlChange(int data1, int data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.CONTROL_CHANGE, getChannel(), data1, data2));
    }


    public void noteOn(int data1, int data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(data1 == -1 ? ShortMessage.NOTE_OFF : ShortMessage.NOTE_ON, getChannel(), data1, data2));
    }


    public void noteOff(int data1, int data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.NOTE_OFF, getChannel(), data1, data2));
    }


    public void polyPressure(int data1, int data2) throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.POLY_PRESSURE, getChannel(), data1, data2));
    }


    public void programChange(int data1, int data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.PROGRAM_CHANGE, getChannel(), data1, data2));
    }


    public void start() throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.START, getChannel(), 0, 0));
    }

    public void stop() throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.STOP, getChannel(), 1, 1));
    }


    public void randomize(List<Integer> params) {

        new Thread(() -> params.forEach(cc ->
        {
            try {
                int value = getBoundaries().containsKey(cc) ?
                        rand.nextInt(getBoundaries().get(cc)[0], getBoundaries().get(cc)[0] >= getBoundaries().get(cc)[1] ? getBoundaries().get(cc)[0] + 1 : getBoundaries().get(cc)[1]) :
                        rand.nextInt(0, 127);

                sendToDevice(new ShortMessage(ShortMessage.CONTROL_CHANGE, getChannel(), cc, value));
            } catch (IllegalArgumentException | MidiUnavailableException | InvalidMidiDataException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        })).start();
    }


    public void sendToDevice(ShortMessage message) throws MidiUnavailableException {
        logger.info(String.join(", ",
                toString(),
                MidiMessage.lookupCommand(message.getCommand()),
                message.getCommand() != ShortMessage.NOTE_ON && message.getCommand() != ShortMessage.NOTE_OFF ? lookupTarget(message.getData1()) : Integer.toString(message.getData1()),
                Integer.toString(message.getData2())));
        getDevice().getReceiver().send(message, new Date().getTime());
    }


    public void assign(int cc, String control) {
        getAssignments().put(cc, control);
    }


    public void setBounds(int cc, int lowerBound, int upperBound) {
        getBoundaries().put(cc, new Integer[]{lowerBound, upperBound});
    }

    public Integer getAssignmentCount() {
        return getAssignments().size();
    }

    private String lookupTarget(int key) {
        return assignments.getOrDefault(key, Integer.toString(key));
    }
}

