package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;

/**
 * Panel containing transport controls (play, stop, record, etc.)
 */
public class TransportPanel extends JPanel {
    private final CommandBus commandBus = CommandBus.getInstance();

    // Transport controls
    private JButton playButton;
    private JButton stopButton;
    private JButton recordButton;
    private JButton rewindButton;
    private JButton forwardButton;
    private JButton pauseButton;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    // Add a flag to track initial application load state
    private boolean initialLoadCompleted = false;
    private boolean ignoreNextSessionUpdate = true; // Flag to ignore the first update

    // Add a timer to delay the auto-recording feature
    private boolean autoRecordingEnabled = false;
    private javax.swing.Timer autoRecordingEnableTimer;

    // Tracking variables for session navigation
    private String lastCommand = "";
    private long lastSessionNavTime = 0;

    public TransportPanel() {
        super(new BorderLayout());
        setPreferredSize(new Dimension(getPreferredSize().width, 75));

        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Start with recording disabled
        isRecording = false;
        isPlaying = false;

        // Set up auto-recording timer
        autoRecordingEnableTimer = new javax.swing.Timer(3000, e -> {
            autoRecordingEnabled = true;
            autoRecordingEnableTimer.stop();
        });
        autoRecordingEnableTimer.setRepeats(false);
        autoRecordingEnableTimer.start();

        // Get transport buttons panel and add it to center
        JPanel transportButtonsPanel = setupTransportButtons();
        add(transportButtonsPanel, BorderLayout.CENTER);

        // JPanel indicatorPanel = new TransportIndicatorPanel();
        // add(indicatorPanel, BorderLayout.SOUTH);

        setupCommandBusListener();

        // Set initial button states
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    /**
     * Creates and configures transport control buttons
     * 
     * @return JPanel containing all transport buttons
     */
    private JPanel setupTransportButtons() {
        // Create panel to hold transport buttons with flow layout
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        rewindButton = createToolbarButton(Commands.TRANSPORT_REWIND, "⏮", "Previous Session");

        // Create pause button with special handling
        pauseButton = new JButton("⏸");
        pauseButton.setToolTipText("Pause (All Notes Off)");
        pauseButton.setEnabled(false); // Initially disabled
        pauseButton.setActionCommand(Commands.TRANSPORT_PAUSE);

        // Style the pause button
        int size = 32;
        pauseButton.setPreferredSize(new Dimension(size, size));
        pauseButton.setMinimumSize(new Dimension(size, size));
        pauseButton.setMaximumSize(new Dimension(size, size));
        pauseButton.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        if (!pauseButton.getFont().canDisplay('⏸')) {
            pauseButton.setFont(new Font("Dialog", Font.PLAIN, 18));
        }
        pauseButton.setMargin(new Insets(0, 0, 0, 0));
        pauseButton.setFocusPainted(false);
        pauseButton.setVerticalAlignment(SwingConstants.CENTER);
        pauseButton.setBorderPainted(false);
        pauseButton.setContentAreaFilled(true);
        pauseButton.setOpaque(true);

        // Special action for pause button - send ALL_NOTES_OFF
        pauseButton.addActionListener(e -> {
            commandBus.publish(Commands.ALL_NOTES_OFF, this);
        });

        // Rest of existing code...

        recordButton = new JButton("⏺");
        // ... existing recordButton setup ...

        stopButton = createToolbarButton(Commands.TRANSPORT_STOP, "⏹", "Stop");
        playButton = createToolbarButton(Commands.TRANSPORT_START, "▶", "Play");
        forwardButton = createToolbarButton(Commands.TRANSPORT_FORWARD, "⏭", "Next Session");

        // Add buttons to the panel instead of directly to TransportPanel
        buttonsPanel.add(rewindButton);
        buttonsPanel.add(pauseButton);
        buttonsPanel.add(stopButton);
        buttonsPanel.add(recordButton);
        buttonsPanel.add(playButton);
        buttonsPanel.add(forwardButton);

        updatePlayButtonAppearance();
        updateRecordButtonAppearance();

        return buttonsPanel;
    }

    private JButton createToolbarButton(String command, String text, String tooltip) {
        JButton button = new JButton(text);

        button.setToolTipText(tooltip);
        button.setEnabled(true);
        button.setActionCommand(command);
        button.addActionListener(e -> {
            commandBus.publish(command, button);
            updatePlayButtonAppearance();
            updateRecordButtonAppearance();
        });

        // Styling
        int size = 32;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));

        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));

        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }

        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setVerticalAlignment(SwingConstants.CENTER);

        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(button.getBackground().brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(UIManager.getColor("Button.background"));
            }
        });

        return button;
    }

    private void toggleRecordingState() {
        boolean wasRecording = isRecording;
        isRecording = !isRecording;
        updateRecordButtonAppearance();

        if (wasRecording && !isRecording) {
            // We're turning recording off - save the session
            try {
                // Get the current session from SessionManager
                Session currentSession = SessionManager.getInstance().getActiveSession();
                if (currentSession != null) {
                    // Save the session (which includes players and rules)
                    SessionManager.getInstance().saveSession(currentSession);

                    // Show save confirmation
                    // commandBus.publish(Commands.SHOW_STATUS, this, "Session saved");
                }
            } catch (Exception ex) {
                // Log and show any errors during save
                System.err.println("Error saving session: " + ex.getMessage());
                ex.printStackTrace();
                // commandBus.publish(Commands.SHOW_ERROR, this, "Error saving session: " +
                // ex.getMessage());
            }
        }

        // Publish the appropriate command
        if (isRecording) {
            commandBus.publish(Commands.TRANSPORT_RECORD_START, this);
        } else {
            commandBus.publish(Commands.TRANSPORT_RECORD_STOP, this);
        }
    }

    private void updateRecordButtonAppearance() {
        if (isRecording) {
            recordButton.setBackground(Color.RED);
            recordButton.setForeground(Color.WHITE);
        } else {
            recordButton.setBackground(UIManager.getColor("Button.background"));
            recordButton.setForeground(UIManager.getColor("Button.foreground"));
        }
    }

    private void updatePlayButtonAppearance() {
        if (isPlaying) {
            playButton.setBackground(Color.GREEN);
            playButton.setForeground(Color.WHITE);
        } else {
            playButton.setBackground(UIManager.getColor("Button.background"));
            playButton.setForeground(UIManager.getColor("Button.foreground"));
        }
        playButton.invalidate();
        playButton.repaint();
    }

    /**
     * Update the enabled state of transport buttons based on session state
     */
    public void updateTransportState(Session session) {
        boolean hasActiveSession = Objects.nonNull(session);

        if (session == null) {
            rewindButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            recordButton.setEnabled(false);
            playButton.setEnabled(false);
            forwardButton.setEnabled(false);
            return;
        }

        // Update states for all buttons
        rewindButton.setEnabled(hasActiveSession && SessionManager.getInstance().canMoveBack());
        forwardButton.setEnabled(SessionManager.getInstance().canMoveForward());

        // Enable pause button when the session is running
        pauseButton.setEnabled(hasActiveSession && session.isRunning());

        playButton.setEnabled(hasActiveSession && !session.isRunning());
        stopButton.setEnabled(hasActiveSession && session.isRunning());
        recordButton.setEnabled(hasActiveSession);
    }

    private void setupCommandBusListener() {
        commandBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                // Skip if this panel is the sender
                if (action.getSender() == TransportPanel.this) {
                    return;
                }

                if (Objects.nonNull(action.getCommand())) {
                    String cmd = action.getCommand();

                    // Track session navigation
                    if (isSessionNavigationCommand(cmd)) {
                        isRecording = false;
                        updateRecordButtonAppearance();
                        lastSessionNavTime = System.currentTimeMillis();
                        lastCommand = cmd;
                    }
                    // Special handling for value changes - wait 1 second after startup/navigation
                    else if (isValueChangeCommand(cmd)) {
                        // Only enable recording if:
                        // 1. We're past the startup period (1 second from app startup)
                        // 2. We're not immediately after session navigation
                        // (500ms from last navigation command)
                        long timeSinceStartup = System.currentTimeMillis() - getAppStartupTime();
                        long timeSinceNavigation = System.currentTimeMillis() - lastSessionNavTime;

                        if (timeSinceStartup > 1000 && timeSinceNavigation > 500) {
                            if (!isRecording) {
                                isRecording = true;
                                updateRecordButtonAppearance();
                                commandBus.publish(Commands.TRANSPORT_RECORD_START, TransportPanel.this);
                            }
                        }
                    }

                    // Switch for specific state handling commands
                    switch (cmd) {
                        case Commands.TRANSPORT_STATE_CHANGED:
                            if (action.getData() instanceof Boolean) {
                                Boolean isPlaying = (Boolean) action.getData();
                                SwingUtilities.invokeLater(() -> {
                                    playButton.setEnabled(!isPlaying);
                                    stopButton.setEnabled(isPlaying);
                                    pauseButton.setEnabled(isPlaying);
                                });
                            }
                            break;
                        case Commands.TRANSPORT_START:
                            isPlaying = true;
                            SwingUtilities.invokeLater(() -> pauseButton.setEnabled(true));
                            break;
                        case Commands.TRANSPORT_STOP:
                            SwingUtilities.invokeLater(() -> pauseButton.setEnabled(false));
                            isRecording = false;
                            isPlaying = false;
                            updateRecordButtonAppearance();
                            break;
                        case Commands.TRANSPORT_RECORD_START:
                            isRecording = true;
                            updateRecordButtonAppearance();
                            break;
                        case Commands.TRANSPORT_RECORD_STOP:
                            isRecording = false;
                            updateRecordButtonAppearance();
                            break;
                        case Commands.SESSION_CREATED:
                        case Commands.SESSION_SELECTED:
                        case Commands.SESSION_LOADED:
                            if (action.getData() instanceof Session) {
                                Session session = (Session) action.getData();
                                updateTransportState(session);

                                // Force disable recording
                                isRecording = false;
                                updateRecordButtonAppearance();

                                // Only enable forward button for non-new sessions
                                if (cmd.equals(Commands.SESSION_CREATED)) {
                                    forwardButton.setEnabled(false);
                                }
                            }
                            break;
                    }

                    // Keep track of last command processed
                    lastCommand = cmd;

                    updatePlayButtonAppearance();
                }
            }
        });
    }

    // Simple app startup time tracker
    private static final long APP_STARTUP_TIME = System.currentTimeMillis();

    private static long getAppStartupTime() {
        return APP_STARTUP_TIME;
    }

    /**
     * Check if a command is related to session navigation
     */
    private boolean isSessionNavigationCommand(String cmd) {
        return cmd.equals(Commands.TRANSPORT_REWIND) ||
                cmd.equals(Commands.TRANSPORT_FORWARD) ||
                cmd.equals(Commands.SESSION_CREATED) ||
                cmd.equals(Commands.SESSION_SELECTED) ||
                cmd.equals(Commands.SESSION_LOADED) ||
                cmd.equals(Commands.SESSION_REQUEST);
    }

    /**
     * Check if a command is related to value changes that should trigger recording
     */
    private boolean isValueChangeCommand(String cmd) {
        // Return true for any command that should trigger recording

        // Player modification commands
        if (cmd.equals(Commands.PLAYER_ADDED) ||
                cmd.equals(Commands.PLAYER_UPDATED) ||
                cmd.equals(Commands.PLAYER_DELETED)) {
            return true;
        }

        // Rule modification commands
        if (cmd.equals(Commands.RULE_ADDED) ||
                cmd.equals(Commands.RULE_UPDATED) ||
                cmd.equals(Commands.RULE_DELETED) ||
                cmd.equals(Commands.RULE_ADDED_TO_PLAYER) ||
                cmd.equals(Commands.RULE_REMOVED_FROM_PLAYER)) {
            return true;
        }

        // Value change commands
        if (cmd.equals(Commands.NEW_VALUE_LEVEL) ||
                cmd.equals(Commands.NEW_VALUE_NOTE) ||
                cmd.equals(Commands.NEW_VALUE_SWING) ||
                cmd.equals(Commands.NEW_VALUE_PROBABILITY) ||
                cmd.equals(Commands.NEW_VALUE_VELOCITY_MIN) ||
                cmd.equals(Commands.NEW_VALUE_VELOCITY_MAX) ||
                cmd.equals(Commands.NEW_VALUE_RANDOM) ||
                cmd.equals(Commands.NEW_VALUE_PAN) ||
                cmd.equals(Commands.NEW_VALUE_SPARSE)) {
            return true;
        }

        // Only enable recording for actual session updates, not initial loading
        if (cmd.equals(Commands.SESSION_UPDATED)) {
            return true;
        }

        // Other parameter changes
        if (cmd.equals(Commands.PRESET_CHANGED) ||
                cmd.equals(Commands.UPDATE_TEMPO) ||
                cmd.equals(Commands.UPDATE_TIME_SIGNATURE) ||
                cmd.equals(Commands.TIMING_PARAMETERS_CHANGED) ||
                cmd.equals(Commands.TRANSPOSE_UP) ||
                cmd.equals(Commands.TRANSPOSE_DOWN)) {
            return true;
        }

        return false;
    }
}