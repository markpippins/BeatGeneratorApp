package com.angrysurfer.beatsui.api;

import java.util.Objects;

public class StatusUpdater {
    private final StatusConsumer statusConsumer;

    public StatusUpdater(StatusConsumer statusConsumer) {
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
