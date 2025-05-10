package com.angrysurfer.beats.util;

import com.angrysurfer.core.util.ErrorHandler;
import com.angrysurfer.core.util.ErrorHandler.ErrorEvent;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * UI-specific error handler that shows dialogs for errors
 */
public class UIErrorHandler {
    private static Component parentComponent = null;
    
    /**
     * Initialize the UI error handler
     * @param parent The parent component for dialogs
     */
    public static void initialize(Component parent) {
        parentComponent = parent;
        
        // Register as a listener for errors from the core module
        ErrorHandler.addErrorListener(UIErrorHandler::handleErrorEvent);
    }
    
    /**
     * Handle an error event from the core module
     */
    private static void handleErrorEvent(ErrorEvent event) {
        // Use invokeLater to ensure UI updates happen on EDT
        SwingUtilities.invokeLater(() -> {
            // Construct message
            String message = event.getMessage();
            String title = "Error: " + event.getSource();
            
            // If there's an exception, show a more detailed dialog
            if (event.getError() != null) {
                showDetailedErrorDialog(title, message, event.getError());
            } else {
                JOptionPane.showMessageDialog(
                    parentComponent, 
                    message,
                    title, 
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
    
    /**
     * Show a simple error message dialog
     */
    public static void showError(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                parentComponent, 
                message,
                title, 
                JOptionPane.ERROR_MESSAGE
            );
        });
    }
    
    /**
     * Show a detailed error dialog with stack trace
     */
    public static void showDetailedErrorDialog(String title, String message, Throwable error) {
        SwingUtilities.invokeLater(() -> {
            // Create a text area for the stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            error.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            // Create a dialog with "Details" button
            String[] options = {"OK", "Show Details"};
            int result = JOptionPane.showOptionDialog(
                parentComponent,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]
            );
            
            // If "Details" clicked, show stack trace
            if (result == 1) {
                JTextArea textArea = new JTextArea(20, 50);
                textArea.setText(stackTrace);
                textArea.setEditable(false);
                textArea.setCaretPosition(0);
                
                JScrollPane scrollPane = new JScrollPane(textArea);
                
                JOptionPane.showMessageDialog(
                    parentComponent,
                    scrollPane,
                    "Error Details",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
    }
}