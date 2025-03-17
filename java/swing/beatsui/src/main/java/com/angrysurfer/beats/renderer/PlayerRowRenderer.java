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
    private static final Color FLASH_COLOR = new Color(255, 255, 200);  // Assuming you have a flash color defined in ColorUtils

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(table, value, 
                                                         isSelected, hasFocus, row, column);
        
        PlayersPanel panel = this.playersPanel;
        Player player = panel.getPlayerAtRow(row);
        
        if (player != null) {
            // First check if this player is flashing (priority over other states)
            if (panel.isPlayerFlashing(player.getName())) {
                c.setBackground(isSelected ? FLASH_COLOR.darker() : FLASH_COLOR);
            }
            // Then check if playing
            else if (player.isPlaying()) {
                c.setBackground(isSelected ? ColorUtils.dustyAmber.darker() : ColorUtils.dustyAmber);
            }
            // Default colors
            else {
                c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            }
        }
        
        return c;
    }
}