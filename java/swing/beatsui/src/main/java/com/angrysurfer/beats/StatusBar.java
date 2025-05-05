package com.angrysurfer.beats;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;

import com.angrysurfer.beats.panel.player.SoundParametersPanel;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.VuMeter;
import com.angrysurfer.beats.widget.LEDIndicator;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.api.TimingBus;
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
    private static final int SMALL_FIELD_WIDTH = 50;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

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
        setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        
        // Create main panel with horizontal layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        
        // Create and add all sections
        mainPanel.add(new SoundParametersPanel());
        mainPanel.add(Box.createHorizontalStrut(SECTION_SPACING));
        mainPanel.add(createMonitoringSection());
        mainPanel.add(Box.createHorizontalStrut(SECTION_SPACING));
        mainPanel.add(createMessageSection());

        // Add main panel to status bar
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createMonitoringSection() {
        JPanel panel = UIHelper.createSectionPanel("System");
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
        JPanel panel = UIHelper.createSectionPanel("Status");
        panel.setLayout(new BorderLayout(5, 0));
        
        // Create a wrapper panel for the label to vertically center it
        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setPreferredSize(new Dimension(labelPanel.getPreferredSize().width, UIHelper.CONTROL_HEIGHT));
        messageLabel = new JLabel("Message:");
        labelPanel.add(messageLabel, BorderLayout.CENTER);
        
        // Add components with proper vertical alignment
        panel.add(labelPanel, BorderLayout.WEST);
        
        messageField = createStatusField(0); // Will expand to fill space
        panel.add(messageField, BorderLayout.CENTER);
        
        // Add current time display
        JTextField timeField = createStatusField((int) (1.2 * SMALL_FIELD_WIDTH));
        timeField.setText(TIME_FORMAT.format(new Date()));
        timeField.setBackground(Color.BLACK);
        timeField.setForeground(Color.WHITE);
        timeField.setEditable(false);
        timeField.setAlignmentY(JTextField.CENTER);
        timeField.setFocusable(false);

        // Update time every second
        Timer timer = new Timer(1000, e -> {
            timeField.setText(TIME_FORMAT.format(new Date()));
        });
        timer.start();
        
        panel.add(timeField, BorderLayout.EAST);
        
        return panel;
    }
     
    private JTextField createStatusField(int width) {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setBackground(UIHelper.FIELD_BACKGROUND);
        field.setForeground(UIHelper.FIELD_FOREGROUND);
        
        // Always set the height to UIHelper.CONTROL_HEIGHT
        if (width > 0) {
            Dimension size = new Dimension(width, UIHelper.CONTROL_HEIGHT);
            field.setPreferredSize(size);
            field.setMinimumSize(size);
        } else {
            // For fields with dynamic width, still set the height
            field.setPreferredSize(new Dimension(field.getPreferredSize().width, UIHelper.CONTROL_HEIGHT));
            field.setMinimumSize(new Dimension(0, UIHelper.CONTROL_HEIGHT));
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
    
    private void updateTimeDisplay() {
        String formattedTime = String.format("%02d:%02d:%02d:%02d", partCount, barCount, beatCount, tickCount);
        positionField.setText(formattedTime);
    }

    public void setMessage(String text) {
        messageField.setText(text);
    }

}
