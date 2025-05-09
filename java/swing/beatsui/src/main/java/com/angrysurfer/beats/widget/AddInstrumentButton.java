package com.angrysurfer.beats.widget;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.model.Player;

import lombok.Getter;
import lombok.Setter;

/**
 * A button for adding new instruments and directly assigning them to the active player
 */
@Getter
@Setter
public class AddInstrumentButton extends JButton implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(AddInstrumentButton.class);
    private final CommandBus commandBus = CommandBus.getInstance();
    private Player currentPlayer;
    
    /**
     * Create a new Add Instrument button
     */
    public AddInstrumentButton() {
        super("+");
        
        // Set appropriate styling
        setToolTipText("Create a new instrument for this player");
        setMargin(new Insets(1, 4, 1, 4)); 
        setPreferredSize(new Dimension(24, 24));
        
        // Register for events
        commandBus.register(this);
        
        // Add action handler
        addActionListener(e -> handleButtonClick());
    }
    
    /**
     * Create with an initial player
     */
    public AddInstrumentButton(Player player) {
        this();
        this.currentPlayer = player;
    }
    
    /**
     * Handle button click - request instrument creation dialog via command bus
     */
    private void handleButtonClick() {
        if (currentPlayer == null) {
            commandBus.publish(
                Commands.STATUS_UPDATE, 
                this, 
                new StatusUpdate("AddInstrumentButton", "Warning", "No player selected")
            );
            return;
        }
        
        // Request the dialog via command bus - the DialogManager will handle it
        commandBus.publish(Commands.CREATE_INSTRUMENT_FOR_PLAYER_REQUEST, this, currentPlayer);
    }
    
    /**
     * Handle command bus events - primarily player selection
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }
        
        switch (action.getCommand()) {
            case Commands.PLAYER_ACTIVATED:
                if (action.getData() instanceof Player player) {
                    currentPlayer = player;
                    setEnabled(player != null);
                }
                break;
        }
    }
}