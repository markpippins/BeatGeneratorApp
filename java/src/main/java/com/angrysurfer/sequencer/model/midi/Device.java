package com.angrysurfer.sequencer.model.midi;

import java.io.Serializable;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Device implements Serializable {

    static Logger logger = LoggerFactory.getLogger(Device.class.getCanonicalName());

    private final String name;

    private final String vendor;

    private final String description;

    private final String version;

    private Integer maxReceivers;

    private Integer maxTransmitters;

    private String receiver;

    private String receivers;

    private String transmitter;

    private String transmitters;

    private Integer[] channels;

    public Device(MidiDevice device) {

        this.name = device.getDeviceInfo().getName();
        this.vendor = device.getDeviceInfo().getVendor();
        this.description = device.getDeviceInfo().getDescription();
        this.version = device.getDeviceInfo().getVersion();

        this.maxReceivers = device.getMaxReceivers();
        this.maxTransmitters = device.getMaxTransmitters();

        // if (maxReceivers > 0)
        try {
            if (device.getReceiver() != null)
                this.receiver = device.getReceiver().toString();
            this.receivers = String.format("%s", device.getReceivers().size());
        } catch (MidiUnavailableException e) {
            logger.error(e.getMessage());
        }

        // if (maxTransmitters > 0)
        try {
            if (device.getTransmitter() != null)
                this.transmitter = device.getTransmitter().toString();
        } catch (MidiUnavailableException e) {
            logger.error(e.getMessage());
        }
        this.transmitters = String.format("%s", device.getTransmitters().size());
    }
}
