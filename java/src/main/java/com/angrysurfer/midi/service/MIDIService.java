package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.StepData;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MIDIService implements IMIDIService {

    public static final String DEVICES_INFO = "/devices/info";
    public static final String DEVICE_NAMES = "/devices/names";
    public static final String SERVICE_RESET = "/service/reset";
    public static final String SERVICE_SELECT = "/service/select";
    static Logger logger = LoggerFactory.getLogger(MIDIService.class.getCanonicalName());

//    static Sequencer sequencer;
//
//    static {
//        try {
//            sequencer = MidiSystem.getSequencer();
////            initialized = true;
//        } catch (MidiUnavailableException e) {
//            throw new RuntimeException(e);
//        }
//    }

    //    public static void playTestNote() throws InvalidMidiDataException, MidiUnavailableException {
    //        int channel = 0;
    //        int note = 60;
    //        int velocity = 127; // velocity (i.e. volume); 127 = high
    //        ShortMessage msg = new ShortMessage();
    //        msg.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
    //    }
    @Override
    public List<MidiDevice> getMidiDevices() {

        Sequencer seq;

        return Arrays.stream(MidiSystem.getMidiDeviceInfo()).map(info -> {
            try {
                return MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException ex) {
                logger.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
    }

    @Override
    public List<MidiDevice> findMidiDevices(boolean receive, boolean transmit) {
        return getMidiDevices().stream().map(device -> {
            if ((transmit == (device.getMaxTransmitters() != 0) && receive == (device.getMaxReceivers() != 0)))
                return device;
            else return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<MidiDevice> findMidiDevice(String name) {
        return findMidiDevices(true, false).stream()
                .filter(d -> d.getDeviceInfo().getName().equals(name)).toList();
    }

    public void reset() {
        try {
            MidiSystem.getSequencer().getReceivers().forEach(Receiver::close);
        } catch (MidiUnavailableException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean select(MidiDevice device) {
        reset();
        if (!device.isOpen()) {
            try {
                device.open();
                MidiSystem.getTransmitter().setReceiver(device.getReceiver());
            } catch (MidiUnavailableException e) {
                throw new RuntimeException(e);
            }
        }
        return device.isOpen();
    }

    public boolean select(String name) {
        reset();
//            try (MidiDevice device = findMidiDevice(name)) {
        try {
            List<MidiDevice> devices = findMidiDevice(name);

            MidiDevice device = devices.get(0);
            if (!device.isOpen()) {
                device.open();
                MidiSystem.getTransmitter().setReceiver(device.getReceiver());
                return device.isOpen();
            }

        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

//    @Override
//    public void playSequence(List<StepData> steps) {
//
//        int channel = 4;
//        int bpm = 120;
//        int resolution = 24;
//
//        int barTicks = 4 * 24;
//
////        int startTick =
//
//
//        try {
//            Sequence sequence = new Sequence(Sequence.PPQ, resolution);
//            Track track = sequence.createTrack();
//            AtomicLong time = new AtomicLong(barTicks);
//
//            steps.forEach(s -> {
//                try {
//                    track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, channel, s.getPitch(), 0), time.get()));
//                    track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, channel, s.getPitch(), 0), time.get() + 50));
//                } catch (InvalidMidiDataException e) {
//                    throw new RuntimeException(e);
//                }
//
//                time.addAndGet(1000);
//            });
//
//            sequencer.setTempoInBPM(bpm);
//            sequencer.setSequence(sequence);
//            sequencer.setLoopCount(Integer.MAX_VALUE);
////            sequencer.open();
////            sequencer.start();
////            sequencer.close();
//
//        } catch (InvalidMidiDataException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public void sendMessage(IMidiInstrument instrument, int messageType, int data1, int data2) throws
            MidiUnavailableException, InvalidMidiDataException {
        instrument.sendToDevice(new ShortMessage(messageType, instrument.getChannel(), data1, data2));
    }
}
