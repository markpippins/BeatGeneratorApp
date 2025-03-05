package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.component.LedIndicator;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;

public class TransportPanel extends JPanel implements CommandListener {
    // Add CommandBus alongside TimingBus
    private final CommandBus commandBus;
    private final TimingBus timeBus;
    private final JButton playButton;
    private final JButton stopButton;
    private final JButton recordButton;
    private final JButton rewindButton;
    private final JButton forwardButton;
    private final JButton pauseButton;
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

    private final LedIndicator tickLed;
    private final LedIndicator beatLed;
    private final LedIndicator barLed;

    public TransportPanel() {
        super(new BorderLayout(PADDING, PADDING));
        // Initialize both buses
        this.commandBus = CommandBus.getInstance();
        this.timeBus = TimingBus.getInstance();
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
        add(new JScrollPane(new GridPanel(null)), BorderLayout.CENTER);

        // Create indicator panel
        JPanel indicatorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        indicatorPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // Create LEDs
        tickLed = new LedIndicator(new Color(255, 50, 50)); // Red
        beatLed = new LedIndicator(new Color(50, 255, 50)); // Green
        barLed = new LedIndicator(new Color(50, 50, 255)); // Blue

        // Add labels and LEDs
        indicatorPanel.add(new JLabel("T"));
        indicatorPanel.add(tickLed);
        indicatorPanel.add(Box.createHorizontalStrut(8));
        indicatorPanel.add(new JLabel("B"));
        indicatorPanel.add(beatLed);
        indicatorPanel.add(Box.createHorizontalStrut(8));
        indicatorPanel.add(new JLabel("M"));
        indicatorPanel.add(barLed);

        // Add indicator panel to bottom
        add(indicatorPanel, BorderLayout.SOUTH);

        // Register for command bus events
        timeBus.register(this);

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
                        beat * PPQ + PPQ / 2));
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
                        // Handle timing messages silently
                        switch (status) {
                            case 0xF8 -> handleTimingClock(); // MIDI Clock
                            case 0xFA -> handleStart(); // Start
                            case 0xFC -> handleStop(); // Stop
                            case 0xFB -> handleContinue(); // Continue
                            default -> {
                                // Log regular MIDI messages
                                String messageType = switch (msg.getCommand()) {
                                    case ShortMessage.NOTE_ON -> "Note ON";
                                    case ShortMessage.NOTE_OFF -> "Note OFF";
                                    case ShortMessage.CONTROL_CHANGE -> "Control Change";
                                    case ShortMessage.PROGRAM_CHANGE -> "Program Change";
                                    case ShortMessage.PITCH_BEND -> "Pitch Bend";
                                    case ShortMessage.CHANNEL_PRESSURE -> "Channel Pressure";
                                    case ShortMessage.POLY_PRESSURE -> "Poly Pressure";
                                    default -> String.format("Unknown (0x%02X)", msg.getCommand());
                                };
                                log(String.format("MIDI %s Ch:%d Data1:%d Data2:%d",
                                        messageType,
                                        msg.getChannel() + 1,
                                        msg.getData1(),
                                        msg.getData2()));
                            }
                        }
                    }
                }

                @Override
                public void close() {
                }
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
                // Send state change to both buses - timing bus for sequencer, command bus for UI
                timeBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, true);
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, true);
                // Send play command to command bus for visualizer
                commandBus.publish(Commands.TRANSPORT_PLAY, this);
            }
            case Commands.TRANSPORT_STOP -> {
                stopMetronome();
                litTick = false;
                litBeat = false;
                litBar = false;
                timeBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
                commandBus.publish(Commands.TRANSPORT_STOP, this);
            }
            case Commands.TRANSPORT_PAUSE -> {
                stopMetronome();
                timeBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
                commandBus.publish(Commands.TRANSPORT_PAUSE, this);
            }
        }
    }

    private void handleTimingClock() {
        // Only timing events go on timing bus
        timeBus.publish(Commands.BASIC_TIMING_TICK, this);
        
        currentTick++;
        flashTickLed(tickLed);

        if (currentTick >= TICKS_PER_BEAT) {
            currentTick = 0;
            currentBeat++;
            flashBeatLed(beatLed);
            timeBus.publish(Commands.BASIC_TIMING_BEAT, this);

            if (currentBeat >= BEATS_PER_BAR) {
                currentBeat = 0;
                currentBar++;
                flashBarLed(barLed);
            }
        }
    }

    boolean litTick = false;
    boolean litBeat = false;
    boolean litBar = false;

    private void flashTickLed(LedIndicator led) {
        litTick = !litTick;
        led.setLit(litTick);
    }

    private void flashBeatLed(LedIndicator led) {
        litBeat = !litBeat;
        led.setLit(litBeat);
    }

    private void flashBarLed(LedIndicator led) {
        litBar = !litBar;
        led.setLit(litBar);
    }

    private void flashLed(LedIndicator led) {
        // SwingUtilities.invokeLater(() -> {
        // led.setLit(true);
        // Timer timer = new Timer(50, e -> led.setLit(false));
        // // timer.setRepeats(false);
        // // timer.start();
        // });
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