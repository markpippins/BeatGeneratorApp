package com.angrysurfer.core.service;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Session;

// In SequencerManager.java - enhance to be a singleton
public class SequencerManager {

    private static SequencerManager instance;

    // Add timing state tracking
    private int currentTick = 0;
    private int currentBeat = 0;
    private int currentBar = 0;

    // Reference to current active session instead of duplicating parameters
    private Session activeSession = null;
    
    // These remain for initial/default values only
    private int defaultPpq = 24;
    private float defaultBpm = 120;
    private int metronomeChannel = 9;
    private int metronomeNote = 60;
    private int metronomeVelocity = 100;

    // Use singletons directly instead of constructor injection
    // private final LogManager logManager = LogManager.getInstance();
    private final TimingBus timingBus = TimingBus.getInstance();
    private final CommandBus commandBus = CommandBus.getInstance();

    private Sequence sequence;
    private Sequencer sequencer;
    private Synthesizer synthesizer;

    // Private constructor for singleton
    private SequencerManager() {
        initialize();
    }

    // Add static accessor
    public static SequencerManager getInstance() {
        if (instance == null) {
            instance = new SequencerManager();
        }
        return instance;
    }

    private void initialize() {
        try {
            setupSequencer();
            setupSynthesizer();
            createSequence();
            connectDevices();
            // logManager.info("SequencerManager initialized successfully");
        } catch (Exception e) {
            // logManager.error("Error initializing MIDI: " + e.getMessage());
        }
    }

    private void setupSequencer() throws MidiUnavailableException {
        sequencer = MidiSystem.getSequencer(false);
        sequencer.open();
    }

    private void setupSynthesizer() throws MidiUnavailableException {
        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
    }

    // Modified createSequence to support dynamic parameters
    private void createSequence() throws InvalidMidiDataException {
        createSequence(4); // Default 4/4 time
    }

    private void createSequence(int beatsPerBar) throws InvalidMidiDataException {
        sequence = new Sequence(Sequence.PPQ, getPpq());
        Track track = sequence.createTrack();

        // Add events for one bar with configurable beats per bar
        for (int beat = 0; beat < beatsPerBar; beat++) {
            // Add timing clocks
            for (int clock = 0; clock < getPpq(); clock++) {
                track.add(new MidiEvent(
                        new ShortMessage(0xF8),
                        beat * getPpq() + clock));
            }

            // Add metronome notes
            track.add(new MidiEvent(
                    new ShortMessage(ShortMessage.NOTE_ON, metronomeChannel, metronomeNote, metronomeVelocity),
                    beat * getPpq()));
            track.add(new MidiEvent(
                    new ShortMessage(ShortMessage.NOTE_OFF, metronomeChannel, metronomeNote, 0),
                    beat * getPpq() + getPpq() / 2));
        }

        sequencer.setSequence(sequence);
        sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        sequencer.setTempoInBPM(getBpm());
        sequencer.setMasterSyncMode(Sequencer.SyncMode.MIDI_SYNC);
        sequencer.setSlaveSyncMode(Sequencer.SyncMode.MIDI_SYNC);
    }

