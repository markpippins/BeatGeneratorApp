package com.angrysurfer.beats.diagnostic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building diagnostic logs
 */
public class DiagnosticLogBuilder {
    private final StringBuilder log = new StringBuilder();
    private final List<String> errors = new ArrayList<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String title;
    
    /**
     * Constructor with title
     * @param title The diagnostic title
     */
    public DiagnosticLogBuilder(String title) {
        this.title = title;
        // Add header with timestamp
        log.append("=== ").append(title).append(" ===\n");
        log.append("Date/Time: ").append(LocalDateTime.now().format(dateFormatter)).append("\n\n");
    }
    
    /**
     * Add a section header
     * @param sectionTitle The section title
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addSection(String sectionTitle) {
        log.append("\n").append(sectionTitle).append("\n");
        log.append("-".repeat(sectionTitle.length())).append("\n");
        return this;
    }
    
    /**
     * Add a line of text
     * @param text The text to add
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addLine(String text) {
        log.append(text).append("\n");
        return this;
    }
    
    /**
     * Add multiple lines of text
     * @param lines The lines to add
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addLines(String... lines) {
        for (String line : lines) {
            addLine(line);
        }
        return this;
    }
    
    /**
     * Add an indented line of text
     * @param text The text to add
     * @param indentLevel The indentation level (number of spaces = 3 * indentLevel)
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addIndentedLine(String text, int indentLevel) {
        log.append(" ".repeat(3 * indentLevel)).append(text).append("\n");
        return this;
    }
    
    /**
     * Add an error message to the log and error collection
     * @param errorMessage The error message
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addError(String errorMessage) {
        String message = "ERROR: " + errorMessage;
        log.append(message).append("\n");
        errors.add(errorMessage);
        return this;
    }
    
    /**
     * Add an exception to the log and error collection
     * @param e The exception
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addException(Exception e) {
        // Add the error message
        addError(e.getMessage());
        
        // Add the stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        log.append("Stack trace:\n").append(sw.toString()).append("\n");
        
        return this;
    }
    
    /**
     * Add a warning message to the log
     * @param warningMessage The warning message
     * @return This builder for chaining
     */
    public DiagnosticLogBuilder addWarning(String warningMessage) {
        log.append("WARNING: ").append(warningMessage).append("\n");
        return this;
    }
    
    /**
     * Check if the log contains any errors
     * @return True if errors are present
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Get all errors found during diagnostics
     * @return List of error messages
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * Build the final log text
     * @return The complete log as a string
     */
    public String build() {
        StringBuilder result = new StringBuilder(log);
        
        // Add summary of errors, if any
        if (!errors.isEmpty()) {
            result.append("\n=== ERROR SUMMARY ===\n");
            result.append("Found ").append(errors.size()).append(" errors:\n");
            for (int i = 0; i < errors.size(); i++) {
                result.append(i + 1).append(". ").append(errors.get(i)).append("\n");
            }
        } else {
            result.append("\n=== DIAGNOSTICS COMPLETED SUCCESSFULLY ===\n");
            result.append("No errors found.\n");
        }
        
        return result.toString();
    }
    
    /**
     * Get the diagnostic title
     * @return The title
     */
    public String getTitle() {
        return title;
    }
}