package com.angrysurfer.core.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Ticker;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClockSource {
    private static final Logger logger = LoggerFactory.getLogger(ClockSource.class);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final TimingBus timingBus;

    public ClockSource() {
        this.timingBus = TimingBus.getInstance();
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.debug("Starting ClockSource");
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.debug("Stopping ClockSource");
        }
    }

    public void pause() {
        stop();
    }

    public boolean isRunning() {
        return running.get();
    }
}