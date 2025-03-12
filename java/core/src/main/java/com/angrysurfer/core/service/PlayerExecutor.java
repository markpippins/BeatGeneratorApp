package com.angrysurfer.core.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerExecutor {
    private static final Logger logger = LoggerFactory.getLogger(PlayerExecutor.class);
    private static PlayerExecutor instance;
    
    // Use a fixed thread pool for MIDI operations
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    
    private PlayerExecutor() {}
    
    public static synchronized PlayerExecutor getInstance() {
        if (instance == null) {
            instance = new PlayerExecutor();
        }
        return instance;
    }
    
    public void submit(Runnable task) {
        executor.submit(task);
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}