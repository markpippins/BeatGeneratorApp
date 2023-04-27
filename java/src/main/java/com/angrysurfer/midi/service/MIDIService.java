package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.MidiDeviceInfo;
import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.repo.MidiInstrumentRepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MIDIService {

    static Logger logger = LoggerFactory.getLogger(MIDIService.class.getCanonicalName());

    static Map<String, MidiDevice> midiInDevices = new HashMap<>();

    static Map<String, MidiDevice> midiOutDevices = new HashMap<>();

    static String GS_SYNTH = "Microsoft GS Wavetable Synth";
    static String GERVILL = "Gervill";

    static List<MidiDevice> midiDevices = new ArrayList<>();

    public static List<MidiDeviceInfo> getMidiDeviceInfos() {
        logger.info("getMidiDeviceInfos");
        return Arrays.stream(MidiSystem.getMidiDeviceInfo()).map(info -> {
            try {
                logger.info(String.format("retrieving device for %s, %s, %s", info.getName(), info.getVendor(),
                        info.getDescription()));
                MidiDevice device = MidiSystem.getMidiDevice(info);
                return Objects.isNull(device) ? null : new MidiDeviceInfo(device);
            } catch (MidiUnavailableException ex) {
                logger.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }).filter(d -> Objects.nonNull(d)).toList();
    }

    public static List<MidiDevice> getMidiDevices() {
        logger.info("getMidiDevices");
        if (midiDevices.size() == 0)
            midiDevices = Arrays.stream(MidiSystem.getMidiDeviceInfo()).map(info -> {
                logger.info(String.format("retrieving device for %s, %s, %s", info.getName(), info.getVendor(),
                        info.getDescription()));
                try {
                    return MidiSystem.getMidiDevice(info);
                } catch (MidiUnavailableException ex) {
                    logger.error(ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                }
            }).toList();

        return midiDevices;
    }

    public static List<MidiDevice> findMidiDevices(boolean receive, boolean transmit) {
        logger.info(String.format("findMidiDevices(receive: %s, transmit: %s)", receive, transmit));
        return getMidiDevices().stream().map(device -> {
            if ((transmit == (device.getMaxTransmitters() != 0) && receive == (device.getMaxReceivers() != 0)))
                return device;
            else
                return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static MidiDevice findMidiInDevice(String name) {
        logger.info(String.format("findMidiInDevice(%s)", name));
        if (midiOutDevices.containsKey(name))
            return midiOutDevices.get(name);

        // else midiInDevices.put(name,
        return findMidiDevices(false, true).stream().filter(d -> d.getDeviceInfo().getName().equals(name)).findAny()
                .orElseThrow();

        // return midiInDevices.get(name);
    }

    public static void reset() {
        logger.info("reset()");
        try {
            MidiSystem.getSequencer().getReceivers().forEach(Receiver::close);
        } catch (MidiUnavailableException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static boolean select(MidiDevice device) {
        logger.info(String.format("select(%s)", device.getDeviceInfo().getName()));
        if (!device.isOpen()) {
            // reset();
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
        logger.info(String.format("select(%s)", name));
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

    public static MidiDevice findMidiOutDevice(String name) {
        logger.info(String.format("findMidiOutDevice(%s)", name));
        if (midiOutDevices.containsKey(name))
            return midiOutDevices.get(name);

        MidiDevice result = null;

        try {
            result = findMidiDevices(true, false).stream()
                    .filter(d -> Objects.nonNull(d.getDeviceInfo().getName())
                            && d.getDeviceInfo().getName().equals(name))
                    .findAny()
                    .orElse(getMidiDevices().stream()
                            .filter(d -> Objects.nonNull(d.getDeviceInfo().getName())
                                    && d.getDeviceInfo().getName().equals(GS_SYNTH))
                            .findFirst()
                            .orElseThrow());

            if (Objects.nonNull(result.getDeviceInfo().getName())
                    && result.getDeviceInfo().getName().equals(name))
                midiOutDevices.put(name, result);
            else
                midiOutDevices.put(GS_SYNTH, result);
        } catch (NoSuchElementException e) {
            logger.error(e.getMessage() + " for device " + name, e);
        }

        return result;
    }

    // public List<MidiInstrument> getInstrumentByChannel(int channel) {
    // logger.info(String.format("getInstrumentByChannel(%s)", channel));
    // if (midiInstruments.containsKey(channel))
    // return List.of(midiInstruments.get(channel));

    // List<MidiInstrument> results = midiInstrumentRepo.findByChannel(channel);
    // results.stream().filter(i -> i.getDeviceName() != null)
    // .forEach(i -> i.setDevice(findMidiOutDevice(i.getDeviceName())));

    // MidiInstrument instrument = new MidiInstrument();
    // instrument.setChannel(channel);
    // if (results.size() == 1)
    // midiInstruments.put(channel, results.get(0));
    // else {
    // try {
    // if (channel > 14) {
    // instrument.setDevice(
    // getMidiDevices().stream()
    // .filter(d -> Objects.nonNull(d.getDeviceInfo().getName())
    // && d.getDeviceInfo().getName().equals(GERVILL))
    // .findAny().orElseThrow());
    // instrument.setName(GERVILL);
    // } else {
    // instrument.setDevice(getMidiDevices().stream()
    // .filter(d -> Objects.nonNull(d.getDeviceInfo().getName())
    // && d.getDeviceInfo().getName().equals(GS_SYNTH))
    // .findAny()
    // .orElseThrow());
    // instrument.setName(GS_SYNTH);
    // }
    // midiInstruments.put(channel, instrument);
    // } catch (Exception e) {
    // logger.error(e.getMessage(), e);
    // }

    // instrument.setDeviceName(instrument.getDevice().getDeviceInfo().getName());
    // }

    // results.add(instrument);

    // return results;
    // }

    // public void sendMessageToInstrument(Long instrumentId, int messageType, int data1, int data2) {
    //     logger.info(String.format("sendMessageToIntrument(%s)", instrumentId));
    //     MidiInstrument instrument = getInstrumentById(instrumentId);
    //     if (Objects.nonNull(instrument)) {
    //         // List<MidiDevice> devices =
    //         // findMidiOutDevice(instrument.getDevice().getDeviceInfo().getName());
    //         // if (!devices.isEmpty()) {
    //         MidiDevice device = findMidiOutDevice(instrument.getDeviceName());
    //         if (!device.isOpen())
    //             try {
    //                 device.open();
    //                 MidiSystem.getTransmitter().setReceiver(device.getReceiver());
    //             } catch (MidiUnavailableException e) {
    //                 throw new RuntimeException(e);
    //             }
    //         new Thread(new Runnable() {

    //             public void run() {
    //                 try {
    //                     ShortMessage message = new ShortMessage();
    //                     message.setMessage(messageType, instrument.getChannel(), data1, data2);
    //                     device.getReceiver().send(message, 0L);
    //                     // log.info(String.join(", ",
    //                     // MidiMessage.lookupCommand(message.getCommand()),
    //                     // "Channel: ".concat(Integer.valueOf(message.getChannel()).toString()),
    //                     // "Data 1: ".concat(Integer.valueOf(message.getData1()).toString()),
    //                     // "Data 2: ".concat(Integer.valueOf(message.getData2()).toString())));
    //                 } catch (InvalidMidiDataException | MidiUnavailableException e) {
    //                     throw new RuntimeException(e);
    //                 }
    //             }
    //         }).start();
    //     }
    //     // }
    // }

    // public void sendMessageToChannel(int channel, int messageType, int data1, int data2) {
    //     logger.info(String.format("sendMessageToChannel(%s)", channel));

    //     getInstrumentByChannel(channel).forEach(instrument -> {
    //         // List<MidiDevice> devices =
    //         // findMidiOutDevice(instrument.getDevice().getDeviceInfo().getName());
    //         // if (!devices.isEmpty()) {
    //         MidiDevice device = findMidiOutDevice(instrument.getDeviceName());
    //         if (!device.isOpen())
    //             try {
    //                 device.open();
    //                 MidiSystem.getTransmitter().setReceiver(device.getReceiver());
    //             } catch (MidiUnavailableException e) {
    //                 throw new RuntimeException(e);
    //             }
    //         new Thread(new Runnable() {

    //             public void run() {
    //                 try {
    //                     ShortMessage message = new ShortMessage();
    //                     message.setMessage(messageType, channel, data1, data2);
    //                     device.getReceiver().send(message, 0L);
    //                     // log.info(String.join(", ",
    //                     // MidiMessage.lookupCommand(message.getCommand()),
    //                     // "Channel: ".concat(Integer.valueOf(message.getChannel()).toString()),
    //                     // "Data 1: ".concat(Integer.valueOf(message.getData1()).toString()),
    //                     // "Data 2: ".concat(Integer.valueOf(message.getData2()).toString())));
    //                 } catch (InvalidMidiDataException | MidiUnavailableException e) {
    //                     throw new RuntimeException(e);
    //                 }
    //             }
    //         }).start();

    //     });
    // }

    public void sendMessage(MidiInstrument instrument, int messageType, int data1, int data2)
            throws MidiUnavailableException, InvalidMidiDataException {
        instrument.sendToDevice(new ShortMessage(messageType, instrument.getChannel(), data1, data2));
    }
}