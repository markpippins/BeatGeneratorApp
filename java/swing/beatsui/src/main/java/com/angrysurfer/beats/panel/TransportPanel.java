package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.widget.LedIndicator;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.service.SequencerManager;

class TransportPanel extends JPanel implements CommandListener {

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


    private final LedIndicator tickLed;
    private final LedIndicator beatLed;
    private final LedIndicator barLed;

    public TransportPanel() {
        super(new BorderLayout(PADDING, PADDING));
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

        // Log initial message
        log("Transport panel initialized");
    }

    private void handleButtonAction(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case Commands.TRANSPORT_PLAY -> {
                // Delegate to SequencerManager
                SequencerManager.getInstance().start();
                // UI updates will happen through command listeners
            }
            case Commands.TRANSPORT_STOP -> {
                // Delegate to SequencerManager
                SequencerManager.getInstance().stop();
                // UI updates will happen through command listeners
            }
            // Other cases...
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
        if (action.getCommand() == null) return;
        
        switch (action.getCommand()) {
            case Commands.BASIC_TIMING_TICK -> {
                // Flash tick LED
                flashTickLed(tickLed);
            }
            case Commands.BASIC_TIMING_BEAT -> {
                // Flash beat LED
                flashBeatLed(beatLed);
            }
            case Commands.TRANSPORT_STATE_CHANGED -> {
                boolean isPlaying = (boolean)action.getData();
                playButton.setEnabled(!isPlaying);
                stopButton.setEnabled(isPlaying);
                // Other transport UI updates
            }
            // Other cases...
        }
    }

    @Override
    protected void finalize() throws Throwable {
        SequencerManager.getInstance().cleanup();
        super.finalize();
    }

    private JButton createTransportButton(String symbol, String command, String tooltip) {
        JButton button = new JButton(symbol);
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
        button.setFocusPainted(false);
        button.setVerticalAlignment(SwingConstants.CENTER);
        
        return button;
    }
}