package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.SessionManager;

class X0XPanel extends StatusProviderPanel implements IBusListener {
    private final List<TriggerButton> triggerButtons = new ArrayList<>();
    private final List<NoteSelectionDial> noteDials = new ArrayList<>();

    private boolean isPlaying = false;
    private int currentStep = 0;

    // Add a class member for timer-based stepping
    private javax.swing.Timer stepTimer;

    private Synthesizer synthesizer = null;

    public X0XPanel() {
        super(new BorderLayout());
        setStatusConsumer(statusConsumer);

        // Initialize the synthesizer
        initializeSynthesizer();

        // Register with both buses
        TimingBus.getInstance().register(this);
        CommandBus.getInstance().register(this);
        setup();

        // Initialize the step timer
        setupStepTimer();
    }

    // Add this method to set up the timer
    private void setupStepTimer() {
        // Start with 120 BPM = 500ms per beat = 125ms per step (for 4 steps per beat)
        int initialMsPerStep = 125;

        stepTimer = new javax.swing.Timer(initialMsPerStep, e -> {
            if (isPlaying) {
                int nextStep = (currentStep + 1) % 16;
                updateStep(currentStep, nextStep);
                currentStep = nextStep;
            }
        });
        stepTimer.setRepeats(true);
    }

    private void initializeSynthesizer() {
        try {
            // Get information about all available MIDI devices
            MidiSystem.getMidiDeviceInfo();
            
            // Look for Gervill specifically
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            MidiDevice.Info gervillInfo = null;
            
            for (MidiDevice.Info info : infos) {
                if (info.getName().contains("Gervill")) {
                    gervillInfo = info;
                    break;
                }
            }
            
            // If we found Gervill, use it
            if (gervillInfo != null) {
                MidiDevice device = MidiSystem.getMidiDevice(gervillInfo);
                if (device instanceof Synthesizer) {
                    synthesizer = (Synthesizer)device;
                }
            }
            
            // If we didn't find Gervill specifically, just get the default synthesizer
            if (synthesizer == null) {
                synthesizer = MidiSystem.getSynthesizer();
            }
            
            // Open the synthesizer
            if (synthesizer != null && !synthesizer.isOpen()) {
                synthesizer.open();
                System.out.println("Opened synthesizer: " + synthesizer.getDeviceInfo().getName());
            }
            
            // Configure to use channel 16 (index 15)
            if (synthesizer != null && synthesizer.isOpen()) {
                // Get channel 16 (index 15)
                MidiChannel channel = synthesizer.getChannels()[15];
                
                // Basic configuration - ensure it's not muted
                if (channel != null) {
                    channel.controlChange(7, 100); // Set volume to 100
                    channel.controlChange(10, 64); // Pan center
                    channel.programChange(0);      // Default program (Grand Piano)
                    System.out.println("Configured channel 16 (index 15) on synthesizer");
                    
                    // Get name of the current program
                    String presetName = InternalSynthManager.getInstance().getPresetName(1L, 0);
                    System.out.println("Initial preset: " + presetName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing synthesizer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null)
            return;
            
        // Ensure timer is initialized before using it
        if (stepTimer == null) {
            setupStepTimer();
        }

        switch (action.getCommand()) {
        case Commands.TRANSPORT_PLAY -> {
            isPlaying = true;
            currentStep = 0;

            // Reset the step timer whenever tempo changes
            int currentBPM = 120; // Default
            try {
                Session session = SessionManager.getInstance().getActiveSession();
                if (session != null) {
                    currentBPM = Math.round(session.getTempoInBPM());
                }
            } catch (Exception ex) {
                // Use default if can't get session
            }

            // Calculate ms per step: 60000ms/min ÷ BPM = ms/beat, then divide by 4
            // steps/beat
            int msPerBeat = 60000 / currentBPM;
            int msPerStep = msPerBeat / 4;
            stepTimer.setDelay(msPerStep);

            // Start the timer
            stepTimer.start();

            SwingUtilities.invokeLater(() -> {
                // Reset all buttons first
                for (TriggerButton button : triggerButtons) {
                    button.setHighlighted(false);
                }
                // Highlight first step
                if (!triggerButtons.isEmpty()) {
                    triggerButtons.get(0).setHighlighted(true);
                }
            });
        }

        case Commands.TRANSPORT_STOP -> {
            isPlaying = false;
            // Stop the timer
            if (stepTimer != null) {
                stepTimer.stop();
            }
            resetSequence();
        }

        case Commands.TIMING_BEAT -> {
            // Just use for tempo synchronization
            if (isPlaying && action.getData() instanceof Number beatNum) {
                // Processing if needed
            }
        }

        case Commands.SESSION_UPDATED -> {
            // Update tempo when session changes
            if (action.getData() instanceof Session session) {
                int bpm = Math.round(session.getTempoInBPM());
                int msPerBeat = 60000 / bpm;
                int msPerStep = msPerBeat / 4;
                
                // Ensure timer exists before setting delay
                if (stepTimer != null) {
                    stepTimer.setDelay(msPerStep);
                }
            }
        }
        }
    }

    private void updateStep(int oldStep, int newStep) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Clear previous step highlight
                if (oldStep >= 0 && oldStep < triggerButtons.size()) {
                    TriggerButton oldButton = triggerButtons.get(oldStep);
                    oldButton.setHighlighted(false);
                }

                // Highlight current step
                if (newStep >= 0 && newStep < triggerButtons.size()) {
                    TriggerButton newButton = triggerButtons.get(newStep);
                    newButton.setHighlighted(true);
                    
                    if (getStatusConsumer() != null) {
                        getStatusConsumer().setStatus("Step: " + (newStep + 1) + " of 16");
                    }
                    
                    // Play note for current step if we're playing AND the trigger is active
                    if (isPlaying && newStep < noteDials.size() && newButton.isActive()) {
                        NoteSelectionDial noteDial = noteDials.get(newStep);
                        int noteValue = noteDial.getValue();
                        
                        // Get velocity from second dial if available (default 100)
                        int velocity = 100;
                        // Get gate time from third dial if available (default 100ms)
                        int gateTime = 100;
                        
                        // Play the note
                        playNote(noteValue, velocity, gateTime);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating X0X step: " + e.getMessage());
            }
        });
    }

    private void resetSequence() {
        currentStep = 0;
        SwingUtilities.invokeLater(() -> {
            // Clear all highlights when stopped
            for (TriggerButton button : triggerButtons) {
                button.setHighlighted(false);
            }
        });
    }

    private void updateTriggerButtons() {
        try {
            // First clear all button highlights
            for (TriggerButton button : triggerButtons) {
                button.setHighlighted(false);
            }

            // Then highlight only the current step
            if (isPlaying && currentStep >= 0 && currentStep < triggerButtons.size()) {
                triggerButtons.get(currentStep).setHighlighted(true);

                // Update status
                if (getStatusConsumer() != null) {
                    getStatusConsumer().setStatus("Step: " + (currentStep + 1));
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating trigger buttons: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for better debugging
        }
    }

    private void setup() {
        // Create a container panel with BorderLayout
        JPanel containerPanel = new JPanel(new BorderLayout(10, 10));
        containerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create synth preset panel at the top
        JPanel presetPanel = createPresetPanel();
        containerPanel.add(presetPanel, BorderLayout.NORTH);
        
        // Create X0X sequencer panel in center
        JPanel x0xPanel = createX0XPanel();
        containerPanel.add(new JScrollPane(x0xPanel), BorderLayout.CENTER);
        
        // Create control buttons at the bottom
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetSequence());
        
        JButton playButton = new JButton("Play");
        playButton.addActionListener(e -> {
            isPlaying = !isPlaying;
            playButton.setText(isPlaying ? "Stop" : "Play");
            
            if (isPlaying) {
                stepTimer.start();
            } else {
                stepTimer.stop();
                resetSequence();
            }
        });
        
        controlPanel.add(resetButton);
        controlPanel.add(playButton);
        
        containerPanel.add(controlPanel, BorderLayout.SOUTH);
        
        add(containerPanel);
    }

    private JPanel createX0XPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create panel for the 16 columns
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 5, 0));
        sequencePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            sequencePanel.add(columnPanel);
        }

        // Wrap in scroll pane in case window gets too small
        JScrollPane scrollPane = new JScrollPane(sequencePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

        // Add 4 knobs
        for (int i = 0; i < 4; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setForeground(Color.GRAY);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (i > 0) {
                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                labelPanel.add(label);
                // Add label to the column
                column.add(labelPanel);
            }

            // Create dial - first one is always a NoteSelectionDial
            Dial dial;
            if (i == 0) {
                NoteSelectionDial noteDial = new NoteSelectionDial();
                noteDials.add(noteDial); // Store the note dial for this column
                dial = noteDial;
            } else {
                dial = new Dial();
            }
            
            dial.setUpdateOnResize(false);
            dial.setToolTipText(String.format("Step %d Knob %d", index + 1, i + 1));
            dial.setName("JDial-" + index + "-" + i);

            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);

            // Add small spacing between knobs
            column.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        
        // Add the trigger button - make it a toggle button
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));
        
        // Make it toggleable
        triggerButton.setToggleable(true);

        // Add a clean action listener that doesn't interfere with toggle behavior
        triggerButton.addActionListener(e -> {
            // No need to manually toggle - JToggleButton handles it automatically
            System.out.println("Trigger " + index + " is now " + 
                               (triggerButton.isSelected() ? "ON" : "OFF"));
        });

        triggerButtons.add(triggerButton);
        // Center the button horizontally
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton);
        column.add(buttonPanel1);

        // Add the pad button
        JButton padButton = new DrumButton(); // createPadButton(index);
        padButton.setName("PadButton-" + index);
        padButton.setToolTipText("Pad " + (index + 1));
        padButton.setText(Integer.toString(index + 1));

        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel2.add(padButton);
        column.add(buttonPanel2);

        return column;
    }

    private String getKnobLabel(int i) {
        return i == 0 ? "Note" : i == 1 ? "Vel." : i == 2 ? "Gate" : "Prob.";
    }

    private JPanel createPresetPanel() {
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        presetPanel.setBorder(BorderFactory.createTitledBorder("Synth Presets"));
        
        JButton presetButton1 = new JButton("Preset 1");
        JButton presetButton2 = new JButton("Preset 2");
        JButton presetButton3 = new JButton("Preset 3");
        
        presetPanel.add(presetButton1);
        presetPanel.add(presetButton2);
        presetPanel.add(presetButton3);
        
        return presetPanel;
    }

    /**
     * Play a note on the synthesizer
     * 
     * @param note MIDI note number (0-127)
     * @param velocity Velocity value (0-127)
     * @param durationMs Duration in milliseconds
     */
    private void playNote(int note, int velocity, int durationMs) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                // Play on channel 16 (index 15)
                MidiChannel channel = synthesizer.getChannels()[15];
                
                if (channel != null) {
                    // Start the note
                    channel.noteOn(note, velocity);
                    
                    // Log the note being played
                    System.out.println("Playing note: " + note + " with velocity: " + velocity + 
                                       " for " + durationMs + "ms");
                    
                    // Schedule note off
                    java.util.Timer timer = new java.util.Timer();
                    timer.schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            channel.noteOff(note);
                            timer.cancel();
                        }
                    }, durationMs);
                }
            } catch (Exception e) {
                System.err.println("Error playing note: " + e.getMessage());
            }
        }
    }
}
