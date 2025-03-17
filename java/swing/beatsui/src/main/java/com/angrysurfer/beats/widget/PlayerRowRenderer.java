package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.angrysurfer.beats.ColorUtils;
import com.angrysurfer.core.model.Player;

public class PlayerRowRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    
    private final PlayersTable table;
    private static final Color PLAYING_COLOR = ColorUtils.dustyAmber;
    
    public PlayerRowRenderer(PlayersTable table) {
        this.table = table;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable ignored, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        Component c = super.getTableCellRendererComponent(table, value, 
                                                     isSelected, hasFocus, row, column);
        
        Player player = table.getPlayerAtRow(row);
        
        if (player != null) {
            // First check if this player is flashing (priority over other states)
            if (table.isPlayerFlashing(player.getName())) {
                c.setBackground(isSelected ? table.getFlashColor().darker() : table.getFlashColor());
            }
            // Then check if playing
            else if (player.isPlaying()) {
                c.setBackground(isSelected ? PLAYING_COLOR.darker() : PLAYING_COLOR);
            }
            // Default colors
            else {
                c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            }
        }
        
        return c;
    }
}