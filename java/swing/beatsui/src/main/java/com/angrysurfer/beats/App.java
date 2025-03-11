package com.angrysurfer.beats;

import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.BusListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.util.Constants;
import com.formdev.flatlaf.FlatLightLaf;

public class App implements BusListener {

    private static final Logger logger = Logger.getLogger(App.class.getName());

    private static final CommandBus commandBus = CommandBus.getInstance();
    private static final RedisService redisService = RedisService.getInstance();

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

            SwingUtilities.invokeLater(() -> {
                try {
                    logger.info("Creating main application window...");
                    App app = new App();
                    app.createAndShowGUI();
                    logger.info("Application started successfully");
                } catch (Exception e) {
                    handleInitializationFailure("Failed to create application window", e);
                }
            });
        } catch (Exception e) {
            handleInitializationFailure("Fatal error during application startup", e);
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
            FrameState state = RedisService.getInstance().loadFrameState(Constants.APPLICATION_FRAME);
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
            // Initialize RedisService first
            RedisService redisService = RedisService.getInstance();
            logger.info("Redis service initialized");

            // Initialize managers in correct order
            // UserConfigurationEngine.getInstance();
            // if (UserConfigurationEngine.getInstance().getCurrentConfig() == null) {
            // logger.warning("No user configuration found, creating default
            // configuration");
            // UserConfigurationEngine.getInstance().setCurrentConfig(new UserConfig());
            // }
            // logger.info("User configuration manager initialized");

            SessionManager.getInstance().initialize();
            logger.info("Session manager initialized");

            // Initialize SessionManager before any UI components

            // SessionManager sessionManager = SessionManager.getInstance();
            // logger.info("Session manager initialized");

            // // Initialize instrument management after session
            // RedisInstrumentHelper instrumentHelper = redisService.getInstrumentHelper();
            // // InstrumentEngine instrumentManager =
            // // InstrumentEngine.getInstance(instrumentHelper);

            // // Verify instrument cache initialization
            // List<Instrument> instruments = instrumentHelper.findAllInstruments();
            // logger.info("Found " + instruments.size() + " instruments in Redis");
            // // instrumentManager.refreshCache(); // Ensure cache is populated

            // Signal system ready
            CommandBus.getInstance().publish(Commands.SYSTEM_READY, App.class, null);
            logger.info("System initialization complete");

        } catch (Exception e) {
            handleInitializationFailure("Failed to initialize services", e);
        }
    }

    private static void handleInitializationFailure(String errorMessage, Exception e) {
        logger.severe("Critical initialization error: " + errorMessage);
        logger.severe("Exception: " + e.getMessage());
        e.printStackTrace();

        SwingUtilities.invokeLater(() -> {
            String fullMessage = String.format("""
                    Failed to initialize application: %s

                    Error details: %s

                    Please ensure Redis is running and try again.

                    The application will now exit.""",
                    errorMessage, e.getMessage());

            JOptionPane.showMessageDialog(null,
                    fullMessage,
                    "Initialization Error",
                    JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        });
    }

    public App() {
        // Register for theme changes using the correct method
        commandBus.register(this);
    }
}
