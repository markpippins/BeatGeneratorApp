package com.angrysurfer.midi.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.midi.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MIDIService implements IMIDIService {

    static Logger logger = LoggerFactory.getLogger(MIDIService.class.getCanonicalName());

    //    public static void playTestNote() throws InvalidMidiDataException, MidiUnavailableException {
    //        int channel = 0;
    //        int note = 60;
    //        int velocity = 127; // velocity (i.e. volume); 127 = high
    //        ShortMessage msg = new ShortMessage();
    //        msg.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
    //    }
    @Override
    public List<MidiDevice> getMidiDevices() {
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

    public void sendMessage(IMidiInstrument instrument, int messageType, int data1, int data2) throws
            MidiUnavailableException, InvalidMidiDataException {
        instrument.sendToDevice(new ShortMessage(messageType, instrument.getChannel(), data1, data2));
    }
}
