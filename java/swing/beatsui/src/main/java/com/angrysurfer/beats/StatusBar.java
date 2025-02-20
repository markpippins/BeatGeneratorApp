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
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusBar extends JPanel implements CommandListener, StatusConsumer {

    private JLabel tickerIdLabel;
    private JLabel playerCountLabel;
    private JLabel playerIdLabel;
    private JLabel ruleCountLabel;
    private JLabel siteLabel;
    private JLabel statusLabel;
    private JLabel messageLabel;
    private JLabel timeLabel;

    private JTextField tickerIdField;
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

        // Request initial ticker state through CommandBus
        SwingUtilities.invokeLater(() -> {
            CommandBus.getInstance().publish(Commands.TICKER_REQUEST, this);
        });
    }

    private void setup() {
        setBorder(BorderFactory.createEmptyBorder(2, 6, 8, 6));

        JPanel tickerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        tickerIdLabel = new JLabel("Ticker: ");
        tickerPanel.add(tickerIdLabel);
        tickerIdField = createTextField(2); // Reduced from 4
        tickerPanel.add(tickerIdField);

        playerCountLabel = new JLabel("Players: ");
        tickerPanel.add(playerCountLabel);
        playerCountField = createTextField(2); // Reduced from 4
        tickerPanel.add(playerCountField);

        playerIdLabel = new JLabel("Player: ");
        tickerPanel.add(playerIdLabel);
        playerIdField = createTextField(2); // Reduced from 4
        tickerPanel.add(playerIdField);

        ruleCountLabel = new JLabel("Rules: ");
        tickerPanel.add(ruleCountLabel);
        ruleCountField = createTextField(2); // Reduced from 4
        tickerPanel.add(ruleCountField);

        add(tickerPanel, BorderLayout.WEST);

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
            case Commands.TICKER_SELECTED, Commands.TICKER_UPDATED, Commands.TICKER_LOADED -> {
                if (action.getData() instanceof ProxyTicker ticker) {
                    updateTickerInfo(ticker);
                }
            }
            case Commands.PLAYER_SELECTED -> {
                if (action.getData() instanceof ProxyStrike player) {
                    updatePlayerInfo(player);
                }
            }
            case Commands.PLAYER_UNSELECTED -> clearPlayerInfo();
            default -> setMessage(action.getCommand());
        }
    }

    private void updateTickerInfo(ProxyTicker ticker) {
        if (ticker != null) {
            tickerIdField.setText(String.valueOf(ticker.getId()));
            playerCountField.setText(String.valueOf(ticker.getPlayers().size()));
        } else {
            clearTickerInfo();
        }
    }

    private void updatePlayerInfo(ProxyStrike player) {
        if (player != null) {
            playerIdField.setText(String.valueOf(player.getId()));
            ruleCountField.setText(String.valueOf(player.getRules().size()));
        } else {
            clearPlayerInfo();
        }
    }

    private void clearTickerInfo() {
        tickerIdField.setText("");
        playerCountField.setText("");
    }

    private void clearPlayerInfo() {
        playerIdField.setText("");
        ruleCountField.setText("");
    }
}
