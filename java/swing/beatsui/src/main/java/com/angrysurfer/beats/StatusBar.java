package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.util.Scale;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusBar extends JPanel implements IBusListener, StatusConsumer {

    private JLabel sessionIdLabel;
    private JLabel playerCountLabel;
    private JLabel playerIdLabel;
    private JLabel ruleCountLabel;
    private JLabel siteLabel;
    private JLabel statusLabel;
    private JLabel messageLabel;
    private JLabel timeLabel;

    private JTextField sessionIdField;
    private JTextField playerCountField;
    private JTextField playerIdField;
    private JTextField ruleCountField;
    private JTextField siteField;
    private JTextField statusField;
    private JTextField messageField;
    private JTextField timeField;

    private CommandBus commandBus = CommandBus.getInstance();

    // private final LedIndicator tickLed;
    // private final LedIndicator beatLed;
    // private final LedIndicator barLed;
    // private boolean litTick = false;
    // private boolean litBeat = false;
    // private boolean litBar = false;

    // Add timing counters
    private int tickCount = 0;
    private int beatCount = 0;
    private int barCount = 0;

    // Add this field to your class if it doesn't exist
    private Map<String, JComponent> rightFields = new HashMap<>();

    public StatusBar() {
        super();

        // Create LEDs
        // tickLed = new LedIndicator(new Color(255, 50, 50)); // Red
        // beatLed = new LedIndicator(new Color(50, 255, 50)); // Green
        // barLed = new LedIndicator(new Color(50, 50, 255)); // Blue

        // Setup panels
        setup();
        setupLedIndicators();

        // Register for timing events
        TimingBus.getInstance().register(this);
        
        // Reset timing display
        resetTimingCounters();

        // Request initial session state through CommandBus
        SwingUtilities.invokeLater(() -> {
            CommandBus.getInstance().publish(Commands.SESSION_REQUEST, this);
        });
    }

    private void setup() {
        // Use a single horizontal box layout
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        
        // 1. LED INDICATORS - Now first on the left
        // JPanel ledPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        // ledPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        // ledPanel.setAlignmentY(CENTER_ALIGNMENT);
        
        // Use more descriptive labels for LEDs
        // ledPanel.add(new JLabel("Tick"));
        // ledPanel.add(tickLed);
        // ledPanel.add(new JLabel("Beat"));
        // ledPanel.add(beatLed);
        // ledPanel.add(new JLabel("Bar"));
        // ledPanel.add(barLed);
        
        // add(ledPanel);
        
        // Add small spacer
        // add(Box.createRigidArea(new Dimension(5, 0)));
        
        // 2. SESSION INFO GROUP
        JPanel sessionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        // sessionPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, Color.LIGHT_GRAY));
        sessionPanel.setAlignmentY(CENTER_ALIGNMENT);
        
        sessionIdLabel = new JLabel("Session:");
        sessionPanel.add(sessionIdLabel);
        sessionIdField = createTextField(2);
        sessionPanel.add(sessionIdField);
        
        playerCountLabel = new JLabel("Players:");
        sessionPanel.add(playerCountLabel);
        playerCountField = createTextField(2);
        sessionPanel.add(playerCountField);
        
        add(sessionPanel);
        
        // Add small spacer
        // add(Box.createRigidArea(new Dimension(5, 0)));
        
        // 3. PLAYER INFO GROUP
        JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        // playerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
        playerPanel.setAlignmentY(CENTER_ALIGNMENT);
        
        playerIdLabel = new JLabel("Player:");
        playerPanel.add(playerIdLabel);
        playerIdField = createTextField(2);
        playerPanel.add(playerIdField);
        
        ruleCountLabel = new JLabel("Rules:");
        playerPanel.add(ruleCountLabel);
        ruleCountField = createTextField(2);
        playerPanel.add(ruleCountField);
        
        add(playerPanel);
        
        // Add small spacer
        // add(Box.createRigidArea(new Dimension(5, 0)));
        
        // 4. SITE INFO
        JPanel sitePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        sitePanel.setAlignmentY(CENTER_ALIGNMENT);
        siteLabel = new JLabel("Site:");
        sitePanel.add(siteLabel);
        siteField = createTextField(8);
        sitePanel.add(siteField);
        
        add(sitePanel);
        
        // 5. STATUS
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        statusPanel.setAlignmentY(CENTER_ALIGNMENT);
        statusLabel = new JLabel("Status:");
        statusPanel.add(statusLabel);
        statusField = createTextField(15);
        statusPanel.add(statusField);
        
        add(statusPanel);
        
        // 6. MESSAGE - takes more space
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        messagePanel.setAlignmentY(CENTER_ALIGNMENT);
        messageLabel = new JLabel("Message:");
        messagePanel.add(messageLabel);
        messageField = createTextField(20);
        messagePanel.add(messageField);
        
        add(messagePanel);
        
        // Add a glue component to push the time to the right
        add(Box.createHorizontalGlue());
        
        // 7. TIME - always on the far right
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        timePanel.setAlignmentY(CENTER_ALIGNMENT);
        // timePanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        // timeLabel = new JLabel("T:B:M"); // Changed from "Time:"
        // timePanel.add(timeLabel);
        timeField = createTextField(4); // Increased to fit 00:00:00
        updateTimeDisplay(); // Initialize with zeros
        timePanel.add(timeField);
        
        add(timePanel);
        
        // Register with CommandBus
        getCommandBus().register(this);
    }

    private JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setEditable(false);
        field.setMaximumSize(field.getPreferredSize());
        field.setAlignmentY(CENTER_ALIGNMENT); // For BoxLayout vertical centering
        return field;
    }

    // Update LED indicators appearance
    private void setupLedIndicators() {
        int ledSize = 14; // Slightly larger for better visibility
        
        // Initialize LEDs with consistent size
        // tickLed.setPreferredSize(new Dimension(ledSize, ledSize));
        // beatLed.setPreferredSize(new Dimension(ledSize, ledSize));
        // barLed.setPreferredSize(new Dimension(ledSize, ledSize));
        
        // Ensure vertical alignment
        // tickLed.setAlignmentY(CENTER_ALIGNMENT);
        // beatLed.setAlignmentY(CENTER_ALIGNMENT);
        // barLed.setAlignmentY(CENTER_ALIGNMENT);
    }

    @Override
    public void clearSite() {
        siteField.setText(" ");
    }

    @Override
    public void setSite(String text) {
        siteField.setText(text);
    }

    @Override
    public void clearMessage() {
        messageField.setText(" ");
    }

    @Override
    public void setMessage(String text) {
        messageField.setText(text);
    }

    @Override
    public void setStatus(String text) {
        statusField.setText(text);
        repaint();
    }

    @Override
    public void clearStatus() {
        statusField.setText(" ");
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null)
            return;

        // Debug all commands received
        System.out.println("StatusBar received: " + action.getCommand());

        try {
            switch (action.getCommand()) {
                case Commands.SESSION_SELECTED, Commands.SESSION_UPDATED, Commands.SESSION_LOADED -> {
                    if (action.getData() instanceof Session session) {
                        updateSessionInfo(session);
                    }
                }
                case Commands.PLAYER_SELECTED -> {
                    if (action.getData() instanceof Player player) {
                        System.out.println("StatusBar updating player info: " + player.getName());
                        updatePlayerInfo(player);
                    }
                }
                case Commands.PLAYER_UNSELECTED -> {
                    System.out.println("StatusBar clearing player info");
                    clearPlayerInfo();
                }
                case Commands.BASIC_TIMING_TICK -> {
                    // flashTickLed();
                    
                    // Increment tick count
                    tickCount++;
                    updateTimeDisplay();
                }
                case Commands.BASIC_TIMING_BEAT -> {
                    // flashBeatLed();
                    
                    // Increment beat count and reset ticks
                    beatCount++;
                    tickCount = 0;
                    updateTimeDisplay();
                }
                case Commands.BASIC_TIMING_BAR -> {
                    // flashBarLed();
                    
                    // Increment bar count and reset beats
                    barCount++;
                    beatCount = 0;
                    updateTimeDisplay();
                }
                case Commands.TRANSPORT_PLAY, Commands.TRANSPORT_STOP -> {
                    resetTimingCounters();
                }
                default -> {
                }
            }
        } catch (Exception e) {
            System.err.println("Error in StatusBar.onAction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateSessionInfo(Session session) {
        if (session != null) {
            sessionIdField.setText(String.valueOf(session.getId()));
            playerCountField.setText(String.valueOf(session.getPlayers().size()));
        } else {
            clearSessionInfo();
        }
    }

    private void updatePlayerInfo(Player player) {
        if (player != null) {
            playerIdField.setText(String.valueOf(player.getId()));
            ruleCountField.setText(String.valueOf(player.getRules().size()));
        } else {
            clearPlayerInfo();
        }
    }

    private void clearSessionInfo() {
        sessionIdField.setText("");
        playerCountField.setText("");
    }

    private void clearPlayerInfo() {
        playerIdField.setText("");
        ruleCountField.setText("");
    }

    /**
     * Reset all timing counters and update display
     */
    private void resetTimingCounters() {
        tickCount = 0;
        beatCount = 0;
        barCount = 0;
        updateTimeDisplay();
    }
    
    /**
     * Format the time display in ticks:beats:bars format
     */
    private void updateTimeDisplay() {
        // Format as 00:00:00 (ticks:beats:bars)
        String formattedTime = String.format("%02d:%02d:%02d", 
                               tickCount, beatCount + 1, barCount);
                               
        // Update the time field on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            timeField.setText(formattedTime);
        });
    }

    private JComboBox<String> createRootNoteCombo() {
        String[] keys = {
                "C", "D", "E", "F", "G", "A", "B"
        };

        JComboBox<String> combo = new JComboBox<>(keys);
        combo.setSelectedItem("C");
        combo.setAlignmentX(Component.CENTER_ALIGNMENT);
        combo.setEnabled(true);

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                commandBus.publish(Commands.ROOT_NOTE_SELECTED, this, (String) combo.getSelectedItem());
        });

        return combo;
    }

    private JComboBox<String> createScaleCombo() {
        String[] scaleNames = Scale.SCALE_PATTERNS.keySet()
                .stream()
                .sorted()
                .toArray(String[]::new);

        JComboBox<String> combo = new JComboBox<>(scaleNames);
        combo.setSelectedItem("Chromatic");
        combo.setAlignmentX(Component.CENTER_ALIGNMENT);
        combo.setEnabled(true);

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedScale = (String) combo.getSelectedItem();
                int selectedIndex = combo.getSelectedIndex();
                commandBus.publish(Commands.SCALE_SELECTED, this, selectedScale);
                
                if (selectedIndex == 0) {
                    commandBus.publish(Commands.FIRST_SCALE_SELECTED, this, selectedScale);
                } else if (selectedIndex == combo.getItemCount() - 1) {
                    commandBus.publish(Commands.LAST_SCALE_SELECTED, this, selectedScale);
                }
            }
        });

        return combo;
    }
}
