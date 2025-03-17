package com.angrysurfer.beats.renderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.angrysurfer.beats.ColorUtils;
import com.angrysurfer.beats.panel.PlayersPanel;
import com.angrysurfer.core.model.Player;

public class PlayerRowRenderer extends DefaultTableCellRenderer {
    /**
     *
     */
    private final PlayersPanel playersPanel;

    /**
     * @param playersPanel
     */
    public PlayerRowRenderer(PlayersPanel playersPanel) {
        this.playersPanel = playersPanel;
    }

    private static final Color ACTIVE_COLOR = ColorUtils.dustyAmber; // new Color(200, 255, 200); // Light green

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        // Get the player for this row
        Player player = this.playersPanel.getPlayerAtRow(row);

        if (player != null && player.isPlaying()) {
            // Set background to green for active players
            if (isSelected) {
                c.setBackground(ACTIVE_COLOR.darker());
            } else {
                c.setBackground(ACTIVE_COLOR);
            }
        } else {
            // Use default colors
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            } else {
                c.setBackground(table.getBackground());
            }
        }

        return c;
    }
}