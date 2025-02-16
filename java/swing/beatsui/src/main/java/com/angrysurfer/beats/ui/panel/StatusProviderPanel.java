package com.angrysurfer.beats.ui.panel;

import javax.swing.JPanel;

import com.angrysurfer.core.api.StatusConsumer;

import java.awt.LayoutManager;
import java.util.Objects;

public class StatusProviderPanel extends JPanel {
    protected StatusConsumer statusConsumer;

    public StatusProviderPanel() {
        super();
    }

    public StatusProviderPanel(LayoutManager layout) {
        super(layout);
    }

    public StatusProviderPanel(StatusConsumer statusConsumer) {
        this();
        this.statusConsumer = statusConsumer;
    }

    public StatusProviderPanel(LayoutManager layout, StatusConsumer statusConsumer) {
        super(layout);
        this.statusConsumer = statusConsumer;
    }

    public void setSite(String site) {
        if (Objects.nonNull(statusConsumer)) {
            statusConsumer.setSite(site);
        }
    }

    public void clearSite() {
        if (Objects.nonNull(statusConsumer)) {
            statusConsumer.clearSite();
        }
    }

    public void setMessage(String message) {
        if (Objects.nonNull(statusConsumer)) {
            statusConsumer.setMessage(message);
        }
    }

    public void clearMessage() {
        if (Objects.nonNull(statusConsumer)) {
            statusConsumer.clearMessage();
            ;
        }
    }

    public void setSender(String sender) {
        if (Objects.nonNull(statusConsumer)) {
            statusConsumer.setSender(sender);
        }
    }

    public void clearSender() {
        if (Objects.nonNull(statusConsumer)) {
            statusConsumer.clearSender();
        }
    }

    public void setStatus(String status) {
        if (Objects.nonNull(statusConsumer)) {
            statusConsumer.setStatus(status);
        }
    }

    public void clearStatus() {
        if (Objects.nonNull(statusConsumer)) {
            statusConsumer.clearStatus();
        }
    }
}
