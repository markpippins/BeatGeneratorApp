package com.angrysurfer.midi.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.midi.exception.MidiDeviceException;
import com.angrysurfer.midi.model.midi.MidiDeviceInfo;
import com.angrysurfer.midi.model.midi.MidiInstrument;

@Service
public class MIDIService {

    static Logger logger = LoggerFactory.getLogger(MIDIService.class.getCanonicalName());
    
    // Improved error handling and validation
    public static List<MidiDeviceInfo> getMidiDeviceInfos() {
        logger.info("getMidiDeviceInfos");
        try {
            return Arrays.stream(MidiSystem.getMidiDeviceInfo())
                .filter(Objects::nonNull)
                .map(info -> {
                    try {
                        MidiDevice device = MidiSystem.getMidiDevice(info);
                        return device != null ? new MidiDeviceInfo(device) : null;
                    } catch (MidiUnavailableException ex) {
                        logger.warn("Failed to get MIDI device: " + info.getName(), ex);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        } catch (Exception ex) {
            throw new MidiDeviceException("Failed to get MIDI device infos", ex);
        }
    }

    public static List<MidiDevice> getMidiDevices() {
        logger.info("getMidiDevices");
        List<MidiDevice> result = Arrays.stream(MidiSystem.getMidiDeviceInfo()).map(info -> {
            logger.info(String.format("retrieving device for %s, %s, %s", info.getName(), info.getVendor(),
                    info.getDescription()));
            try {
                return MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException ex) {
                logger.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        }).toList();

        return result;
    }

    public static List<MidiDevice> findMidiDevices(boolean receive, boolean transmit) {
        logger.info(String.format("findMidiDevices(receive: %s, transmit: %s)", receive, transmit));
        List<MidiDevice> result = getMidiDevices().stream().map(device -> {
            if ((transmit == (device.getMaxTransmitters() != 0) && receive == (device.getMaxReceivers() != 0)))
                return device;
            else
                return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return result;
    }

    public static MidiDevice findMidiInDevice(String name) {
        logger.info(String.format("findMidiInDevice(%s)", name));
        return findMidiDevices(false, true).stream().filter(d -> d.getDeviceInfo().getName().equals(name)).findAny()
                .orElseThrow();
    }

    // Better error handling for reset
    public static void reset() {
        logger.info("reset()");
        try {
            var sequencer = MidiSystem.getSequencer();
            if (sequencer != null) {
                sequencer.getReceivers().forEach(Receiver::close);
            }
        } catch (MidiUnavailableException ex) {
            logger.warn("Failed to reset MIDI system", ex);
            // Don't throw - this is cleanup code
        }
    }

    // Improved resource management
    public static boolean select(MidiDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Device cannot be null");
        }
        
        logger.info("select({})", device.getDeviceInfo().getName());
        
        try {
            if (!device.isOpen()) {
                reset();
                device.open();
                try (Receiver receiver = device.getReceiver()) {
                    MidiSystem.getTransmitter().setReceiver(receiver);
                }
            }
            return device.isOpen();
        } catch (MidiUnavailableException ex) {
            throw new MidiDeviceException("Failed to select MIDI device", ex);
        }
    }

    public static boolean select(String name) throws MidiUnavailableException {
        logger.info(String.format("select(%s)", name));
        reset();
        // MidiDevice device = InstrumentService.findMidiOutDevice(name);
        MidiDevice device = InstrumentService.getMidiDevice(name);
        if (!device.isOpen()) {
            device.open();
            MidiSystem.getTransmitter().setReceiver(device.getReceiver());
            return device.isOpen();
        }

        return false;
    }

    // public static MidiDevice findMidiOutDevice(String name) {
    //     logger.info(String.format("findMidiOutDevice(%s)", name));
    //     MidiDevice result = null;

    //     result = findMidiDevices(true, false).stream()
    //             .filter(d -> Objects.nonNull(d.getDeviceInfo().getName())
    //                     && d.getDeviceInfo().getName().equals(name))
    //             .findAny()
    //             .orElseThrow();

    //     return result;
    // }

    // Add proper cleanup
    @Override
    public void finalize() {
        reset();
    }

    // Improved message sending with validation
    public void sendMessage(MidiInstrument instrument, int messageType, int data1, int data2) {
        if (instrument == null) {
            throw new IllegalArgumentException("Instrument cannot be null");
        }
        
        try {
            ShortMessage message = new ShortMessage(messageType, 
                instrument.getChannel(), 
                validateData(data1), 
                validateData(data2));
            instrument.sendToDevice(message);
        } catch (InvalidMidiDataException | MidiUnavailableException ex) {
            throw new MidiDeviceException("Failed to send MIDI message", ex);
        }
    }

    private int validateData(int data) {
        if (data < 0 || data > 127) {
            throw new IllegalArgumentException("MIDI data must be between 0 and 127");
        }
        return data;
    }
}