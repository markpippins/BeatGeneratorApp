package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.LookupItem;
import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.repo.MidiInstrumentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MIDIService {
    
    static Logger logger = LoggerFactory.getLogger(MIDIService.class.getCanonicalName());

    private MidiInstrumentRepository midiInstrumentRepo;


    public MIDIService(MidiInstrumentRepository midiInstrumentRepo) {
        this.midiInstrumentRepo = midiInstrumentRepo;
    }

    public static MidiDevice getDevice(String deviceName) {
        try {
            MidiDevice result = MidiSystem.getMidiDevice(Stream.of(MidiSystem.getMidiDeviceInfo()).
                    filter(info -> info.getName().toLowerCase().contains(deviceName.toLowerCase())).toList().get(0));
            return result;
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<MidiDevice> getMidiDevices() {
        return Arrays.stream(MidiSystem.getMidiDeviceInfo()).map(info -> {
            try {
                return MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException ex) {
                logger.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }).toList();
    }

    public static List<MidiDevice> findMidiDevices(boolean receive, boolean transmit) {
        return getMidiDevices().stream().map(device -> {
            if ((transmit == (device.getMaxTransmitters() != 0) && receive == (device.getMaxReceivers() != 0)))
                return device;
            else return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static MidiDevice findMidiOutDevice(String name) {
        return findMidiDevices(true, false).stream()
                .filter(d -> d.getDeviceInfo().getName().equals(name)).findAny().orElseThrow();
    }

    public static MidiDevice findMidiInDevice(String name) {
        return findMidiDevices(false, true).stream()
                .filter(d -> d.getDeviceInfo().getName().equals(name)).findAny().orElseThrow();
    }

    public static void reset() {
        try {
            MidiSystem.getSequencer().getReceivers().forEach(Receiver::close);
        } catch (MidiUnavailableException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    public static boolean select(MidiDevice device) {
        if (!device.isOpen()) {
            reset();
            try {
                device.open();
                MidiSystem.getTransmitter().setReceiver(device.getReceiver());
            } catch (MidiUnavailableException e) {
                throw new RuntimeException(e);
            }
        }
        return device.isOpen();
    }

    public static boolean select(String name) {
        reset();
        try {
            MidiDevice device = findMidiOutDevice(name);
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

    public List<MidiInstrument> getAllInstruments() {
        List<MidiInstrument> results = midiInstrumentRepo.findAll();
        results.forEach(i -> i.setDevice(findMidiOutDevice(i.getDeviceName())));
        return results;
    }

    
    public MidiInstrument getInstrumentByChannel(int channel) {
        Optional<MidiInstrument> instrument = midiInstrumentRepo.findByChannel(channel);
        if (instrument.isPresent())
            instrument.get().setDevice(findMidiOutDevice(instrument.get().getDeviceName()));
        return instrument.orElseThrow();
    }

    public MidiInstrument getInstrumentById(Long id) {
        return midiInstrumentRepo.findById(id).orElseThrow();
    }

    public void sendMessage(int messageType, int channel, int data1, int data2) {
        MidiInstrument instrument = getInstrumentByChannel(channel);
        if (Objects.nonNull(instrument)) {
            // List<MidiDevice> devices = findMidiOutDevice(instrument.getDevice().getDeviceInfo().getName());
            // if (!devices.isEmpty()) {
                MidiDevice device = findMidiOutDevice(instrument.getDeviceName());
                if (!device.isOpen())
                    try {
                        device.open();
                        MidiSystem.getTransmitter().setReceiver(device.getReceiver());
                    } catch (MidiUnavailableException e) {
                        throw new RuntimeException(e);
                    }
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            ShortMessage message = new ShortMessage();
                            message.setMessage(messageType, channel, data1, data2);
                            device.getReceiver().send(message, 0L);
                            // log.info(String.join(", ",
                            //         MidiMessage.lookupCommand(message.getCommand()),
                            //         "Channel: ".concat(Integer.valueOf(message.getChannel()).toString()),
                            //         "Data 1: ".concat(Integer.valueOf(message.getData1()).toString()),
                            //         "Data 2: ".concat(Integer.valueOf(message.getData2()).toString())));
                        } catch (InvalidMidiDataException | MidiUnavailableException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }   
        // }
    }

    public List<String> getInstrumentNames() {
        return midiInstrumentRepo.findAll().stream().map(i -> i.getName()).toList();
    }

    public void sendMessage(MidiInstrument instrument, int messageType, int data1, int data2) throws
            MidiUnavailableException, InvalidMidiDataException {
        instrument.sendToDevice(new ShortMessage(messageType, instrument.getChannel(), data1, data2));
    }

    public List<MidiInstrument> getInstrumentList() {
        return midiInstrumentRepo.findAll();
    }    

    public List<LookupItem> getInstrumentLookupItems() {
        return midiInstrumentRepo.findAll().stream().map(ins -> new LookupItem(ins.getId(), ins.getName(), (long) ins.getChannel())).toList();
    }

}

// public MidiInstrument getInstrumentInfo(Long instrumentId) {
//     return midiInstrumentRepo.findById(instrumentId).orElseThrow();
// }



//    public static void playTestNote() throws InvalidMidiDataException, MidiUnavailableException {
//        int channel = 0;
//        int note = 60;
//        int velocity = 127; // velocity (i.e. volume); 127 = high
//        ShortMessage msg = new ShortMessage();
//        msg.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
//    }


//
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
