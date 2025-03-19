package com.angrysurfer.beats.visualization;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LockHandler {

    private boolean locked = false;

    public LockHandler() {
    }

    public void lockDisplay() {
        CommandBus.getInstance().publish(Commands.LOCK_CURRENT_VISUALIZATION);
        locked = true;
    }

    public void unlockDisplay() {
        CommandBus.getInstance().publish(Commands.UNLOCK_CURRENT_VISUALIZATION);
        locked = false;
    }
}
