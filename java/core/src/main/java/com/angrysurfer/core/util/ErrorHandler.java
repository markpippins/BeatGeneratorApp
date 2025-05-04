package com.angrysurfer.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Non-UI error handler for core modules
 */
public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    
    // Error listeners for propagating errors to UI
    private static final List<Consumer<ErrorEvent>> errorListeners = new ArrayList<>();
    
    /**
     * Log an error without showing UI dialogs
     */
    public static void logError(String source, String message) {
        logger.error("[{}] {}", source, message);
        notifyListeners(new ErrorEvent(source, message, null));
    }
    
    /**
     * Log an error with exception details but without showing UI dialogs
     */
    public static void logError(String source, String message, Throwable error) {
        logger.error("[{}] {}", source, message, error);
        notifyListeners(new ErrorEvent(source, message, error));
    }
    
    /**
     * Log a warning without showing UI dialogs
     */
    public static void logWarning(String source, String message) {
        logger.warn("[{}] {}", source, message);
    }
    
    /**
     * Add a listener that will be notified of errors
     * UI components can register to be notified when errors occur
     */
    public static void addErrorListener(Consumer<ErrorEvent> listener) {
        if (listener != null) {
            errorListeners.add(listener);
        }
    }
    
    /**
     * Remove an error listener
     */
    public static void removeErrorListener(Consumer<ErrorEvent> listener) {
        if (listener != null) {
            errorListeners.remove(listener);
        }
    }
    
    /**
     * Notify all listeners about an error
     */
    private static void notifyListeners(ErrorEvent event) {
        for (Consumer<ErrorEvent> listener : errorListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.error("Error in error listener", e);
            }
        }
    }
    
    /**
     * Error event class for passing error details
     */
    public static class ErrorEvent {
        private final String source;
        private final String message;
        private final Throwable error;
        
        public ErrorEvent(String source, String message, Throwable error) {
            this.source = source;
            this.message = message;
            this.error = error;
        }
        
        public String getSource() {
            return source;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Throwable getError() {
            return error;
        }
        
        @Override
        public String toString() {
            return source + ": " + message;
        }
    }
}