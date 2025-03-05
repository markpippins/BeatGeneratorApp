package com.angrysurfer.beats.panel;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;

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

        // Log initial message
        log("Transport panel initialized");
    }

    private JButton createTransportButton(String symbol, String command, String tooltip) {
        JButton button = new JButton(symbol);
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));

        // Fallback font if needed
        if (!button.getFont().canDisplay(symbol.charAt(0))) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }

        // Set button properties
        button.setToolTipText(tooltip);
        button.setActionCommand(command);
        button.addActionListener(this::handleButtonAction);
        button.setFocusPainted(false);
        button.setMargin(new Insets(6, 12, 6, 12));

        // Set size constraints
        int size = 44;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));

        return button;
    }

    private void handleButtonAction(ActionEvent e) {
        String command = e.getActionCommand();
        commandBus.publish(command, this);
        log("Transport command: " + command);
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
                    });
                    log("Transport state changed: " + (isPlaying ? "Playing" : "Stopped"));
                }
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
}