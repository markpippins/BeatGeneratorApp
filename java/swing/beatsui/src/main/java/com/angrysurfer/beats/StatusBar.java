package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.widget.UIHelper;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusBar extends JPanel implements IBusListener {

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

    private int tickCount = 0;
    private int beatCount = 0;
    private int barCount = 0;
    private int partCount = 0;

    private Map<String, JComponent> rightFields = new HashMap<>();

    private UIHelper uiHelper = UIHelper.getInstance();

    public StatusBar() {
        super();

        setup();

        TimingBus.getInstance().register(this);

        resetTimingCounters();

        SwingUtilities.invokeLater(() -> {
            CommandBus.getInstance().publish(Commands.SESSION_REQUEST, this);
        });
    }

    private void setup() {
        // Use BorderLayout for the main container
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        
        // Create a main panel with GridBagLayout for more precise control
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Left panel - combines session and player info with no spacing
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        
        // 1. SESSION INFO GROUP - no right margin to eliminate space
        JPanel sessionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        sessionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        sessionIdLabel = new JLabel("Session:");
        sessionPanel.add(sessionIdLabel);
        sessionIdField = uiHelper.createTextField("", 2);
        sessionPanel.add(sessionIdField);
        
        playerCountLabel = new JLabel("Players:");
        sessionPanel.add(playerCountLabel);
        playerCountField = uiHelper.createStatusField("", 2);
        sessionPanel.add(playerCountField);
        
        leftPanel.add(sessionPanel);
        
        // 2. PLAYER INFO GROUP - no left margin to eliminate space
        JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        playerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        playerIdLabel = new JLabel("Player:");
        playerPanel.add(playerIdLabel);
        playerIdField = uiHelper.createTextField("", 2);
        playerPanel.add(playerIdField);
        
        ruleCountLabel = new JLabel("Rules:");
        playerPanel.add(ruleCountLabel);
        ruleCountField = uiHelper.createTextField("", 2);
        playerPanel.add(ruleCountField);
        
        leftPanel.add(playerPanel);
        
        // Add the left panel to the main panel
        mainPanel.add(leftPanel, BorderLayout.WEST);
        
        // 3. MIDDLE PANEL for Site, Status, and Message - left aligned and filling available space
        JPanel middlePanel = new JPanel(new GridLayout(1, 3, 10, 0));
        
        // 3a. SITE INFO (left-aligned)
        JPanel sitePanel = new JPanel(new BorderLayout(3, 0));
        siteLabel = new JLabel("Site:");
        sitePanel.add(siteLabel, BorderLayout.WEST);
        siteField = uiHelper.createTextField("", 1);
        sitePanel.add(siteField, BorderLayout.CENTER);
        middlePanel.add(sitePanel);
        
        // 3b. STATUS (left-aligned)
        JPanel statusPanel = new JPanel(new BorderLayout(3, 0));
        statusLabel = new JLabel("Status:");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusField = uiHelper.createTextField("", 1);
        statusPanel.add(statusField, BorderLayout.CENTER);
        middlePanel.add(statusPanel);
        
        // 3c. MESSAGE (left-aligned)
        JPanel messagePanel = new JPanel(new BorderLayout(3, 0));
        messageLabel = new JLabel("Message:");
        messagePanel.add(messageLabel, BorderLayout.WEST);
        messageField = uiHelper.createTextField("", 1);
        messagePanel.add(messageField, BorderLayout.CENTER);
        middlePanel.add(messagePanel);
        
        // Add the middle panel to the main panel (it will fill available space)
        mainPanel.add(middlePanel, BorderLayout.CENTER);
        
        // 4. TIME panel on the far right
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        timeField = uiHelper.createTextField("", 8);
        updateTimeDisplay(); // Initialize with zeros
        timePanel.add(timeField);
        
        // Add the time panel to the main panel
        mainPanel.add(timePanel, BorderLayout.EAST);
        
        // Add the main panel to the status bar
        add(mainPanel, BorderLayout.CENTER);
        
        // Register with CommandBus
        getCommandBus().register(this);
    }

    public void clearSite() {
        siteField.setText(" ");
    }

    public void setSite(String text) {
        siteField.setText(text);
    }

    public void clearMessage() {
        messageField.setText(" ");
    }

    public void setMessage(String text) {
        messageField.setText(text);
    }

    public void setStatus(String text) {
        statusField.setText(text);
        repaint();
    }

    public void clearStatus() {
        statusField.setText(" ");
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null)
            return;

        try {
            switch (action.getCommand()) {
            case Commands.STATUS_UPDATE -> {
                if (action.getData() instanceof StatusUpdate update) {
                    // Update only the fields that are provided (non-null)
                    if (update.site() != null) {
                        setSite(update.site());
                    }
                    if (update.status() != null) {
                        setStatus(update.status());
                    }
                    if (update.message() != null) {
                        setMessage(update.message());
                    }
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
            case Commands.PLAYER_UNSELECTED -> {
                clearPlayerInfo();
            }
            case Commands.TIMING_TICK -> {
                if (action.getData() instanceof Number tick) {
                    tickCount = tick.intValue();
                    updateTimeDisplay();
                }
            }
            case Commands.TIMING_BEAT -> {
                if (action.getData() instanceof Number beat) {
                    beatCount = beat.intValue();
                    updateTimeDisplay();
                }
            }
            case Commands.TIMING_BAR -> {
                if (action.getData() instanceof Number bar) {
                    barCount = bar.intValue();
                    updateTimeDisplay();
                }
            }
            case Commands.TIMING_PART -> {
                if (action.getData() instanceof Number partVal) {
                    partCount = partVal.intValue();
                    updateTimeDisplay();
                }
            }
            case Commands.TRANSPORT_PLAY -> {
                resetTimingCounters();
                setStatus("Playing");
            }
            case Commands.TRANSPORT_STOP -> {
                resetTimingCounters();
                setStatus("Stopped");
            }
            case Commands.TRANSPORT_RECORD -> {
                setStatus("Recording");
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

    private void resetTimingCounters() {
        tickCount = 0;
        beatCount = 0;
        barCount = 0;
        partCount = 0;
        updateTimeDisplay();
    }

    private void updateTimeDisplay() {
        String formattedTime = String.format("%02d:%02d:%02d:%02d", tickCount + 1, beatCount + 1, barCount + 1,
                partCount + 1);

        SwingUtilities.invokeLater(() -> {
            timeField.setText(formattedTime);
        });
    }
}
