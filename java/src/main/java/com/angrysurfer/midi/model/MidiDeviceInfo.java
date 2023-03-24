package com.angrysurfer.midi.model;

import java.io.Serializable;

import javax.sound.midi.MidiDevice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MidiDeviceInfo implements Serializable {

    private final String name;

    private final String vendor;

    private final String description;

    private final String version;

    private int maxReceivers;

    private int maxTransmitters;

    private String receivers;

    private String transmitters;
    
    public MidiDeviceInfo(MidiDevice device) {

        this.name = device.getDeviceInfo().getName();
        this.vendor = device.getDeviceInfo().getVendor();
        this.description = device.getDeviceInfo().getDescription();
        this.version = device.getDeviceInfo().getVersion();

        this.maxReceivers = device.getMaxReceivers();
        this.maxTransmitters = device.getMaxTransmitters();

        StringBuffer receivers = new StringBuffer();
        device.getReceivers().forEach(r -> receivers.append(r.toString() + " "));
        this.receivers = receivers.toString();

        StringBuffer transmitters = new StringBuffer();
        device.getTransmitters().forEach(r -> transmitters.append(r.toString() + " "));
        this.transmitters = receivers.toString();
    }
}
