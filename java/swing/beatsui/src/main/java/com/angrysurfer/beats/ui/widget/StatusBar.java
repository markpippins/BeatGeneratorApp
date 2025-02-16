package com.angrysurfer.beats.ui.widget;

import java.awt.BorderLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusBar extends JPanel implements CommandListener, StatusConsumer {

    private final JLabel siteLabel;
    private final JLabel senderLabel;
    private final JLabel statusLabel;
    private final JLabel messageLabel;
    // private final JLabel timeLabel;

    private CommandBus commandBus = CommandBus.getInstance();

    public StatusBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 6, 8, 6));

        siteLabel = new JLabel(" ");
        siteLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(siteLabel, BorderLayout.WEST);

        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(statusLabel, BorderLayout.WEST);

        senderLabel = new JLabel(" ");
        senderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        add(senderLabel, BorderLayout.EAST);

        messageLabel = new JLabel(" ");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(messageLabel, BorderLayout.CENTER);

        getCommandBus().register(this);
    }

    @Override
    public void clearSite() {
        siteLabel.setText(" ");
    }

    @Override
    public void setSite(String text) {
        siteLabel.setText(text);
    }

    @Override
    public void clearSender() {
        senderLabel.setText(" ");
    }

    @Override
    public void setSender(String text) {
        senderLabel.setText(text);
    }

    @Override
    public void clearMessage() {
        messageLabel.setText(" ");
    }

    @Override
    public void setMessage(String text) {
        messageLabel.setText(text);
    }

    @Override
    public void setStatus(String text) {
        statusLabel.setText(text);
        repaint();
    }

    @Override
    public void clearStatus() {
        statusLabel.setText(" ");
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null)
            return;

        if (Objects.nonNull(action.getCommand()))
            setMessage(action.getCommand());

        // if (Objects.nonNull(action.getSender()))
        // setSender(action.getSender());

        // switch (action.getCommand()) {
        // case Commands.STATUS_MESSAGE:
        // setMessage((String) action.getData());
        // break;
        // case Commands.STATUS_SENDER:
        // setSender((String) action.getData());
        // break;
        // case Commands.STATUS_SITE:
        // setSite((String) action.getData());
        // break;
        // case Commands.STATUS_CLEAR:
        // clearMessage();
        // clearSender();
        // clearSite();
        // break;
        // case Commands.STATUS_STATUS:
        // setStatus((String) action.getData());
        // break;
        // case Commands.STATUS_CLEAR_STATUS:
        // clearStatus();
        // break;
        // }
    }
}
