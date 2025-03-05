package com.angrysurfer.beats.panel;

import java.util.function.Consumer;
import java.util.logging.Logger;

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

// Inner class to manage MIDI sequencer functionality
class SequencerManager {

    private static final int PPQ = 24;
    private static final int BPM = 120;
    private static final int METRONOME_CHANNEL = 9;
    private static final int METRONOME_NOTE = 60;
    private static final int METRONOME_VELOCITY = 100;

    private final Consumer<String> logger;
    private final Runnable clockHandler;
    private Sequence sequence;
    private Sequencer sequencer;
    private Synthesizer synthesizer;

    public SequencerManager(Consumer<String> logger, Runnable clockHandler) {
        this.logger = logger;
        this.clockHandler = clockHandler;
        initialize();
    }

    private void initialize() {
        try {
            setupSequencer();
            setupSynthesizer();
            createSequence();
            connectDevices();
        } catch (Exception e) {
            logger.accept("Error initializing MIDI: " + e.getMessage());
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

    private void createSequence() throws InvalidMidiDataException {
        sequence = new Sequence(Sequence.PPQ, PPQ);
        Track track = sequence.createTrack();

        // Add events for one bar
        for (int beat = 0; beat < 4; beat++) {
            // Add timing clocks
            for (int clock = 0; clock < PPQ; clock++) {
                track.add(new MidiEvent(
                        new ShortMessage(0xF8),
                        beat * PPQ + clock));
            }

            // Add metronome notes
            track.add(new MidiEvent(
                    new ShortMessage(ShortMessage.NOTE_ON, METRONOME_CHANNEL, METRONOME_NOTE, METRONOME_VELOCITY),
                    beat * PPQ));
            track.add(new MidiEvent(
                    new ShortMessage(ShortMessage.NOTE_OFF, METRONOME_CHANNEL, METRONOME_NOTE, 0),
                    beat * PPQ + PPQ / 2));
        }

        sequencer.setSequence(sequence);
        sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        sequencer.setTempoInBPM(BPM);
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
                        clockHandler.run();
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

    public void start() {
        try {
            if (sequencer != null && !sequencer.isRunning()) {
                sequencer.start();
                logger.accept("Sequencer started");
            }
        } catch (Exception e) {
            logger.accept("Error starting sequencer: " + e.getMessage());
        }
    }

    public void stop() {
        try {
            if (sequencer != null && sequencer.isRunning()) {
                sequencer.stop();
                sequencer.setMicrosecondPosition(0);
                logger.accept("Sequencer stopped");
            }
        } catch (Exception e) {
            logger.accept("Error stopping sequencer: " + e.getMessage());
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
            logger.accept("MIDI resources cleaned up");
        } catch (Exception e) {
            logger.accept("Error cleaning up MIDI resources: " + e.getMessage());
        }
    }
}