package com.angrysurfer.beats.panel;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransportPanel extends JPanel implements CommandListener {
    private final JButton playButton;
    private final JButton stopButton;
    private final JButton recordButton;
    private final JButton rewindButton;
    private final JButton forwardButton;
    private final JButton pauseButton;
    private final CommandBus commandBus;
    private final JTextArea logArea;
    private final SimpleDateFormat timeFormat;
    private static final int MAX_LOG_LINES = 1000;
    private static final int PADDING = 12;

    private Sequence sequence;
    private Sequencer sequencer;
    private Synthesizer synthesizer;
    private static final int METRONOME_CHANNEL = 9; // Channel 10 (0-based)
    private static final int METRONOME_NOTE = 60; // Middle C
    private static final int METRONOME_VELOCITY = 100;
    private static final int PPQ = 24; // Pulses per quarter note
    private static final int BPM = 120; // Beats per minute

    // Add these fields after the existing constants
    private int currentTick = 0;
    private int currentBeat = 0;
    private int currentBar = 0;
    private static final int TICKS_PER_BEAT = PPQ;
    private static final int BEATS_PER_BAR = 4;

    public TransportPanel() {
        super(new BorderLayout(PADDING, PADDING));
        this.commandBus = CommandBus.getInstance();
        this.timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

        // Set panel padding
        setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Create transport controls panel
        JPanel transportControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        transportControls.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        transportControls.setPreferredSize(new Dimension(transportControls.getPreferredSize().width, 60));

        // Create transport buttons
        rewindButton = createTransportButton("⏮", Commands.TRANSPORT_REWIND, "Previous");
        pauseButton = createTransportButton("⏸", Commands.TRANSPORT_PAUSE, "Pause");
        stopButton = createTransportButton("⏹", Commands.TRANSPORT_STOP, "Stop");
        recordButton = createTransportButton("⏺", Commands.TRANSPORT_RECORD, "Record");
        playButton = createTransportButton("▶", Commands.TRANSPORT_PLAY, "Play");
        forwardButton = createTransportButton("⏭", Commands.TRANSPORT_FORWARD, "Next");

        // Add buttons to transport controls
        transportControls.add(rewindButton);
        transportControls.add(pauseButton);
        transportControls.add(stopButton);
        transportControls.add(recordButton);
        transportControls.add(playButton);
        transportControls.add(forwardButton);

        // Set initial button states
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
        pauseButton.setEnabled(false);
        recordButton.setEnabled(true);
        rewindButton.setEnabled(true);
        forwardButton.setEnabled(true);

        // Create log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(240, 240, 240));
        logArea.setForeground(new Color(50, 50, 50));
        logArea.setMargin(new Insets(8, 8, 8, 8));

        // Create scroll pane for log area
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0),
                BorderFactory.createLineBorder(new Color(200, 200, 200))));

        // Add components to main panel
        add(transportControls, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Register for command bus events
        commandBus.register(this);

        // Initialize MIDI
        initializeMIDI();

        // Log initial message
        log("Transport panel initialized");
    }

    private void initializeMIDI() {
        try {
            // Get and setup the sequencer first
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();

            // Get and open synthesizer
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();

            // Create sequence with metronome
            sequence = new Sequence(Sequence.PPQ, PPQ);
            Track track = sequence.createTrack();

            // Add metronome events and timing clocks for one bar
            for (int beat = 0; beat < 4; beat++) {
                // Add 24 timing clock messages per beat (standard MIDI spec)
                for (int clock = 0; clock < PPQ; clock++) {
                    track.add(new MidiEvent(
                        new ShortMessage(0xF8), // Timing Clock message
                        beat * PPQ + clock));
                }
                
                // Add note events
                track.add(new MidiEvent(
                    new ShortMessage(ShortMessage.NOTE_ON, METRONOME_CHANNEL, METRONOME_NOTE, METRONOME_VELOCITY),
                    beat * PPQ));
                track.add(new MidiEvent(
                    new ShortMessage(ShortMessage.NOTE_OFF, METRONOME_CHANNEL, METRONOME_NOTE, 0),
                    beat * PPQ + PPQ/2));
            }

            // Set up sequencer
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.setTempoInBPM(BPM);

            // Important: Set sync modes before creating transmitters/receivers
            sequencer.setMasterSyncMode(Sequencer.SyncMode.MIDI_SYNC);
            sequencer.setSlaveSyncMode(Sequencer.SyncMode.MIDI_SYNC);

            log("Master mode: " + sequencer.getMasterSyncMode());
            log("Slave mode: " + sequencer.getSlaveSyncMode());

            // Create timing receiver first
            Receiver timingReceiver = new Receiver() {
                @Override
                public void send(MidiMessage message, long timeStamp) {
                    if (message instanceof ShortMessage msg) {
                        int status = msg.getStatus();
                        switch (status) {
                            case 0xF8 -> handleTimingClock();  // MIDI Clock
                            case 0xFA -> handleStart();        // Start
                            case 0xFC -> handleStop();         // Stop
                            case 0xFB -> handleContinue();     // Continue
                            default -> {
                                if (msg.getCommand() == ShortMessage.NOTE_ON || 
                                    msg.getCommand() == ShortMessage.NOTE_OFF) {
                                    log(String.format("Note %s Ch:%d N:%d V:%d", 
                                        msg.getCommand() == ShortMessage.NOTE_ON ? "ON" : "OFF",
                                        msg.getChannel() + 1, 
                                        msg.getData1(), 
                                        msg.getData2()));
                                }
                            }
                        }
                    }
                }
                @Override
                public void close() {}
            };

            // Connect timing transmitter first
            Transmitter timingTransmitter = sequencer.getTransmitter();
            timingTransmitter.setReceiver(timingReceiver);

            // Then connect audio transmitter
            Transmitter audioTransmitter = sequencer.getTransmitter();
            audioTransmitter.setReceiver(synthesizer.getReceiver());

            log("MIDI initialized with timing and audio receivers");

        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            log("Error initializing MIDI: " + e.getMessage());
        }
    }

    private void startMetronome() {
        try {
            if (sequencer != null && !sequencer.isRunning()) {
                sequencer.start();
                log("Metronome started");
            }
        } catch (Exception e) {
            log("Error starting metronome: " + e.getMessage());
        }
    }

    private void stopMetronome() {
        try {
            if (sequencer != null && sequencer.isRunning()) {
                sequencer.stop();
                sequencer.setMicrosecondPosition(0); // Reset position to start
                log("Metronome stopped");
            }
        } catch (Exception e) {
            log("Error stopping metronome: " + e.getMessage());
        }
    }

    private void cleanupMIDI() {
        try {
            if (sequencer != null) {
                sequencer.stop();
                sequencer.close();
            }
            if (synthesizer != null) {
                synthesizer.close();
            }
            log("MIDI resources cleaned up");
        } catch (Exception e) {
            log("Error cleaning up MIDI resources: " + e.getMessage());
        }
    }

    private JButton createTransportButton(String symbol, String command, String tooltip) {
        JButton button = new JButton(symbol) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Calculate center position
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(getText());
                int textHeight = fm.getHeight();
                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() + textHeight) / 2 - fm.getDescent();

                // Draw background if button is pressed/selected
                if (getModel().isPressed()) {
                    g2.setColor(getBackground().darker());
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                // Draw the text
                g2.setColor(getForeground());
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };

        button.setToolTipText(tooltip);
        button.setEnabled(true);
        button.setActionCommand(command);
        button.addActionListener(this::handleButtonAction);

        // Adjust size and font
        int size = 32;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));

        // Fallback font if needed
        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }

        button.setMargin(new Insets(0, 0, 0, 0));
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setFocusPainted(false);

        return button;
    }

    private void handleButtonAction(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case Commands.TRANSPORT_PLAY -> {
                startMetronome();
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, true);
            }
            case Commands.TRANSPORT_STOP -> {
                stopMetronome();
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
            }
            case Commands.TRANSPORT_PAUSE -> {
                stopMetronome();
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
            }
        }
        commandBus.publish(command, this);
        log("Transport command: " + command);
    }

    private void handleTimingClock() {
        currentTick++;
        if (currentTick >= TICKS_PER_BEAT) {
            currentTick = 0;
            currentBeat++;
            if (currentBeat >= BEATS_PER_BAR) {
                currentBeat = 0;
                currentBar++;
                log(String.format("Bar: %d Beat: %d Tick: %d", currentBar, currentBeat, currentTick));
            } else {
                log(String.format("Beat: %d Tick: %d", currentBeat, currentTick));
            }
        }
        // Only log every 6 ticks (quarter of a beat) to reduce spam
        else if (currentTick % 6 == 0) {
            log(String.format("Tick: %d", currentTick));
        }
    }

    private void handleStart() {
        currentTick = 0;
        currentBeat = 0;
        currentBar = 0;
        log("Sequencer started");
    }

    private void handleStop() {
        log("Sequencer stopped");
    }

    private void handleContinue() {
        log("Sequencer continued");
    }

    /**
     * Logs a message to the text area with timestamp
     */
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = timeFormat.format(new Date());
            String logMessage = String.format("[%s] %s%n", timestamp, message);
            logArea.append(logMessage);

            // Trim log if it gets too long
            if (logArea.getLineCount() > MAX_LOG_LINES) {
                try {
                    int endOfFirstLine = logArea.getLineEndOffset(logArea.getLineCount() - MAX_LOG_LINES);
                    logArea.replaceRange("", 0, endOfFirstLine);
                } catch (Exception e) {
                    // Ignore any errors during trimming
                }
            }

            // Scroll to bottom
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Clears the log area
     */
    public void clearLog() {
        SwingUtilities.invokeLater(() -> logArea.setText(""));
    }

    @Override
    public void onAction(Command action) {
        switch (action.getCommand()) {
            case Commands.TRANSPORT_STATE_CHANGED -> {
                if (action.getData() instanceof Boolean isPlaying) {
                    SwingUtilities.invokeLater(() -> {
                        playButton.setEnabled(!isPlaying);
                        stopButton.setEnabled(isPlaying);
                        pauseButton.setEnabled(isPlaying);
                        if (isPlaying) {
                            startMetronome();
                        } else {
                            stopMetronome();
                        }
                    });
                    log("Transport state changed: " + (isPlaying ? "Playing" : "Stopped"));
                }
            }
            case Commands.TRANSPORT_STOP -> {
                stopMetronome();
                SwingUtilities.invokeLater(() -> {
                    playButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    pauseButton.setEnabled(false);
                });
                log("Transport stopped");
            }
            case Commands.TRANSPORT_RECORD_STATE_CHANGED -> {
                if (action.getData() instanceof Boolean isRecording) {
                    SwingUtilities.invokeLater(() -> {
                        recordButton.setEnabled(!isRecording);
                    });
                    log("Record state changed: " + (isRecording ? "Recording" : "Stopped"));
                }
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        cleanupMIDI();
        super.finalize();
    }
}