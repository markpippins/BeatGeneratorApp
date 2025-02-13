package com.angrysurfer.beatsui.widget;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.angrysurfer.beatsui.api.StatusConsumer;

public class StatusBar extends JPanel implements StatusConsumer {

    private final JLabel siteLabel;
    private final JLabel senderLabel;
    private final JLabel statusLabel;
    private final JLabel messageLabel;
    // private final JLabel timeLabel;

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
}
