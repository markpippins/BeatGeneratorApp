package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusBar extends JPanel implements CommandListener, StatusConsumer {

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

    public StatusBar() {
        super(new BorderLayout());
        setup();

        // Request initial session state through CommandBus
        SwingUtilities.invokeLater(() -> {
            CommandBus.getInstance().publish(Commands.SESSION_REQUEST, this);
        });
    }

    private void setup() {
        setBorder(BorderFactory.createEmptyBorder(2, 6, 8, 6));

        JPanel sessionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        sessionIdLabel = new JLabel("Session: ");
        sessionPanel.add(sessionIdLabel);
        sessionIdField = createTextField(2); // Reduced from 4
        sessionPanel.add(sessionIdField);

        playerCountLabel = new JLabel("Players: ");
        sessionPanel.add(playerCountLabel);
        playerCountField = createTextField(2); // Reduced from 4
        sessionPanel.add(playerCountField);

        playerIdLabel = new JLabel("Player: ");
        sessionPanel.add(playerIdLabel);
        playerIdField = createTextField(2); // Reduced from 4
        sessionPanel.add(playerIdField);

        ruleCountLabel = new JLabel("Rules: ");
        sessionPanel.add(ruleCountLabel);
        ruleCountField = createTextField(2); // Reduced from 4
        sessionPanel.add(ruleCountField);

        add(sessionPanel, BorderLayout.WEST);

        JPanel statusPanel = new JPanel(new BorderLayout());

        JPanel leftStatusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        siteLabel = new JLabel("Site: ");
        leftStatusPanel.add(siteLabel);
        siteField = createTextField(10);
        leftStatusPanel.add(siteField);

        statusLabel = new JLabel("Status: ");
        leftStatusPanel.add(statusLabel);
        statusField = createTextField(10); // Reduced from 32
        leftStatusPanel.add(statusField);

        statusPanel.add(leftStatusPanel, BorderLayout.WEST);

        messageLabel = new JLabel("Message: ");
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        messagePanel.add(messageLabel);
        messageField = createTextField(20); // Reduced from 60
        messagePanel.add(messageField);
        statusPanel.add(messagePanel, BorderLayout.CENTER);

        timeLabel = new JLabel("Time: ");
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        timePanel.add(timeLabel);
        timeField = createTextField(6);
        timePanel.add(timeField);
        statusPanel.add(timePanel, BorderLayout.EAST);

        add(statusPanel, BorderLayout.CENTER);

        getCommandBus().register(this);
    }

    private JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setEditable(false);
        return field;
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

        switch (action.getCommand()) {
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
            case Commands.PLAYER_UNSELECTED -> clearPlayerInfo();
            default -> setMessage(action.getCommand());
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
            playerIdField.setText(String.valueOf(   player.getId()));
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
}
