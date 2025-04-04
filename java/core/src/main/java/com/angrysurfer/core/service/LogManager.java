package com.angrysurfer.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogManager {
    private static final LogManager INSTANCE = new LogManager();
    private static final Logger logger = LoggerFactory.getLogger("BeatGenerator");

    private LogManager() {
        // Private constructor to enforce singleton
    }

    public static LogManager getInstance() {
        return INSTANCE;
    }

    public void debug(String source, String message) {
        logger.debug("[{}] {}", source, message);
    }

    public void info(String source, String message) {
        logger.info("[{}] {}", source, message);
    }

    public void warn(String source, String message) {
        logger.warn("[{}] {}", source, message);
    }

    public void error(String source, String message, Throwable e) {
        logger.error("[{}] {}", source, message, e);
    }

    public void error(String source, String message) {
        logger.error("[{}] {}", source, message);
    }
}
