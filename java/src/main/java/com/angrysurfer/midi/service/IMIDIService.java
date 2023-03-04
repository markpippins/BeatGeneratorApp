package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.StepData;

import javax.sound.midi.MidiDevice;
import java.util.List;

public interface IMIDIService {
    public List<MidiDevice> getMidiDevices();

    public List<MidiDevice> findMidiDevices(boolean receive, boolean transmit);

    public List<MidiDevice> findMidiDevice(String name);

    public void reset();

    public boolean select(MidiDevice device);

    public boolean select(String name);

//    void playSequence(List<StepData> steps);
}