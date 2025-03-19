package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.angrysurfer.beats.ColorUtils;
import com.angrysurfer.core.model.Player;

/**
 * Custom renderer for player table rows that centers numeric values
 */
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
        
        // Use our table reference instead of the provided one
        Component c = super.getTableCellRendererComponent(table, value, 
                                                     isSelected, hasFocus, row, column);
        
        if (c instanceof JLabel) {
            JLabel label = (JLabel)c;
            
            // Check if this is a numeric column (using column model index)
            int modelColumnIndex = table.getColumnModel().getColumn(column).getModelIndex();
            
            // Get numeric columns array from the table model
            int[] numericColumns = PlayersTableModel.getNumericColumns();
            
            // Center alignment for numeric columns
            boolean isNumeric = false;
            for (int numericCol : numericColumns) {
                if (numericCol == modelColumnIndex) {
                    isNumeric = true;
                    break;
                }
            }
            
            // Set alignment based on column type
            if (isNumeric) {
                label.setHorizontalAlignment(JLabel.CENTER);
            } else {
                label.setHorizontalAlignment(JLabel.LEFT);
            }
        }
        
        // Background color logic
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