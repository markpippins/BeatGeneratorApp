package com.angrysurfer.beats.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
}
