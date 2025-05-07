package com.angrysurfer.beats.panel;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;

import javax.swing.*;

public abstract class PlayerAwarePanel extends JPanel implements IBusListener {
    private static final long serialVersionUID = 1L;

    private  Player player;

    public PlayerAwarePanel() {
        super();
        CommandBus.getInstance().register(this);
    }


    @Override
    public void onAction(Command action) {
        String command = action.getCommand();
        switch (command) {
            case Commands.PLAYER_ACTIVATED -> setPlayerActive((Player) action.getData());
            case Commands.PLAYER_UPDATED -> setUpdatedPlayerActive((Player) action.getData());
        }
    }

    void setUpdatedPlayerActive(Player player) {
        if (player != this.player) {
            this.player = player;
            handlePlayerUpdated();
        }
    }

    void setPlayerActive(Player player) {
        if (player != this.player) {
            this.player = player;
            handlePlayerActivated();
        }
    }

    protected Player getPlayer() {
        return  player;
    }

    public abstract void handlePlayerActivated();
    public abstract void handlePlayerUpdated();

}