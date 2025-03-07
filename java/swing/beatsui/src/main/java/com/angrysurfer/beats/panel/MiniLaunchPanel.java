package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.ColorUtils;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public class MiniLaunchPanel extends StatusProviderPanel implements CommandListener {
    private static final int BUTTON_SIZE = 40;
    private static final int GRID_ROWS = 2;
    private static final int GRID_COLS = 4;
    private static final int GRID_GAP = 5;
    private final CommandBus commandBus = CommandBus.getInstance();
    
    // Add label mapping array
    private static final int[] PAD_LABELS = {
        43, 44, 45, 46,  // top row
        36, 37, 38, 39   // bottom row
    };

    private static final class PadButton extends JButton {
        private final int midiNote;
        private boolean isFlashing;

        PadButton(int midiNote) {
            super(String.valueOf(midiNote));
            this.midiNote = midiNote;
            this.isFlashing = false;
        }

        public int getMidiNote() {
            return midiNote;
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
        if (Commands.CHANGE_THEME.equals(action.getCommand())) {
            SwingUtilities.invokeLater(this::repaint);
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
            Math.min(baseColor.getBlue() + 100, 255)
        );

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

        // Update action listener to send command
        button.addActionListener(e -> {
            button.setFlashing(true);
            setStatus("Mini pad " + midiNote + " pressed");
            
            // Send command with MIDI note value
            commandBus.publish(Commands.MINI_NOTE_SELECTED, this, midiNote);

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
}