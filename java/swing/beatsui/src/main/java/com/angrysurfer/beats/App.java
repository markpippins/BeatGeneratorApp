package com.angrysurfer.beats;

import java.util.List;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.angrysurfer.beats.service.RedisMidiDeviceService;
import com.angrysurfer.beats.service.TickerManager;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.data.RedisService;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.service.InstrumentManager;
import com.formdev.flatlaf.FlatLightLaf;

public class App implements CommandListener {

    private static final Logger logger = Logger.getLogger(App.class.getName());

    private static final CommandBus commandBus = CommandBus.getInstance();
    private static final RedisService redisService = RedisService.getInstance();
    private static final TickerManager tickerManager = TickerManager.getInstance();

    private Frame frame;

    public static void main(String[] args) {
        // Configure logging first
        System.setProperty("java.util.logging.config.file", 
            "src/main/resources/logging.properties");
        
        try {
            logger.info("Starting application...");
            
            // Initialize services first
            logger.info("Initializing services...");
            initializeServices();
            
            // Then setup UI
            logger.info("Setting up Look and Feel...");
            setupLookAndFeel();

            // Initialize InstrumentManager with Redis service
            logger.info("Initializing InstrumentManager...");
            InstrumentManager.getInstance().setMidiDeviceService(new RedisMidiDeviceService());

            SwingUtilities.invokeLater(() -> {
                try {
                    logger.info("Creating main application window...");
                    App app = new App();
                    app.createAndShowGUI();
                    logger.info("Application started successfully");
                } catch (Exception e) {
                    logger.severe("Error creating application window: " + e);
                    e.printStackTrace();
                    System.exit(1);
                }
            });
        } catch (Exception e) {
            logger.severe("Fatal error during application startup: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void createAndShowGUI() {
        frame = new Frame();
        frame.loadFrameState();
        frame.setVisible(true);
    }

    @Override
    public void onAction(Command action) {
        if (Commands.CHANGE_THEME.equals(action.getCommand())) {
            SwingUtilities.invokeLater(() -> {
                try {
                    frame.saveFrameState();
                    String newLafClass = (String) action.getData();
                    UIManager.setLookAndFeel(newLafClass);
                    frame.close();
                    frame = new Frame();
                    frame.loadFrameState();
                    frame.setVisible(true);
                } catch (Exception e) {
                    logger.severe("Error handling theme change: " + e.getMessage());
                }
            });
        }
    }

    private static void setupLookAndFeel() {
        try {
            // Add logging
            logger.info("Loading frame state for Look and Feel");
            FrameState state = redisService.getInstance().loadFrameState();
            logger.info("Frame state loaded: " + (state != null ? state.getLookAndFeelClassName() : "null"));

            if (state != null && state.getLookAndFeelClassName() != null) {
                UIManager.setLookAndFeel(state.getLookAndFeelClassName());
                logger.info("Set Look and Feel to: " + state.getLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                logger.info("Set default Look and Feel: FlatLightLaf");
            }
        } catch (Exception e) {
            logger.warning("Error setting look and feel: " + e.getMessage());
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception ex) {
                logger.severe("Error setting default look and feel: " + ex.getMessage());
            }
        }
    }

    private static void initializeServices() {
        try {
            // Get RedisService instance
            RedisService redisService = RedisService.getInstance();
            logger.info("Redis service initialized");

            // Check for instruments
            List<ProxyInstrument> instruments = redisService.findAllInstruments();
            if (instruments.isEmpty()) {
                logger.info("No instruments found, loading initial configuration");
                // String configPath = "config/beats-config.json";
                // UserConfig config = redisService.loadConfigFromXml(configPath);
                // redisService.saveConfig(config);
                logger.info("Initial configuration loaded");
            } else {
                logger.info("Found " + instruments.size() + " existing instruments");
            }
        } catch (Exception e) {
            logger.severe("Error initializing services: " + e.getMessage());
            throw new RuntimeException("Failed to initialize services", e);
        }
    }

    // public static RedisService getRedisService() {
    // return RedisService.getInstance();
    // }

    // public static TickerManager getTickerManager() {
    // if (tickerManager == null) {
    // logger.severe("Ticker manager not initialized");
    // throw new IllegalStateException("Ticker manager not initialized");
    // }
    // return tickerManager;
    // }

    public App() {
        // Register for theme changes using the correct method
        commandBus.register(this);
    }
}
