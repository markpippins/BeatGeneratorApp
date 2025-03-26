package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.InternalSynthManager;

/**
 * Custom renderer for player table rows that centers numeric values
 */
public class PlayerRowRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    
    private final PlayersTable table;
    private static final Color PLAYING_COLOR = new Color(255, 165, 0); // Bright orange for better visibility
    
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
            Player player = table.getPlayerAtRow(row);
            
            if (player == null) {
                return c;
            }
            
            // Check if this is a numeric column (using column model index)
            int modelColumnIndex = table.getColumnModel().getColumn(column).getModelIndex();
            
            // Get numeric columns array from the table model
            int[] numericColumns = PlayersTableModel.getNumericColumns();
            
            // Check if this is the Preset column specifically
            boolean isPresetColumn = modelColumnIndex == table.getColumnIndex(PlayersTableModel.COL_PRESET);
            
            // Special handling for preset column
            if (isPresetColumn) {
                // For drum channel (channel 9), show drum name instead of preset
                if (player.getChannel() == 9) {
                    // Get the current note value from the player
                    int noteValue = player.getNote() != null ? player.getNote().intValue() : 0;
                    
                    // Get drum name from InternalSynthManager
                    String drumName = InternalSynthManager.getInstance().getDrumName(noteValue);
                    
                    // Set the drum name in the preset column
                    label.setText(drumName);
                    label.setToolTipText("Drum: " + drumName);
                    label.setHorizontalAlignment(JLabel.LEFT); // Left-align drum names as they can be long
                }
                // For internal synth instruments, show preset name
                else if (player.getInstrument() != null && 
                        InternalSynthManager.getInstance().isInternalSynth(player.getInstrument())) {
                    // Get preset name from InternalSynthManager
                    String presetName = InternalSynthManager.getInstance().getPresetName(
                        player.getInstrument().getId(), 
                        value instanceof Number ? ((Number)value).longValue() : 0
                    );
                    
                    // Use preset name instead of number
                    label.setText(presetName);
                    label.setToolTipText(presetName);
                    label.setHorizontalAlignment(JLabel.LEFT); // Changed from CENTER to LEFT for consistent text alignment
                }
                // For standard presets, just show the number
                else {
                    label.setHorizontalAlignment(JLabel.CENTER); // Keep numeric presets centered
                }
                
                // Set background color based on player state
                setBackgroundColor(label, player, isSelected);
                return c;
            }
            
            // For all other columns, handle numeric vs non-numeric formatting
            boolean isNumeric = false;
            for (int numericCol : numericColumns) {
                if (numericCol == modelColumnIndex) {
                    isNumeric = true;
                    break;
                }
            }
            
            if (isNumeric) {
                label.setHorizontalAlignment(JLabel.CENTER);
            } else {
                label.setHorizontalAlignment(JLabel.LEFT);
            }
            
            // Set background color based on player state
            setBackgroundColor(label, player, isSelected);
        }
        
        return c;
    }
    
    // Helper method to set background color based on player state
    private void setBackgroundColor(JLabel label, Player player, boolean isSelected) {
        if (player != null) {
            // First check if this player is flashing (priority over other states)
            if (table.isPlayerFlashing(player)) {
                label.setBackground(isSelected ? table.getFlashColor().darker() : table.getFlashColor());
                label.setForeground(Color.BLACK);
            }
            // Then check if playing
            else if (player.isPlaying()) {
                label.setBackground(isSelected ? PLAYING_COLOR.darker() : PLAYING_COLOR);
                label.setForeground(Color.BLACK);
            }
            // Default colors
            else {
                label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            }
        }
    }
}