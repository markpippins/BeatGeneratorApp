package com.angrysurfer.core.model;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;

public record InstrumentInfo(String name, int channel, String deviceName, MidiDevice device,
                             Receiver receiver) {
}
