//package com.angrysurfer.beats.util;
//
//import com.angrysurfer.beats.widget.Dial;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.MouseWheelEvent;
//import java.util.function.Consumer;
//
/// **
// * Utility class for handling mousewheel events across various UI components.
// * Centralizes mousewheel handling to reduce code duplication and ensure
// * consistent behavior across different panels.
// */
//public class MouseWheelUtil {
//
//    /**
//     * Handle mousewheel events for a JComboBox
//     *
//     * @param comboBox       The combo box to adjust
//     * @param scrollDirection The direction of scrolling (-1 for up, 1 for down)
//     * @return true if the value was changed, false otherwise
//     */
//    public static boolean handleComboBoxWheel(JComboBox<?> comboBox, int scrollDirection) {
//        if (comboBox == null) return false;
//
//        int currentIndex = comboBox.getSelectedIndex();
//        int newIndex = currentIndex + scrollDirection;
//        newIndex = Math.max(0, Math.min(newIndex, comboBox.getItemCount() - 1));
//
//        if (newIndex != currentIndex) {
//            comboBox.setSelectedIndex(newIndex);
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * Handle mousewheel events for a JSpinner
//     *
//     * @param spinner        The spinner to adjust
//     * @param scrollDirection The direction of scrolling (-1 for up, 1 for down)
//     * @return true if the value was changed, false otherwise
//     */
//    public static boolean handleSpinnerWheel(JSpinner spinner, int scrollDirection) {
//        if (spinner == null) return false;
//
//        try {
//            // Check if this is a number spinner
//            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
//
//            Number currentValue = model.getNumber();
//            Number step = model.getStepSize();
//            Number minValue = model.getMinimum();
//            Number maxValue = model.getMaximum();
//
//            // Calculate new value based on number type
//            Number newValue;
//
//            if (currentValue instanceof Integer) {
//                int current = currentValue.intValue();
//                int stepSize = step.intValue();
//                newValue = current + (scrollDirection * stepSize);
//
//                if (minValue != null && newValue.intValue() < minValue.intValue()) {
//                    newValue = minValue;
//                }
//
//                if (maxValue != null && newValue.intValue() > maxValue.intValue()) {
//                    newValue = maxValue;
//                }
//            } else if (currentValue instanceof Double || currentValue instanceof Float) {
//                double current = currentValue.doubleValue();
//                double stepSize = step.doubleValue();
//                newValue = current + (scrollDirection * stepSize);
//
//                if (minValue != null && newValue.doubleValue() < minValue.doubleValue()) {
//                    newValue = minValue;
//                }
//
//                if (maxValue != null && newValue.doubleValue() > maxValue.doubleValue()) {
//                    newValue = maxValue;
//                }
//            } else {
//                return false; // Unsupported spinner type
//            }
//
//            // Update if changed
//            if (!currentValue.equals(newValue)) {
//                spinner.setValue(newValue);
//                return true;
//            }
//
//        } catch (ClassCastException e) {
//            // Not a number spinner, try to handle other types
//            // For now, we only support number spinners
//        }
//
//        return false;
//    }
//
//    /**
//     * Handle mousewheel events for a JToggleButton
//     *
//     * @param toggleButton The toggle button to adjust
//     * @return true if the button was toggled
//     */
//    public static boolean handleToggleButtonWheel(JToggleButton toggleButton) {
//        if (toggleButton == null) return false;
//
//        toggleButton.doClick();
//        return true;
//    }
//
//    /**
//     * Handle mousewheel events for a Dial control
//     *
//     * @param dial           The dial to adjust
//     * @param scrollDirection The direction of scrolling (-1 for up, 1 for down)
//     * @return true if the value was changed, false otherwise
//     */
//    public static boolean handleDialWheel(Dial dial, int scrollDirection) {
//        if (dial == null) return false;
//
//        int currentValue = dial.getValue();
//        int newValue = currentValue + scrollDirection;
//
//        // Ensure within bounds
//        newValue = Math.max(dial.getMinimum(), Math.min(newValue, dial.getMaximum()));
//
//        // Update if changed
//        if (newValue != currentValue) {
//            dial.setValue(newValue);
//            return true;
//        }
//
//        return false;
//    }
//
//    /**
//     * Create a mousewheel listener that handles common component types
//     *
//     * @param panel The panel to add the listener to
//     */
//    public static void installWheelListener(JPanel panel) {
//        panel.addMouseWheelListener(e -> handlePanelMouseWheelEvent(panel, e));
//    }
//
//    /**
//     * Handle mousewheel events for a panel by checking focus and component under mouse
//     *
//     * @param panel The panel
//     * @param e The mousewheel event
//     */
//    private static void handlePanelMouseWheelEvent(JPanel panel, MouseWheelEvent e) {
//        // Get scroll direction (-1 for up, 1 for down)
//        int scrollDirection = e.getWheelRotation() > 0 ? -1 : 1;
//
//        // Get the component with focus
//        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
//
//        // Try to handle the focused component first
//        if (handleFocusedComponent(focusOwner, scrollDirection)) {
//            return;
//        }
//
//        // If no component has focus or the focused component wasn't handled,
//        // try the component under the mouse pointer
//        if (!(focusOwner instanceof JComponent)) {
//            Component componentAt = SwingUtilities.getDeepestComponentAt(panel, e.getX(), e.getY());
//            handleComponentUnderMouse(componentAt, scrollDirection);
//        }
//    }
//
//    /**
//     * Handle mousewheel events for a focused component
//     *
//     * @param focusOwner The component with focus
//     * @param scrollDirection The scroll direction
//     * @return true if the event was handled
//     */
//    private static boolean handleFocusedComponent(Component focusOwner, int scrollDirection) {
//        if (focusOwner instanceof JComboBox) {
//            return handleComboBoxWheel((JComboBox<?>) focusOwner, scrollDirection);
//        } else if (focusOwner instanceof JSpinner || focusOwner instanceof JSpinner.DefaultEditor) {
//            JSpinner spinner;
//            if (focusOwner instanceof JSpinner) {
//                spinner = (JSpinner) focusOwner;
//            } else {
//                // Get the spinner from its editor
//                spinner = ((JSpinner.DefaultEditor) focusOwner).getSpinner();
//            }
//            return handleSpinnerWheel(spinner, scrollDirection);
//        } else if (focusOwner instanceof JToggleButton) {
//            return handleToggleButtonWheel((JToggleButton) focusOwner);
//        } else if (focusOwner instanceof Dial) {
//            return handleDialWheel((Dial) focusOwner, scrollDirection);
//        }
//
//        return false;
//    }
//
//    /**
//     * Handle mousewheel events for a component under the mouse pointer
//     *
//     * @param component The component under the mouse
//     * @param scrollDirection The scroll direction
//     * @return true if the event was handled
//     */
//    private static boolean handleComponentUnderMouse(Component component, int scrollDirection) {
//        if (component instanceof JComboBox) {
//            return handleComboBoxWheel((JComboBox<?>) component, scrollDirection);
//        } else if (component instanceof JSpinner) {
//            return handleSpinnerWheel((JSpinner) component, scrollDirection);
//        } else if (component instanceof JToggleButton) {
//            return handleToggleButtonWheel((JToggleButton) component);
//        } else if (component instanceof Dial) {
//            return handleDialWheel((Dial) component, scrollDirection);
//        }
//
//        return false;
//    }
//}
