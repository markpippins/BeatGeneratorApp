package com.angrysurfer.core.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface IInstrument {

    Long getId();

    List<IControlCode> getControlCodes();

    Set<IPad> getPads();

    Map<Integer, String> getAssignments();

    Map<Integer, Integer[]> getBoundaries();

    MidiDevice getDevice();

    AtomicReference<Receiver> getReceiver();

    String getName();

    String getDeviceName();

    Integer[] getChannels();

    Integer getLowestNote();

    Integer getHighestNote();

    Integer getHighestPreset();

    Integer getPreferredPreset();

    Boolean getHasAssignments();

    String getPlayerClassName();

    Boolean getAvailable();

    Set<IPattern> getPatterns();

    boolean isInitialized();

    void setId(Long id);

    void setControlCodes(List<IControlCode> controlCodes);

    void setPads(Set<IPad> pads);

    void setAssignments(Map<Integer, String> assignments);

    void setBoundaries(Map<Integer, Integer[]> boundaries);

    @JsonIgnore
    void setReceiver(AtomicReference<Receiver> receiver);

    void setName(String name);

    void setDeviceName(String deviceName);

    void setChannels(Integer[] channels);

    void setLowestNote(Integer lowestNote);

    void setHighestNote(Integer highestNote);

    void setHighestPreset(Integer highestPreset);

    void setPreferredPreset(Integer preferredPreset);

    void setHasAssignments(Boolean hasAssignments);

    void setPlayerClassName(String playerClassName);

    void setAvailable(Boolean available);

    void setPatterns(Set<IPattern> patterns);

    void setInitialized(boolean initialized);

    Integer DEFAULT_CHANNEL = 0;
    Integer[] DEFAULT_CHANNELS = new Integer[] { DEFAULT_CHANNEL };
    Integer[] ALL_CHANNELS = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

    // Helper method to determine if device is likely multi-timbral
    boolean isMultiTimbral();

    boolean receivesOn(Integer channel);

    // Convenience method for single-channel devices
    int getDefaultChannel();

    String assignedControl(int cc);

    void channelPressure(int channel, long data1, long data2)
            throws MidiUnavailableException, InvalidMidiDataException;

    void controlChange(int channel, long data1, long data2)
            throws InvalidMidiDataException, MidiUnavailableException;

    void noteOn(int channel, long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException;

    void noteOff(int channel, long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException;

    void polyPressure(int channel, long data1, long data2)
            throws MidiUnavailableException, InvalidMidiDataException;

    void programChange(int channel, long data1, long data2)
            throws InvalidMidiDataException, MidiUnavailableException;

    void start(int channel) throws MidiUnavailableException, InvalidMidiDataException;

    void stop(int channel) throws MidiUnavailableException, InvalidMidiDataException;

    void randomize(int channel, List<Integer> params);

    void sendToDevice(ShortMessage message) throws MidiUnavailableException;

    void cleanup();

    void setDevice(MidiDevice device);

    void assign(int cc, String control);

    void setBounds(int cc, int lowerBound, int upperBound);

    Integer getAssignmentCount();

}