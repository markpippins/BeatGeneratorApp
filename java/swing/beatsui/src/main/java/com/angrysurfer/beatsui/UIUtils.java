package com.angrysurfer.beatsui;

import java.util.stream.IntStream;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

public class UIUtils {

    public static void setupColumnEditor(JTable table, String columnName, int min, int max) {
        int columnIndex = getColumnIndex(table, columnName);
        if (columnIndex != -1) {
            TableColumn column = table.getColumnModel().getColumn(columnIndex);

            // Create combo box with range values
            JComboBox<Integer> comboBox = new JComboBox<>(
                    IntStream.rangeClosed(min, max)
                            .boxed()
                            .toArray(Integer[]::new));

            // Make combo box editable for quick value entry
            comboBox.setEditable(true);

            // Set preferred width based on maximum value's width
            int width = Math.max(60, comboBox.getPreferredSize().width + 20);
            column.setPreferredWidth(width);

            // Create and set the cell editor
            DefaultCellEditor editor = new DefaultCellEditor(comboBox) {
                @Override
                public boolean stopCellEditing() {
                    try {
                        // Validate input range
                        int value = Integer.parseInt(comboBox.getEditor().getItem().toString());
                        if (value >= min && value <= max) {
                            return super.stopCellEditing();
                        }
                        // Show error message if out of range
                        // setStatus(String.format("Value must be between %d and %d", min, max));
                        return false;
                    } catch (NumberFormatException e) {
                        // setStatus("Please enter a valid number");
                        return false;
                    }
                }
            };

            column.setCellEditor(editor);
        }
    }

    public static int getColumnIndex(JTable table, String columnName) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnName(i).equals(columnName)) {
                return i;
            }
        }
        return -1;
    }
}
