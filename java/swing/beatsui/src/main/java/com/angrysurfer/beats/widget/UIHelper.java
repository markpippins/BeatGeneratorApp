package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTextField;
import javax.swing.JTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.config.TableState;
import com.angrysurfer.core.redis.RedisService;

public class UIHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(UIHelper.class.getName());

    private static UIHelper instance;

    private UIHelper() {
    }

    public static UIHelper getInstance() {
        if (instance == null) {
            instance = new UIHelper();
        }
        return instance;
    }

    
    public void saveColumnOrder(JTable table, String tableName, Set<String> columns) {
        try {
            TableState state = RedisService.getInstance().loadTableState(tableName);
            if (state != null) {
                List<String> columnOrder = new ArrayList<>();
                // Get visible column order
                for (int i = 0; i < table.getColumnCount(); i++) {
                    int modelIndex = table.convertColumnIndexToModel(i);
                    String columnName = columns.toArray()[modelIndex].toString();
                    columnOrder.add(columnName);
                }

                // Only save if we have all columns
                if (columnOrder.size() == columns.size()) {
                    logger.info("Saving column order: " + String.join(", ", columnOrder));
                    state.setColumnOrder(columnOrder);
                    RedisService.getInstance().saveTableState(state, tableName);
                } else {
                    logger.error("Column order incomplete, not saving");
                }
            }
        } catch (Exception e) {
            logger.error("Error saving column order: " + e.getMessage());
        }
    }

    public void restoreColumnOrder(JTable table, String tableName, Set<String> columns) {
        try {
            TableState state = RedisService.getInstance().loadTableState(tableName);
            List<String> savedOrder = state != null ? state.getColumnOrder() : null;

            if (savedOrder != null && !savedOrder.isEmpty() && savedOrder.size() == columns.size()) {
                logger.info("Restoring column order: " + String.join(", ", savedOrder));

                // Create a map of column names to their current positions
                Map<String, Integer> currentOrder = new HashMap<>();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    currentOrder.put(columns.toArray()[i].toString(), i);
                }

                // Move each column to its saved position
                for (int i = 0; i < savedOrder.size(); i++) {
                    String colName = savedOrder.get(i);
                    Integer currentPos = currentOrder.get(colName);
                    if (currentPos != null && currentPos != i) {
                        table.getColumnModel().moveColumn(currentPos, i);
                        // Update the currentOrder map after moving
                        for (Map.Entry<String, Integer> entry : currentOrder.entrySet()) {
                            if (entry.getValue() == i) {
                                currentOrder.put(entry.getKey(), currentPos);
                                break;
                            }
                        }
                        currentOrder.put(colName, i);
                    }
                }
            } else {
                logger.info("No valid column order found to restore");
            }
        } catch (Exception e) {
            logger.error("Error restoring column order: " + e.getMessage());
        }
    }

    /**
     * Creates a standardized text field with customizable properties
     * 
     * @param initialValue The initial text value (optional, can be null)
     * @param columns The number of columns (use -1 to keep default)
     * @param editable Whether the field is editable
     * @param enabled Whether the field is enabled
     * @param centered Whether text should be center-aligned
     * @param backgroundColor Background color (null for default)
     * @return The configured JTextField
     */
    public JTextField createTextField(String initialValue, int columns, 
                                     boolean editable, boolean enabled, 
                                     boolean centered, Color backgroundColor) {
        JTextField field;
        
        // Create with initial text or columns
        if (initialValue != null) {
            field = new JTextField(initialValue);
            if (columns > 0) {
                field.setColumns(columns);
            }
        } else {
            field = new JTextField(columns > 0 ? columns : 10); // Default to 10 columns
        }
        
        // Apply common settings
        field.setEditable(editable);
        field.setEnabled(enabled);
        
        // Optional text alignment
        if (centered) {
            field.setHorizontalAlignment(JTextField.CENTER);
        }
        
        // Optional background color
        if (backgroundColor != null) {
            field.setBackground(backgroundColor);
        }
        
        // Set both alignment properties to CENTER
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        return field;
    }

    /**
     * Overloaded method with fewer parameters for simpler cases
     */
    public JTextField createTextField(String initialValue, int columns) {
        return createTextField(initialValue, columns, false, true, true, null);
    }

    /**
     * Creates a status display field (non-editable, with initial value)
     */
    public JTextField createStatusField(String initialValue, int columns) {
        Color lightGray = new Color(240, 240, 240);
        JTextField field = createTextField(initialValue, columns, false, false, true, lightGray);
        field.setMaximumSize(new Dimension(columns * 10, 25)); // Rough size approximation
        return field;
    }

    /**
     * Creates a disabled status field with consistent sizing
     */
    public JTextField createDisplayField(String initialValue) {
        Color lightGray = new Color(240, 240, 240);
        JTextField field = createTextField(initialValue, 4, false, false, true, lightGray);
        field.setMaximumSize(new Dimension(50, 25));
        return field;
    }
}
