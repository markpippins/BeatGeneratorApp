package com.angrysurfer.beats.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.TableState;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.PlayerManager;

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
     * @param initialValue    The initial text value (optional, can be null)
     * @param columns         The number of columns (use -1 to keep default)
     * @param editable        Whether the field is editable
     * @param enabled         Whether the field is enabled
     * @param centered        Whether text should be center-aligned
     * @param backgroundColor Background color (null for default)
     * @return The configured JTextField
     */
    public JTextField createTextField(String initialValue, int columns, boolean editable, boolean enabled,
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

    public JPanel createVerticalAdjustPanel(String label, String upLabel, String downLabel, String upCommand,
            String downCommand) {
        JPanel navPanel = new JPanel(new BorderLayout(0, 2));
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Add margins
        JLabel octaveLabel = new JLabel(label, JLabel.CENTER);

        // Create up and down buttons
        JButton prevButton = new JButton(upLabel);
        prevButton.setActionCommand(upCommand);
        // If it's a transpose command, publish without player data
        if (upCommand.equals(Commands.TRANSPOSE_UP) || upCommand.equals(Commands.TRANSPOSE_DOWN)) {
            prevButton.addActionListener(e -> CommandBus.getInstance().publish(e.getActionCommand(), this, null));
        } else {
            // Original code path for other commands
            prevButton.addActionListener(e -> CommandBus.getInstance().publish(e.getActionCommand(), this,
                    PlayerManager.getInstance().getActivePlayer()));
        }

        // Similar change for the down button
        JButton nextButton = new JButton(downLabel);
        nextButton.setActionCommand(downCommand);
        if (downCommand.equals(Commands.TRANSPOSE_UP) || downCommand.equals(Commands.TRANSPOSE_DOWN)) {
            nextButton.addActionListener(e -> CommandBus.getInstance().publish(e.getActionCommand(), this, null));
        } else {
            nextButton.addActionListener(e -> CommandBus.getInstance().publish(e.getActionCommand(), this,
                    PlayerManager.getInstance().getActivePlayer()));
        }

        // Enable/disable buttons based on player selection
        prevButton.setEnabled(true);
        nextButton.setEnabled(true);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);

        navPanel.add(octaveLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);

        return navPanel;
    }

    /**
     * Helper method to find components by type in a container hierarchy and apply an action
     * 
     * @param <T> The type of component to find
     * @param container The container to search in
     * @param componentClass The class of the component type to find
     * @param action The action to perform on each found component
     */
    public <T extends Component> void findComponentsByType(Container container,
            Class<T> componentClass, Consumer<Component> action) {

        // Check all components in the container
        for (Component component : container.getComponents()) {
            // If component matches the requested class, apply the action
            if (componentClass.isAssignableFrom(component.getClass())) {
                action.accept(component);
            }

            // If component is itself a container, recursively search it
            if (component instanceof Container) {
                findComponentsByType((Container) component, componentClass, action);
            }
        }
    }

    /**
     * Check if a component is a child (direct or indirect) of a container
     * 
     * @param child The component to check
     * @param parent The potential parent container
     * @return true if child is contained within parent's hierarchy
     */
    public boolean isChildOf(Component child, Container parent) {
        // Check if the component's parent is the target container
        Container current = child.getParent();
        while (current != null) {
            if (current == parent) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Get the first parent of a specific type
     * 
     * @param <T> The type of parent to find
     * @param component The component to start from
     * @param parentClass The class of the parent to find
     * @return The first parent of the specified type, or null if not found
     */
    public <T extends Container> T getParentOfType(Component component, Class<T> parentClass) {
        Container current = component.getParent();
        while (current != null) {
            if (parentClass.isAssignableFrom(current.getClass())) {
                return parentClass.cast(current);
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Find all components of a specific type in a container
     * 
     * @param <T> The type of component to find
     * @param container The container to search in
     * @param componentClass The class of components to find
     * @return List of components matching the specified type
     */
    public <T extends Component> List<T> findAllComponentsOfType(Container container, Class<T> componentClass) {
        List<T> result = new ArrayList<>();
        
        findComponentsByType(container, componentClass, component -> {
            result.add(componentClass.cast(component));
        });
        
        return result;
    }
}