    private void connectDevices() throws MidiUnavailableException {
        // Create and connect timing receiver
        Receiver timingReceiver = new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof ShortMessage msg) {
                    if (msg.getStatus() == 0xF8) {
                        handleMidiClock();
                    }
                }
            }

            @Override
            public void close() {
            }
        };

        // Connect timing and audio
        Transmitter timingTransmitter = sequencer.getTransmitter();
        timingTransmitter.setReceiver(timingReceiver);

        Transmitter audioTransmitter = sequencer.getTransmitter();
        audioTransmitter.setReceiver(synthesizer.getReceiver());
    }

    // In SequencerManager.java - fix handleMidiClock() method
    private void handleMidiClock() {
        // First publish before-tick event 
        timingBus.publish(Commands.BEFORE_TICK, this, currentTick);
        
        // Increment tick counter
        currentTick = (currentTick + 1) % getPpq();
        
        // Publish standard tick event (for Players and Visualizations)
        timingBus.publish(Commands.BASIC_TIMING_TICK, this, currentTick);
        
        // Publish after-tick event
        timingBus.publish(Commands.AFTER_TICK, this, currentTick);
        
        // Handle beat change
        if (currentTick == 0) {
            // Before beat event
            timingBus.publish(Commands.BEFORE_BEAT, this, currentBeat);
            
            currentBeat = (currentBeat + 1) % getBeatsPerBar();  
            
            // Standard beat event
            timingBus.publish(Commands.BASIC_TIMING_BEAT, this, currentBeat);
            
            // After beat event
            timingBus.publish(Commands.AFTER_BEAT, this, currentBeat);
            
            // Handle bar change
            if (currentBeat == 0) {
                // Before bar event
                timingBus.publish(Commands.BEFORE_BAR, this, currentBar);
                
                currentBar++;
                
                // Standard bar event
                timingBus.publish(Commands.BASIC_TIMING_BAR, this, currentBar);
                
                // After bar event
                timingBus.publish(Commands.AFTER_BAR, this, currentBar);
            }
        }
    }

    public void start() {
        try {
            if (sequencer != null && !sequencer.isRunning()) {
                // Reset counters
                currentTick = 0;
                currentBeat = 0;
                currentBar = 0;
                
                // Start sequencer
                sequencer.start();
                
                // Notify about transport state change
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, true);
            }
        } catch (Exception e) {
            // Error handling
        }
    }

    public void stop() {
        try {
            if (sequencer != null && sequencer.isRunning()) {
                sequencer.stop();
                sequencer.setMicrosecondPosition(0);
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
                // logManager.info("Sequencer stopped");
            }
        } catch (Exception e) {
            // logManager.error("Error stopping sequencer: " + e.getMessage());
        }
    }

    public void cleanup() {
        try {
            if (sequencer != null) {
                sequencer.stop();
                sequencer.close();
            }
            if (synthesizer != null) {
                synthesizer.close();
            }
            // logManager.info("MIDI resources cleaned up");
        } catch (Exception e) {
            // logManager.error("Error cleaning up MIDI resources: " + e.getMessage());
        }
    }

    // Add state query methods
    public boolean isRunning() {
        return sequencer != null && sequencer.isRunning();
    }
    
    public int getCurrentTick() {
        return currentTick;
    }
    
    public int getCurrentBeat() {
        return currentBeat;
    }
    
    public int getCurrentBar() {
        return currentBar;
    }

    // Add a method to update timing parameters
    public void updateTimingParameters(float tempoInBPM, int ticksPerBeat, int beatsPerBar) {
        boolean wasRunning = isRunning();
        
        // If running, stop temporarily
        if (wasRunning) {
            sequencer.stop();
        }
        
        try {
            // Update our internal values
            this.defaultBpm = tempoInBPM;
            this.defaultPpq = ticksPerBeat;
            
            // Update sequencer parameters
            sequencer.setTempoInBPM(tempoInBPM);
            
            // Recreate the sequence with the new PPQ value if it changed
            createSequence(beatsPerBar);
            
            // Restart if it was running
            if (wasRunning) {
                sequencer.start();
            }
        } catch (Exception e) {
            // Handle error
        }
    }

    // Add getter methods that check activeSession first
    public int getPpq() {
        return (activeSession != null) ? activeSession.getTicksPerBeat() : defaultPpq;
    }
    
    public float getBpm() {
        return (activeSession != null) ? activeSession.getTempoInBPM() : defaultBpm;
    }
    
    public int getBeatsPerBar() {
        return (activeSession != null) ? activeSession.getBeatsPerBar() : 4;
    }
    
    // Update the method to set the active session
    public void setActiveSession(Session session) {
        this.activeSession = session;
        if (session != null) {
            // Apply settings from session
            updateSequencerSettings();
        }
    }
    
    // Method to apply current session settings to sequencer
    private void updateSequencerSettings() {
        if (activeSession == null) return;
        
        boolean wasRunning = isRunning();
        
        // If running, stop temporarily
        if (wasRunning) {
            sequencer.stop();
        }
        
        try {
            // Update sequencer parameters from session
            sequencer.setTempoInBPM(activeSession.getTempoInBPM());
            
            // Recreate sequence if PPQ changed
            createSequence(activeSession.getBeatsPerBar());
            
            // Restart if it was running
            if (wasRunning) {
                sequencer.start();
            }
        } catch (Exception e) {
            // Handle error
        }
    }
    
    // Replace syncFromSession with setActiveSession
    public void syncFromSession(Session session) {
        setActiveSession(session);
    }
}