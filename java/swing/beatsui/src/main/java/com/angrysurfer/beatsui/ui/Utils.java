package com.angrysurfer.beatsui.ui;

import java.awt.Color;
import java.util.stream.IntStream;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

public class Utils {

    
    // Greys & Dark Blues
    public static Color charcoalGray = new Color(40, 40, 40); // Deep console casing
    public static Color slateGray = new Color(70, 80, 90); // Cool metallic panel
    public static Color deepNavy = new Color(20, 50, 90); // Darker Neve-style blue

    public static Color mutedOlive = new Color(85, 110, 60); // Vintage military-style green
    public static Color fadedLime = new Color(140, 160, 80); // Aged LED green

    // Yellows & Oranges
    public static Color dustyAmber = new Color(200, 140, 60); // Classic VU meter glow
    public static Color warmMustard = new Color(180, 140, 50); // Retro knob indicator
    public static Color deepOrange = new Color(190, 90, 40); // Vintage warning light

    // Accents
    public static Color agedOffWhite = new Color(225, 215, 190); // Worn plastic knobs
    public static Color deepTeal = new Color(30, 80, 90); // Tascam-inspired accent

    public static Color darkGray = new Color(50, 50, 50); // Deep charcoal (console casing)
    public static Color warmGray = new Color(120, 120, 120); // Aged metal panel
    public static Color mutedRed = new Color(180, 60, 60); // Classic button color
    public static Color fadedOrange = new Color(210, 120, 50); // Vintage indicator light
    public static Color coolBlue = new Color(50, 130, 200); // Neve-style trim
    public static Color warmOffWhite = new Color(230, 220, 200);// Aged plastic knobs

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

    public static Color[] getRainbowColors() {
        return new Color[] {
            Color.RED, Color.ORANGE, Color.YELLOW,
            Color.GREEN, Color.BLUE, new Color(75, 0, 130)
        };
    }

    public static Color[] getRainbowColors(int count) {
        Color[] colors = new Color[count];
        for (int i = 0; i < count; i++) {
            colors[i] = Color.getHSBColor((float) i / count, 1, 1);
        }
        return colors;
    }

    public static Color[] getRainbowColors(int count, float saturation, float brightness) {
        Color[] colors = new Color[count];
        for (int i = 0; i < count; i++) {
            colors[i] = Color.getHSBColor((float) i / count, saturation, brightness);
        }
        return colors;
    }

    public static Color[] getRainbowColors(int count, float saturation, float brightness, float alpha) {
        Color[] colors = new Color[count];
        for (int i = 0; i < count; i++) {
            colors[i] = new Color(Color.HSBtoRGB((float) i / count, saturation, brightness));
            colors[i] = new Color(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(), (int) (alpha * 255));
        }
        return colors;
    }

    public static Color[] getRainbowColors(int count, float alpha) {
        Color[] colors = new Color[count];
        for (int i = 0; i < count; i++) {
            colors[i] = new Color(Color.HSBtoRGB((float) i / count, 1, 1));
            colors[i] = new Color(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(), (int) (alpha * 255));
        }
        return colors;
    }

}
