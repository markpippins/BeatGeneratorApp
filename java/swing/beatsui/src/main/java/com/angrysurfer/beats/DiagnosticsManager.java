package com.angrysurfer.beats;

import com.angrysurfer.beats.diagnostic.DetailedDiagnosticPanel;
import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.beats.diagnostic.RedisDiagnosticsHelper;

import javax.swing.*;

import java.awt.*;


/**
 * Utility class for showing dialogs
 */
public class DiagnosticsManager {

    /**
     * Show a diagnostic log dialog
     * 
     * @param parent The parent component
     * @param log    The diagnostic log builder
     */
    public static void showDiagnosticLogDialog(Component parent, DiagnosticLogBuilder log) {
        String logText = log.build();
        
        // If we have errors, also show an error dialog first
        if (log.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder("Diagnostics found errors:\n\n");
            int count = 0;
            for (String error : log.getErrors()) {
                if (count < 5) { // Show max 5 errors in the summary dialog
                    errorMessage.append("• ").append(error).append("\n");
                    count++;
                } else {
                    errorMessage.append("• ... and ").append(log.getErrors().size() - 5)
                              .append(" more errors\n");
                    break;
                }
            }
            errorMessage.append("\nSee detailed report for more information.");
            RedisDiagnosticsHelper.showError(log.getTitle(), errorMessage.toString());
        }
        
        // Use invokeLater to show the detailed log dialog after any other dialogs are dismissed
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(parent), log.getTitle() + " Report", false);
            DetailedDiagnosticPanel panel = new DetailedDiagnosticPanel(logText);
            
            dialog.setContentPane(panel);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(parent);
            
            // Set close action
            panel.setOnClose(() -> dialog.dispose());
            
            dialog.setVisible(true);
        });
    }

    /**
     * Show a standard error dialog
     * 
     * @param parent  The parent component
     * @param title   The dialog title
     * @param message The error message
     */
    public static void showErrorDialog(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show a standard info dialog
     * 
     * @param parent  The parent component
     * @param title   The dialog title
     * @param message The info message
     */
    public static void showInfoDialog(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Show a confirmation dialog
     * 
     * @param parent  The parent component
     * @param title   The dialog title
     * @param message The confirmation message
     * @return true if user confirmed, false otherwise
     */
    public static boolean showConfirmationDialog(Component parent, String title, String message) {
        int result = JOptionPane.showConfirmDialog(
                parent, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
}