package com.angrysurfer.midi.model;

import java.io.Serializable;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MidiDeviceInfo implements Serializable {

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
    
    
    public MidiDeviceInfo(MidiDevice device) {

        this.name = device.getDeviceInfo().getName();
        this.vendor = device.getDeviceInfo().getVendor();
        this.description = device.getDeviceInfo().getDescription();
        this.version = device.getDeviceInfo().getVersion();

        this.maxReceivers = device.getMaxReceivers();
        this.maxTransmitters = device.getMaxTransmitters();

        try {
            this.receiver = device.getReceiver().toString();
        } catch (MidiUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.receivers = String.format("%s", device.getReceivers().size());

        try {
            this.transmitter = device.getTransmitter().toString();
        } catch (MidiUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.transmitters = String.format("%s", device.getTransmitters().size());
    }
}
