package com.angrysurfer.beats;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.angrysurfer.beats.widget.UIHelper;
import com.angrysurfer.beats.widget.VuMeter;
import com.angrysurfer.beats.widget.LEDIndicator;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.sequencer.TimingUpdate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusBar extends JPanel implements IBusListener {

    // Constants for UI sizing and formatting
    private static final int SECTION_SPACING = 4;
    private static final int FIELD_HEIGHT = 20;
    private static final int SMALL_FIELD_WIDTH = 50;
    private static final int MEDIUM_FIELD_WIDTH = 100;
    private static final Color SECTION_BORDER_COLOR = new Color(180, 180, 180);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    // Session information
    private JLabel sessionIdLabel;
    private JTextField sessionIdField;
    private JLabel bpmLabel;
    private JTextField bpmField;
    private JLabel playerCountLabel;
    private JTextField playerCountField;

    // Transport section
    private LEDIndicator playingLed;
    private LEDIndicator recordingLed;
    // private JTextField transportStateField;
    private JLabel timeSignatureLabel;
    private JTextField timeSignatureField;

    // Player information
    private JLabel playerLabel;
    private JTextField playerNameField;
    private JLabel channelLabel;
    private JTextField channelField;
    private JLabel instrumentLabel;
    private JTextField instrumentField;
    
    // Performance monitors
    private JLabel cpuLabel;
    private JProgressBar cpuUsageBar;
    private JLabel memoryLabel;
    private JProgressBar memoryUsageBar;
    
    // Timing display
    private JLabel positionLabel;
    private JTextField positionField;
    
    // Level meters and status indicators
    private VuMeter leftMeter;
    private VuMeter rightMeter;
    
    // System message area
    private JLabel messageLabel;
    private JTextField messageField;

    // Data fields
    private CommandBus commandBus = CommandBus.getInstance();
    private int tickCount = 0;
    private int beatCount = 0;
    private int barCount = 0;
    private int partCount = 0;
    private float tempo = 120.0f;
    private boolean isPlaying = false;
    private boolean isRecording = false;
    private String timeSignature = "4/4";
    private Player currentPlayer;
    private Timer performanceMonitorTimer;
    private Random random = new Random(); // For demo level meter movement

    public StatusBar() {
        super();
        setup();
        registerForEvents();
        startPerformanceMonitoring();
        requestInitialData();
    }

    private void registerForEvents() {
        TimingBus.getInstance().register(this);
        commandBus.register(this);
    }

    private void requestInitialData() {
        SwingUtilities.invokeLater(() -> {
            commandBus.publish(Commands.SESSION_REQUEST, this);
            commandBus.publish(Commands.TRANSPORT_STATE_REQUEST, this);
            commandBus.publish(Commands.ACTIVE_PLAYER_REQUEST, this);
        });
    }

    private void setup() {
        // Global panel setup
        setLayout(new BorderLayout());
        // setBorder(new CompoundBorder(
        //     new MatteBorder(1, 0, 0, 0, SECTION_BORDER_COLOR),
        //     new EmptyBorder(3, 6, 3, 6)
        // ));
        
        // Create main panel with horizontal layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        
        // Create and add all sections
        mainPanel.add(createSessionSection());
        mainPanel.add(Box.createHorizontalStrut(SECTION_SPACING));
        mainPanel.add(createTransportSection());
        mainPanel.add(Box.createHorizontalStrut(SECTION_SPACING));
        mainPanel.add(createPlayerSection());
        mainPanel.add(Box.createHorizontalStrut(SECTION_SPACING));
        mainPanel.add(createMonitoringSection());
        mainPanel.add(Box.createHorizontalStrut(SECTION_SPACING));
        mainPanel.add(createMessageSection());
        
        // Add main panel to status bar
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createSessionSection() {
        JPanel panel = createSectionPanel("Session");
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Session ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        sessionIdLabel = new JLabel("ID:");
        panel.add(sessionIdLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 2, 0, 8);
        sessionIdField = createStatusField(SMALL_FIELD_WIDTH);
        panel.add(sessionIdField, gbc);
        
        // BPM
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 2);
        bpmLabel = new JLabel("BPM:");
        panel.add(bpmLabel, gbc);
        
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 2, 0, 8);
        bpmField = createStatusField(SMALL_FIELD_WIDTH);
        bpmField.setText("120");
        panel.add(bpmField, gbc);
        
        // Player count
        gbc.gridx = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 2);
        playerCountLabel = new JLabel("Players:");
        panel.add(playerCountLabel, gbc);
        
        gbc.gridx = 5;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 2, 0, 0);
        playerCountField = createStatusField(SMALL_FIELD_WIDTH);
        panel.add(playerCountField, gbc);
        
        return panel;
    }
    
    private JPanel createTransportSection() {
        JPanel panel = createSectionPanel("Transport");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // Transport state indicator
        JPanel ledPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        playingLed = new LEDIndicator(Color.GREEN, "PLAY");
        playingLed.setOffColor(Color.RED);
        recordingLed = new LEDIndicator(Color.RED, "REC");
        recordingLed.setOffColor(Color.RED);
        ledPanel.add(playingLed);
        ledPanel.add(recordingLed);
        panel.add(ledPanel);
        
        // Transport state text
        // transportStateField = createStatusField(SMALL_FIELD_WIDTH);
        // transportStateField.setText("Stopped");
        // panel.add(transportStateField);
        
        // Time signature
        timeSignatureLabel = new JLabel("Time:");
        panel.add(timeSignatureLabel);
        
        timeSignatureField = createStatusField(SMALL_FIELD_WIDTH);
        timeSignatureField.setText("4/4");
        panel.add(timeSignatureField);
        
        // Position display
        positionLabel = new JLabel("Pos:");
        panel.add(positionLabel);
        
        positionField = createStatusField(MEDIUM_FIELD_WIDTH);
        positionField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        updateTimeDisplay();
        panel.add(positionField);
        
        return panel;
    }
    
    private JPanel createPlayerSection() {
        JPanel panel = createSectionPanel("Active Player");
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // Player name
        playerLabel = new JLabel("ID:");
        panel.add(playerLabel);
        
        playerNameField = createStatusField(MEDIUM_FIELD_WIDTH);
        panel.add(playerNameField);
        
        // Channel
        channelLabel = new JLabel("Ch:");
        panel.add(channelLabel);
        
        channelField = createStatusField(SMALL_FIELD_WIDTH);
        panel.add(channelField);
        
        // Instrument
        instrumentLabel = new JLabel("Inst:");
        panel.add(instrumentLabel);
        
        instrumentField = createStatusField(MEDIUM_FIELD_WIDTH);
        panel.add(instrumentField);
        
        return panel;
    }
    
    private JPanel createMonitoringSection() {
        JPanel panel = createSectionPanel("System");
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // CPU usage
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        cpuLabel = new JLabel("CPU:");
        panel.add(cpuLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 2, 2, 0);
        cpuUsageBar = createProgressBar();
        panel.add(cpuUsageBar, gbc);
        
        // Memory usage
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 2);
        memoryLabel = new JLabel("Mem:");
        panel.add(memoryLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 2, 0, 0);
        memoryUsageBar = createProgressBar();
        panel.add(memoryUsageBar, gbc);
        
        // Audio levels
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 8, 0, 0);
        
        JPanel meterPanel = new JPanel();
        meterPanel.setLayout(new BoxLayout(meterPanel, BoxLayout.Y_AXIS));
        leftMeter = new VuMeter(VuMeter.Orientation.HORIZONTAL);
        rightMeter = new VuMeter(VuMeter.Orientation.HORIZONTAL);
        meterPanel.add(leftMeter);
        meterPanel.add(Box.createVerticalStrut(4));
       
        meterPanel.add(rightMeter);
        
        panel.add(meterPanel, gbc);
        
        return panel;
    }
    
    private JPanel createMessageSection() {
        JPanel panel = createSectionPanel("Status");
        panel.setLayout(new BorderLayout(1, 0));
        
        messageLabel = new JLabel("Message:");
        panel.add(messageLabel, BorderLayout.WEST);
        
        messageField = createStatusField(0); // Will expand to fill space
        panel.add(messageField, BorderLayout.CENTER);
        
        // Add current time display
        JTextField timeField = createStatusField(SMALL_FIELD_WIDTH);
        timeField.setText(TIME_FORMAT.format(new Date()));
        
        // Update time every second
        Timer timer = new Timer(1000, e -> {
            timeField.setText(TIME_FORMAT.format(new Date()));
        });
        timer.start();
        
        panel.add(timeField, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SECTION_BORDER_COLOR, 1),
            title
        ));
        return panel;
    }
    
    private JTextField createStatusField(int width) {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setBackground(UIHelper.FIELD_BACKGROUND);
        field.setForeground(UIHelper.FIELD_FOREGROUND);
        
        if (width > 0) {
            Dimension size = new Dimension(width, UIUtils.CONTROL_HEIGHT);
            field.setPreferredSize(size);
            field.setMinimumSize(size);
        }
        
        return field;
    }
    
    private JProgressBar createProgressBar() {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setPreferredSize(new Dimension(100, 15));
        bar.setBorderPainted(true);
        return bar;
    }
    
    private void startPerformanceMonitoring() {
        performanceMonitorTimer = new Timer(1000, e -> {
            updatePerformanceMetrics();
            updateLevelMeters();
        });
        performanceMonitorTimer.start();
    }
    
    private void updatePerformanceMetrics() {
        // Get CPU usage (example implementation)
        long cpuUsage = getSystemCpuUsage();
        cpuUsageBar.setValue((int)cpuUsage);
        cpuUsageBar.setString(cpuUsage + "%");
        
        // Get memory usage
        long memoryUsage = getSystemMemoryUsage();
        memoryUsageBar.setValue((int)memoryUsage);
        memoryUsageBar.setString(memoryUsage + "%");
    }
    
    private void updateLevelMeters() {
        // For demo/testing, use random values - replace with actual audio levels
        if (isPlaying) {
            int leftLevel = Math.max(0, Math.min(100, random.nextInt(60) + (isRecording ? 40 : 20)));
            int rightLevel = Math.max(0, Math.min(100, random.nextInt(60) + (isRecording ? 40 : 20)));
            
            leftMeter.setLevel(leftLevel);
            rightMeter.setLevel(rightLevel);
        } else {
            leftMeter.setLevel(0);
            rightMeter.setLevel(0);
        }
    }
    
    private long getSystemCpuUsage() {
        // Mock implementation - replace with actual JMX or other system monitoring
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return Math.round(osBean.getCpuLoad() * 100.0);
        } catch (Exception e) {
            // Fallback to random values if the above doesn't work
            return isPlaying ? Math.min(90, 30 + random.nextInt(20)) : 10 + random.nextInt(10);
        }
    }
    
    private long getSystemMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return (usedMemory * 100) / maxMemory;
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null)
            return;

        try {
            switch (action.getCommand()) {
                case Commands.STATUS_UPDATE -> {
                    if (action.getData() instanceof StatusUpdate update) {
                        handleStatusUpdate(update);
                    }
                }
                case Commands.SESSION_SELECTED, Commands.SESSION_UPDATED, Commands.SESSION_LOADED -> {
                    if (action.getData() instanceof Session session) {
                        updateSessionInfo(session);
                    }
                }
                case Commands.PLAYER_SELECTED -> {
                    if (action.getData() instanceof Player player) {
                        updatePlayerInfo(player);
                    }
                }
                case Commands.PLAYER_UPDATED -> {
                    if (action.getData() instanceof Player player && 
                        currentPlayer != null && 
                        player.getId().equals(currentPlayer.getId())) {
                        updatePlayerInfo(player);
                    }
                }
                case Commands.PLAYER_UNSELECTED -> {
                    clearPlayerInfo();
                }
                case Commands.TIMING_UPDATE -> {
                    if (action.getData() instanceof TimingUpdate update) {
                        handleTimingUpdate(update);
                    }
                }
                case Commands.TEMPO_CHANGE -> {
                    if (action.getData() instanceof Number newTempo) {
                        tempo = newTempo.floatValue();
                        bpmField.setText(String.format("%.1f", tempo));
                    }
                }
                case Commands.TIME_SIGNATURE_CHANGE -> {
                    if (action.getData() instanceof String newTimeSignature) {
                        timeSignature = newTimeSignature;
                        timeSignatureField.setText(timeSignature);
                    }
                }
                case Commands.TIMING_RESET -> {
                    resetTimingCounters();
                }
                case Commands.TRANSPORT_START -> {
                    isPlaying = true;
                    isRecording = false;
                    updateTransportState("Playing");
                }
                case Commands.TRANSPORT_STOP -> {
                    isPlaying = false;
                    isRecording = false;
                    updateTransportState("Stopped");
                }
                case Commands.TRANSPORT_RECORD -> {
                    isPlaying = true;
                    isRecording = true;
                    updateTransportState("Recording");
                }
                case Commands.TRANSPORT_PAUSE -> {
                    isPlaying = false;
                    updateTransportState("Paused");
                }
                case Commands.INSTRUMENT_UPDATED -> {
                    if (currentPlayer != null && 
                        action.getData() instanceof InstrumentWrapper instrument &&
                        currentPlayer.getInstrumentId() != null &&
                        currentPlayer.getInstrumentId().equals(instrument.getId())) {
                        updateInstrumentInfo(instrument);
                    }
                }
                default -> {
                    // No action needed for other commands
                }
            }
        } catch (Exception e) {
            System.err.println("Error in StatusBar.onAction: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleStatusUpdate(StatusUpdate update) {
        // Update only the fields that are provided (non-null)
        if (update.message() != null) {
            messageField.setText(update.message());
        }
        
        // Other status update fields can be mapped as needed
    }
    
    private void handleTimingUpdate(TimingUpdate update) {
        // Update timing values from the TimingUpdate record
        if (update.tick() != null) {
            tickCount = update.tick().intValue();
        }
        if (update.beat() != null) {
            beatCount = update.beat().intValue();
        }
        if (update.bar() != null) {
            barCount = update.bar().intValue();
        }
        if (update.part() != null) {
            partCount = update.part().intValue();
        }
        
        // Update the time display with all values
        updateTimeDisplay();
    }

    private void updateSessionInfo(Session session) {
        if (session != null) {
            sessionIdField.setText(String.valueOf(session.getId()));
            playerCountField.setText(String.valueOf(session.getPlayers().size()));
            
            // Update tempo if available
            if (session.getTempoInBPM() != null) {
                tempo = session.getTempoInBPM();
                bpmField.setText(String.format("%.1f", tempo));
            }
        } else {
            clearSessionInfo();
        }
    }

    private void updatePlayerInfo(Player player) {
        if (player != null) {
            currentPlayer = player;
            playerNameField.setText(player.getName());
            channelField.setText(player.getChannel() != null ? String.valueOf(player.getChannel()) : "");
            
            if (player.getInstrument() != null) {
                updateInstrumentInfo(player.getInstrument());
            } else {
                instrumentField.setText("None");
            }
        } else {
            clearPlayerInfo();
        }
    }
    
    private void updateInstrumentInfo(InstrumentWrapper instrument) {
        if (instrument != null) {
            String instName = instrument.getName();
            
            // If available, add preset information
            if (instrument.getCurrentPreset() != null) {
                instName += " (" + instrument.getCurrentPreset() + ")";
            }
            
            instrumentField.setText(instName);
        } else {
            instrumentField.setText("None");
        }
    }
    
    private void updateTransportState(String state) {
        // transportStateField.setText(state);
        
        // Update LED indicators
        playingLed.setOn(isPlaying);
        recordingLed.setOn(isRecording);
        
        // Additional visual feedback
        // transportStateField.setForeground(isRecording ? Color.RED : (isPlaying ? new Color(0, 150, 0) : Color.BLACK));
    }

    private void clearSessionInfo() {
        sessionIdField.setText("");
        playerCountField.setText("");
        bpmField.setText("120");
    }

    private void clearPlayerInfo() {
        currentPlayer = null;
        playerNameField.setText("");
        channelField.setText("");
        instrumentField.setText("");
    }

    private void resetTimingCounters() {
        tickCount = 0;
        beatCount = 0;
        barCount = 0;
        partCount = 0;
        updateTimeDisplay();
    }

    private void updateTimeDisplay() {
        String formattedTime = String.format("%02d:%02d:%02d:%02d", partCount, barCount, beatCount, tickCount);
        positionField.setText(formattedTime);
    }

    public void setMessage(String text) {
        messageField.setText(text);
    }
    
    /**
     * Must be called when application is closing to prevent memory leaks
     */
    public void cleanup() {
        if (performanceMonitorTimer != null) {
            performanceMonitorTimer.stop();
        }
    }
}
