package com.angrysurfer.core.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogManager {
    private static final LogManager INSTANCE = new LogManager();
    private static final Logger logger = Logger.getLogger("BeatGenerator");

    private LogManager() {
        // Private constructor to enforce singleton
    }

    public static LogManager getInstance() {
        return INSTANCE;
    }

    public void debug(String source, String message) {
        logger.log(Level.FINE, "[" + source + "] " + message);
    }

    public void info(String source, String message) {
        logger.log(Level.INFO, "[" + source + "] " + message);
    }

    public void warn(String source, String message) {
        logger.log(Level.WARNING, "[" + source + "] " + message);
    }

    public void error(String source, String message, Throwable e) {
        logger.log(Level.SEVERE, "[" + source + "] " + message, e);
    }

    public void error(String source, String message) {
        logger.log(Level.SEVERE, "[" + source + "] " + message);
    }
}
