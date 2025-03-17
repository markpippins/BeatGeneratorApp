package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.ColorUtils;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;

public class MiniLaunchPanel extends StatusProviderPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(MiniLaunchPanel.class);

    private static final int BUTTON_SIZE = 40;
    private static final int GRID_ROWS = 2;
    private static final int GRID_COLS = 4;
    private static final int GRID_GAP = 5;
    private final CommandBus commandBus = CommandBus.getInstance();

    // Add label mapping array
    private static final int[] PAD_LABELS = {
            43, 44, 45, 46, // top row
            36, 37, 38, 39 // bottom row
    };

    // Keep track of last preset sent to each channel
    private final Map<Integer, Long> lastSentPresets = new HashMap<>();

    private static final class PadButton extends JButton {
        private final int midiNote;
        private boolean isFlashing;

        PadButton(int midiNote) {
            super(String.valueOf(midiNote));
            this.midiNote = midiNote;
            this.isFlashing = false;
        }

        public void setFlashing(boolean flashing) {
            isFlashing = flashing;
            repaint();
        }

        public boolean isFlashing() {
            return isFlashing;
        }
    }

    public MiniLaunchPanel(StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        commandBus.register(this);
        setup();
    }

    @Override
    public void onAction(com.angrysurfer.core.api.Command action) {
        if (action.getCommand() == null) return;
        
        switch(action.getCommand()) {
            case Commands.CHANGE_THEME -> SwingUtilities.invokeLater(this::repaint);
            
            case Commands.MINI_NOTE_SELECTED -> {
                if (action.getData() instanceof Integer midiNote) {
                    sendNoteToActivePlayer(midiNote);
                }
            }
        }
    }

    private void setup() {
        setLayout(new BorderLayout());
        JPanel gridPanel = createGridPanel();
        add(gridPanel, BorderLayout.CENTER);

        // Set preferred size based on content
        int width = GRID_COLS * (BUTTON_SIZE + GRID_GAP) + GRID_GAP;
        int height = GRID_ROWS * (BUTTON_SIZE + GRID_GAP) + GRID_GAP;
        setPreferredSize(new Dimension(width, height));
    }

    private JPanel createGridPanel() {
        JPanel gridPanel = new JPanel(new GridLayout(GRID_ROWS, GRID_COLS, GRID_GAP, GRID_GAP));
        gridPanel.setBorder(new EmptyBorder(GRID_GAP, GRID_GAP, GRID_GAP, GRID_GAP));
        gridPanel.setOpaque(false);

        for (int i = 0; i < GRID_ROWS * GRID_COLS; i++) {
            int midiNote = PAD_LABELS[i];
            PadButton padButton = createPadButton(midiNote);
            gridPanel.add(padButton);
        }

        return gridPanel;
    }

    private PadButton createPadButton(int midiNote) {
        PadButton button = new PadButton(midiNote);
        Color baseColor = ColorUtils.mutedRed;
        Color flashColor = new Color(
                Math.min(baseColor.getRed() + 100, 255),
                Math.min(baseColor.getGreen() + 100, 255),
                Math.min(baseColor.getBlue() + 100, 255));

        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Fill background
                g2d.setColor(button.isFlashing() ? flashColor : baseColor);
                g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Draw border
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Draw highlight
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                // Draw text
                String text = button.getText();
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                g2d.drawString(text,
                        (w - textWidth) / 2,
                        ((h + textHeight) / 2) - fm.getDescent());

                g2d.dispose();
            }
        });

        // Update action listener to directly send note
        button.addActionListener(e -> {
            // Send note directly
            sendNoteToActivePlayer(midiNote);
            
            // Also publish event for other components that might need it
            CommandBus.getInstance().publish(Commands.MINI_NOTE_SELECTED, this, midiNote);

            // Visual feedback
            button.setFlashing(true);
            Timer timer = new Timer(100, evt -> {
                button.setFlashing(false);
                ((Timer) evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        });

        button.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setToolTipText("MIDI Note " + midiNote);

        return button;
    }

    /**
     * Sends a MIDI note to the active player without triggering heavy updates
     * 
     * @param midiNote The MIDI note to send
     * @return true if note was successfully sent, false otherwise
     */
    public boolean sendNoteToActivePlayer(int midiNote) {

        Player activePlayer = PlayerManager.getInstance().getActivePlayer();

        if (activePlayer == null) {
            // logger.debug("No active player to receive MIDI note: {}", midiNote);
            return false;
        }

        if (SessionManager.getInstance().isRecording()) {
            CommandBus.getInstance().publish(Commands.NEW_VALUE_NOTE, this, midiNote);
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        }   


        try {
            // Use the player's instrument, channel, and a reasonable velocity
            Instrument instrument = activePlayer.getInstrument();
            if (instrument == null) {
                logger.debug("Active player has no instrument");
                return false;
            }

            int channel = activePlayer.getChannel();

            // Only send program change if preset has changed for this channel
            if (activePlayer.getPreset() != null) {
                Long lastPreset = lastSentPresets.get(channel);
                Long currentPreset = activePlayer.getPreset();

                // Send program change only if the preset has changed for this channel
                if (lastPreset == null || !lastPreset.equals(currentPreset)) {
                    try {
                        logger.debug("Sending program change: channel={}, preset={}",
                                channel, currentPreset);
                        instrument.programChange(channel, currentPreset, 0);

                        // Remember this preset for this channel
                        lastSentPresets.put(channel, currentPreset);
                    } catch (Exception e) {
                        logger.warn("Failed to send program change: {}", e.getMessage());
                    }
                }
            }

            // Calculate velocity from player settings
            int velocity = (int) Math.round((activePlayer.getMinVelocity() + activePlayer.getMaxVelocity()) / 2.0);

            // Just update the note in memory temporarily - don't save to Redis
            activePlayer.setNote((long) midiNote);

            // Send the note to the device
            logger.debug("Sending note: note={}, channel={}, velocity={}", midiNote, channel, velocity);
            instrument.noteOn(channel, midiNote, velocity);

            // Schedule note-off after a reasonable duration
            long duration = 250; // milliseconds
            new java.util.Timer(true).schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            try {
                                instrument.noteOff(channel, midiNote, 0);
                            } catch (Exception e) {
                                logger.debug("Error sending note-off: {}", e.getMessage());
                            }
                        }
                    },
                    duration);

            return true;

        } catch (Exception e) {
            logger.warn("Error sending MIDI note: {}", e.getMessage());
            return false;
        }
    }
}