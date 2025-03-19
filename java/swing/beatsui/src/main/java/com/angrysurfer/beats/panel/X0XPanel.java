package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

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
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;

class X0XPanel extends StatusProviderPanel implements IBusListener {
    private final List<TriggerButton> triggerButtons = new ArrayList<>();

    private boolean isPlaying = false;
    private int currentStep = 0;

    // Add a class member for timer-based stepping
    private javax.swing.Timer stepTimer;

    public X0XPanel() {
        super(new BorderLayout());
        setStatusConsumer(statusConsumer);
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
                System.out.println("Timer firing: current=" + currentStep + ", next=" + nextStep);
                updateStep(currentStep, nextStep);
                currentStep = nextStep;
            }
        });
        stepTimer.setRepeats(true);
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;
        
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
                
                // Calculate ms per step: 60000ms/min ÷ BPM = ms/beat, then divide by 4 steps/beat
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
                    System.out.println("X0X: Transport Play - reset to step 0, step timer started");
                });
            }
            
            case Commands.TRANSPORT_STOP -> {
                isPlaying = false;
                // Stop the timer
                stepTimer.stop();
                resetSequence();
                System.out.println("X0X: Transport Stop - sequence reset, step timer stopped");
            }
            
            // COMPLETELY DISABLE the tick-based approach
            // case Commands.BASIC_TIMING_TICK -> {
            //    // Removed to avoid conflicts
            // }
            
            case Commands.BASIC_TIMING_BEAT -> {
                // Just use for tempo synchronization
                if (isPlaying && action.getData() instanceof Number beatNum) {
                    System.out.println("X0X: Beat " + beatNum.intValue() + " (purely informational)");
                }
            }
            
            case Commands.SESSION_UPDATED -> {
                // Update tempo when session changes
                if (action.getData() instanceof Session session) {
                    int bpm = Math.round(session.getTempoInBPM());
                    int msPerBeat = 60000 / bpm;
                    int msPerStep = msPerBeat / 4;
                    stepTimer.setDelay(msPerStep);
                    System.out.println("X0X: Session updated - tempo=" + bpm + ", msPerStep=" + msPerStep);
                }
            }

            // Enable tempo change handling
            // case Commands.TEMPO_CHANGED -> {
            //     if (action.getData() instanceof Number tempoBPM) {
            //         int bpm = tempoBPM.intValue();
            //         int msPerBeat = 60000 / bpm;
            //         int msPerStep = msPerBeat / 4;
            //         stepTimer.setDelay(msPerStep);
            //         System.out.println("X0X: Tempo changed to " + bpm + " BPM, step timer=" + msPerStep + "ms");
            //     }
            // }
        }
    }

    private void updateStep(int oldStep, int newStep) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Clear previous step
                if (oldStep >= 0 && oldStep < triggerButtons.size()) {
                    triggerButtons.get(oldStep).setHighlighted(false);
                }
                
                // Highlight current step
                if (newStep >= 0 && newStep < triggerButtons.size()) {
                    triggerButtons.get(newStep).setHighlighted(true);
                    if (getStatusConsumer() != null) {
                        getStatusConsumer().setStatus("Step: " + (newStep + 1) + " of 16");
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
            // Debug output to help diagnose issues
            System.out.println("Updating trigger buttons: step=" + currentStep + 
                               ", isPlaying=" + isPlaying + 
                               ", triggerButtons.size=" + triggerButtons.size());
            
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
            e.printStackTrace();  // Print stack trace for better debugging
        }
    }

    private void setup() {
        setLayout(new BorderLayout());
        add(createX0XPanel(), BorderLayout.CENTER);
        
        // Create more comprehensive debug panel
        JPanel debugPanel = new JPanel();
        debugPanel.setLayout(new BoxLayout(debugPanel, BoxLayout.Y_AXIS));
        
        // Panel for test buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        // Display current step
        JLabel stepLabel = new JLabel("Step: 0/16");
        buttonPanel.add(stepLabel);
        
        // Test button to cycle through all 16 steps exactly once
        JButton testCycleButton = new JButton("Test All 16 Steps");
        testCycleButton.addActionListener(e -> {
            // Use array to track progress across timer ticks
            final int[] counter = {0};
            
            javax.swing.Timer timer = new javax.swing.Timer(100, event -> {
                // Reset all buttons first
                for (TriggerButton button : triggerButtons) {
                    button.setHighlighted(false);
                }
                
                // Highlight the current test step
                if (counter[0] < triggerButtons.size()) {
                    triggerButtons.get(counter[0]).setHighlighted(true);
                    stepLabel.setText("Test Step: " + (counter[0] + 1) + "/16");
                    System.out.println("Test highlighting step " + counter[0]);
                    counter[0]++;
                } else {
                    // Stop when we've gone through all steps
                    ((javax.swing.Timer)event.getSource()).stop();
                    stepLabel.setText("Test complete");
                    
                    // Reset all highlights
                    for (TriggerButton button : triggerButtons) {
                        button.setHighlighted(false);
                    }
                }
            });
            timer.setInitialDelay(0);
            timer.start();
        });
        buttonPanel.add(testCycleButton);
        
        // Button to verify the trigger buttons collection
        JButton verifyButton = new JButton("Verify Buttons");
        verifyButton.addActionListener(e -> {
            System.out.println("==== TRIGGER BUTTON VERIFICATION ====");
            System.out.println("Number of trigger buttons: " + triggerButtons.size());
            if (triggerButtons.size() != 16) {
                System.out.println("ERROR: Expected 16 buttons but found " + triggerButtons.size());
            }
            
            for (int i = 0; i < triggerButtons.size(); i++) {
                // Don't try to access isHighlighted() as it doesn't exist
                System.out.println("Button " + i + ": " + 
                    triggerButtons.get(i).getName());
                    
                // Visual test - temporarily highlight each button in sequence
                final int buttonIndex = i;
                SwingUtilities.invokeLater(() -> {
                    // First clear all highlights
                    for (TriggerButton btn : triggerButtons) {
                        btn.setHighlighted(false);
                    }
                    // Highlight just this button
                    triggerButtons.get(buttonIndex).setHighlighted(true);
                });
                
                // Pause briefly to see the highlight
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Reset all highlights after verification
            SwingUtilities.invokeLater(() -> {
                for (TriggerButton btn : triggerButtons) {
                    btn.setHighlighted(false);
                }
            });
            
            stepLabel.setText("Verified: " + triggerButtons.size() + " buttons");
        });
        buttonPanel.add(verifyButton);
        
        debugPanel.add(buttonPanel);
        add(debugPanel, BorderLayout.SOUTH);
        
        // Verify setup
        System.out.println("X0XPanel setup complete: " + triggerButtons.size() + " trigger buttons created");
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
        for (int i = 0; i < 5; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setForeground(Color.GRAY);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            labelPanel.add(label);
            // Add label to the column
            column.add(labelPanel);

            Dial dial = new Dial();
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

        // Add the trigger button
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));
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

}
