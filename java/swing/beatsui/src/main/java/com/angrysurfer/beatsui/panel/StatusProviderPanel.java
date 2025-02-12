package com.angrysurfer.beatsui.panel;

import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.Objects;
import com.angrysurfer.beatsui.api.StatusConsumer;

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
