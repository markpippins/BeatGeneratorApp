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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
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
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> gateDials = new ArrayList<>();

    private boolean isPlaying = false;
    private int currentStep = 0;

    // Add these fields for tick-based timing
    private int stepCounter = 0; // Current step in X0X pattern (0-15)
    private int tickCounter = 0; // Count ticks within current step
    private int ticksPerStep = 6; // How many ticks make one X0X step
    private int nextStepTick = 0; // When to trigger the next step

    private Synthesizer synthesizer = null;

    // Add this field to X0XPanel
    private JComboBox<PresetItem> presetCombo;

    // Add these fields to X0XPanel
    private int latencyCompensation = 20; // milliseconds to compensate for system latency

    // Add these fields to X0XPanel
    private int lookAheadMs = 40; // How far ahead to schedule notes
    private boolean useAheadScheduling = true; // Enable/disable look-ahead

    // Constructor - remove the stepTimer initialization
    public X0XPanel() {
        super(new BorderLayout());
        setStatusConsumer(statusConsumer);

        // Initialize the synthesizer
        initializeSynthesizer();

        // Register with both buses
        TimingBus.getInstance().register(this);
        CommandBus.getInstance().register(this);
        setup();

        // Calculate initial timing
        updateTimingParameters();
    }

    // Add a method to calculate timing parameters
    private void updateTimingParameters() {
        try {
            Session session = SessionManager.getInstance().getActiveSession();
            if (session != null) {
                // Calculate ticks per step based on session settings
                int ppq = session.getTicksPerBeat(); // Pulses per quarter note
                int stepsPerBeat = 4; // Standard X0X uses 4 steps per beat

                // Calculate timing parameters
                ticksPerStep = ppq / stepsPerBeat;
                nextStepTick = ticksPerStep; // Reset next step counter

                System.out.println("X0X timing: " + ticksPerStep + " ticks per step");
            }
        } catch (Exception ex) {
            // Use default values if something goes wrong
            ticksPerStep = 6;
            nextStepTick = ticksPerStep;
        }
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
                    synthesizer = (Synthesizer) device;
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
                    channel.programChange(0); // Default program (Grand Piano)
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

        switch (action.getCommand()) {
        case Commands.TRANSPORT_PLAY -> {
            isPlaying = true;

            // Reset all counters
            stepCounter = 0;
            tickCounter = 0;
            nextStepTick = ticksPerStep;

            // Update timing parameters in case they changed
            updateTimingParameters();

            // Reset and highlight the first step
            SwingUtilities.invokeLater(() -> {
                // Reset all buttons
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
            resetSequence();
        }

        case Commands.TIMING_TICK -> {
            // Process timing ticks for sequencing
            if (isPlaying && action.getData() instanceof Number) {
                tickCounter++;

                // Check if it's time for the next step
                if (tickCounter >= nextStepTick) {
                    // Calculate the next step
                    int oldStep = stepCounter;
                    stepCounter = (stepCounter + 1) % 16;

                    // Play the step and update UI
                    updateStepAndPlayNote(oldStep, stepCounter);

                    // Reset tick counter and calculate next step time
                    tickCounter = 0;
                    nextStepTick = ticksPerStep;
                }
            }
        }

        case Commands.SESSION_UPDATED -> {
            // Update timing parameters when session changes
            if (action.getData() instanceof Session) {
                updateTimingParameters();
            }
        }
        }
    }

    private void updateStepAndPlayNote(int oldStep, int newStep) {
        // First check if we need to play a note, and do that immediately with minimum
        // latency
        if (isPlaying && newStep >= 0 && newStep < triggerButtons.size() && newStep < noteDials.size()) {
            TriggerButton newButton = triggerButtons.get(newStep);
            if (newButton.isSelected()) { // Check if the trigger is active (selected)
                // Get note value
                NoteSelectionDial noteDial = noteDials.get(newStep);
                int noteValue = noteDial.getValue();

                // Get velocity from velocity dial
                int velocity = 100; // Default
                if (newStep < velocityDials.size()) {
                    // Scale dial value (0-100) to MIDI velocity range (0-127)
                    velocity = (int) Math.round(velocityDials.get(newStep).getValue() * 1.27);
                    // Ensure it's within valid MIDI range
                    velocity = Math.max(1, Math.min(127, velocity));
                }

                // Get gate time from gate dial
                int gateTime = 100; // Default (ms)
                if (newStep < gateDials.size()) {
                    // Scale dial value (0-100) to reasonable gate times (10-500ms)
                    gateTime = (int) Math.round(10 + gateDials.get(newStep).getValue() * 4.9);
                }

                // Play note with the parameters from all three dials
                playNote(noteValue, velocity, gateTime);
            }
        }

        // Then update UI (less time-critical)
        SwingUtilities.invokeLater(() -> {
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
        JTabbedPane x0xPanel = createX0XPanel();
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
                // Start playing
            } else {
                resetSequence();
            }
        });

        controlPanel.add(resetButton);
        controlPanel.add(playButton);

        containerPanel.add(controlPanel, BorderLayout.SOUTH);

        add(containerPanel);
    }

    private JTabbedPane createX0XPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Sequence", createSequencerPanel());
        tabbedPane.addTab("Instrument", createInstrumentPanel());
        tabbedPane.addTab("Settings", createSoundBankPanel());
        tabbedPane.addTab("About", createEffectsPanel());

        return tabbedPane;

    }

    private Component createSequencerPanel() {
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

    private Component createEffectsPanel() {
        return null;
    }

    private Component createSoundBankPanel() {
        return null;
    }

    private Component createInstrumentPanel() {
        return null;
    }

    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

        // Add 4 knobs
        for (int i = 0; i < 3; i++) {
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
            Dial dial = i == 0 ? new NoteSelectionDial() : new Dial();

            // Store the dial in the appropriate collection based on its type
            switch (i) {
            case 0:
                noteDials.add((NoteSelectionDial) dial); // Store the note dial for this column
                break; // <-- Missing break statement causing case 1 to execute too!

            case 1:
                velocityDials.add(dial); // Store the velocity dial
                break;

            case 2:
                gateDials.add(dial); // Store the gate dial
                break;

            default:
                // Probability dial - could be stored in a separate collection if needed
                break;
            }

            dial.setUpdateOnResize(false);
            dial.setToolTipText(String.format("Step %d %s", index + 1, getKnobLabel(i)));
            dial.setName("JDial-" + index + "-" + i);

            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }
        // Add small spacing between knobs
        column.add(Box.createRigidArea(new Dimension(0, 5)));

        // Add the trigger button - make it a toggle button
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));

        // Make it toggleable
        triggerButton.setToggleable(true);

        // Add a clean action listener that doesn't interfere with toggle behavior
        triggerButton.addActionListener(e -> {
            // No need to manually toggle - JToggleButton handles it automatically
            System.out.println("Trigger " + index + " is now " + (triggerButton.isSelected() ? "ON" : "OFF"));
        });

        triggerButtons.add(triggerButton);
        // Center the button horizontally
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton);
        column.add(buttonPanel1);

        // Add the pad button
        JButton padButton = new DrumButton();
        padButton.setName("PadButton-" + index);
        padButton.setToolTipText("Pad " + (index + 1));
        padButton.setText(Integer.toString(index + 1));

        // Add action to manually trigger the note when pad button is clicked
        padButton.addActionListener(e -> {
            if (index < noteDials.size()) {
                // Get note from dial
                NoteSelectionDial noteDial = noteDials.get(index);
                int noteValue = noteDial.getValue();

                // Get velocity
                int velocity = 127; // Full velocity for manual triggers
                if (index < velocityDials.size()) {
                    velocity = (int) Math.round(velocityDials.get(index).getValue() * 1.27);
                    velocity = Math.max(1, Math.min(127, velocity));
                }

                // Get gate time
                int gateTime = 250; // Longer gate time for manual triggers
                if (index < gateDials.size()) {
                    gateTime = (int) Math.round(50 + gateDials.get(index).getValue() * 4.5);
                }

                // Play the note with velocity and gate time from dials
                playNote(noteValue, velocity, gateTime);
            }
        });

        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel2.add(padButton);
        column.add(buttonPanel2);

        return column;
    }

    private String getKnobLabel(int i) {
        return i == 0 ? "Note" : i == 1 ? "Vel." : i == 2 ? "Gate" : "Prob.";
    }

    /**
     * Inner class to represent preset items in the combo box
     */
    private static class PresetItem {
        private final int number;
        private final String name;

        public PresetItem(int number, String name) {
            this.number = number;
            this.name = name;
        }

        public int getNumber() {
            return number;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Create a panel with a preset selector combo box
     */
    private JPanel createPresetPanel() {
        JPanel presetPanel = new JPanel(new BorderLayout(5, 5));
        presetPanel.setBorder(BorderFactory.createTitledBorder("Synth Presets"));

        // Create preset combo box
        presetCombo = new JComboBox<>();
        populatePresetCombo();

        // Add listener to change synth preset when selected
        presetCombo.addActionListener(e -> {
            if (presetCombo.getSelectedItem() instanceof PresetItem) {
                PresetItem item = (PresetItem) presetCombo.getSelectedItem();
                setProgramChange(item.getNumber());
                System.out.println("Selected preset: " + item.getName() + " (#" + item.getNumber() + ")");
            }
        });

        // Add a label and the combo box to the panel
        JPanel innerPanel = new JPanel(new BorderLayout(5, 0));
        innerPanel.add(new JLabel("Preset:"), BorderLayout.WEST);
        innerPanel.add(presetCombo, BorderLayout.CENTER);

        presetPanel.add(innerPanel, BorderLayout.NORTH);

        return presetPanel;
    }

    /**
     * Load presets for the current instrument into the combo box
     */
    private void populatePresetCombo() {
        presetCombo.removeAllItems();

        // Get preset names from InternalSynthManager - use GM synth ID (1)
        List<String> presetNames = InternalSynthManager.getInstance().getPresetNames(1L);

        // Always add all 128 GM presets
        for (int i = 0; i < 128; i++) {
            // Get name from the list if available, otherwise use generic name
            String presetName;
            if (i < presetNames.size() && presetNames.get(i) != null && !presetNames.get(i).isEmpty()) {
                presetName = presetNames.get(i);
            } else {
                presetName = "Program " + i;
            }

            // Add the preset to the combo box
            presetCombo.addItem(new PresetItem(i, i + ": " + presetName));
        }

        // Select the first preset by default
        if (presetCombo.getItemCount() > 0) {
            presetCombo.setSelectedIndex(0);
        }
    }

    /**
     * Send program change to the synthesizer
     */
    private void setProgramChange(int program) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                // Use channel 16 (index 15)
                MidiChannel channel = synthesizer.getChannels()[15];

                if (channel != null) {
                    channel.programChange(program);
                    System.out.println("Changed synth program to " + program);
                }
            } catch (Exception e) {
                System.err.println("Error changing program: " + e.getMessage());
            }
        }
    }

    /**
     * Play a note on the synthesizer
     * 
     * @param note       MIDI note number (0-127)
     * @param velocity   Velocity value (0-127)
     * @param durationMs Duration in milliseconds
     */
    private void playNote(int note, int velocity, int durationMs) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                // Play on channel 16 (index 15)
                final MidiChannel channel = synthesizer.getChannels()[15];

                if (channel != null) {
                    if (useAheadScheduling) {
                        // Schedule note in advance to compensate for latency
                        new Thread(() -> {
                            try {
                                // Sleep for a very short time to allow thread to get high priority
                                Thread.sleep(1);
                                // Set this thread to max priority
                                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                                // Calculate when to play the note (slightly in the future)
                                long currentTime = System.currentTimeMillis();
                                long targetTime = currentTime + lookAheadMs;
                                long waitTime = targetTime - System.currentTimeMillis();

                                // Wait until precise time to play the note
                                if (waitTime > 0) {
                                    Thread.sleep(waitTime);
                                }

                                // Play the note exactly when needed
                                channel.noteOn(note, velocity);

                                // Sleep for note duration
                                Thread.sleep(durationMs);

                                // Turn off the note
                                channel.noteOff(note);
                            } catch (InterruptedException e) {
                                // Ignore interruptions
                            }
                        }).start();
                    } else {
                        // Original direct playback code
                        channel.noteOn(note, velocity);

                        // Schedule note off with our gate time
                        java.util.Timer timer = new java.util.Timer(true);
                        timer.schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                channel.noteOff(note);
                                timer.cancel();
                            }
                        }, durationMs);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error playing note: " + e.getMessage());
            }
        }
    }
}
