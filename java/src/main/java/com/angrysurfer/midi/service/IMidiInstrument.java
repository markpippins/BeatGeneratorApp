package com.angrysurfer.midi.service;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import java.util.List;
import java.util.Map;

public interface IMidiInstrument {
    public String assignedControl(int cc);

    void channelPressure(int data1, int data2) throws MidiUnavailableException, InvalidMidiDataException;

    void controlChange(int data1, int data2) throws InvalidMidiDataException, MidiUnavailableException;

    void noteOn(int data1, int data2) throws InvalidMidiDataException, MidiUnavailableException;

    void noteOff(int data1, int data2) throws InvalidMidiDataException, MidiUnavailableException;

    void polyPressure(int data1, int data2) throws MidiUnavailableException, InvalidMidiDataException;

    void programChange(int data1, int data2) throws InvalidMidiDataException, MidiUnavailableException;

    void start() throws MidiUnavailableException, InvalidMidiDataException;

    void stop() throws MidiUnavailableException, InvalidMidiDataException;

    Map<Integer, String> getAssignments();

    void randomize(List<Integer> params);

    //    boolean initialize();
    String getName();

    void setName(String name);

    MidiDevice getDevice();

    void sendToDevice(ShortMessage message) throws MidiUnavailableException;

    int getChannel();

    void setChannel(int channel);

    void assign(int cc, String control);

    void setBounds(int cc, int lowerBound, int upperBound);

    Map<Integer, Integer[]> getBoundaries();

    int getHighestNote();

    void setHighestNote(int note);

    int getLowestNote();

    void setLowestNote(int note);

    int getPreferredPreset();

    void setPreferredPreset(int preset);

    int getHighestPreset();

    void setHighestPreset(int preset);

}