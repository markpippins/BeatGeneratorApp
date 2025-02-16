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
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.data.RedisService;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.service.InstrumentManager;
import com.formdev.flatlaf.FlatLightLaf;

public class App implements CommandListener {

    private static final Logger logger = Logger.getLogger(App.class.getName());

    private Frame frame;
    private static RedisService redisService;
    private static TickerManager tickerManager;

    public static void main(String[] args) {
        try {
            // Initialize services first
            initializeServices();
            // Then setup UI
            setupLookAndFeel();

            // Initialize InstrumentManager with Redis service
            InstrumentManager.getInstance().setMidiDeviceService(new RedisMidiDeviceService());

            SwingUtilities.invokeLater(() -> {
                try {
                    App app = new App();
                    app.createAndShowGUI();
                } catch (Exception e) {
                    logger.severe("Error initializing application: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.severe("Application startup error: " + e.getMessage());
        }
    }

    private void createAndShowGUI() {
        frame = new Frame();

        // Add logging for frame state loading
        logger.info("Loading frame state for window");
        FrameState state = redisService.loadFrameState();
        logger.info("Frame state loaded: " + (state != null));

        if (state != null) {
            frame.setSize(state.getFrameSizeX(), state.getFrameSizeY());
            frame.setLocation(state.getFramePosX(), state.getFramePosY());
            frame.setSelectedTab(state.getSelectedTab());
            logger.info("Applied frame state: " +
                    "size=" + state.getFrameSizeX() + "x" + state.getFrameSizeY() +
                    ", pos=" + state.getFramePosX() + "," + state.getFramePosY() +
                    ", tab=" + state.getSelectedTab());
        }

        // Add window listener for saving state on close
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveFrameState();
            }
        });

        setupFrame();
        frame.setVisible(true);
    }

    @Override
    public void onAction(Command action) { // This is now the correct method from our interface
        if (Commands.CHANGE_THEME.equals(action.getCommand())) {
            SwingUtilities.invokeLater(() -> {
                try {
                    saveFrameState();
                    String newLafClass = (String) action.getData();
                    UIManager.setLookAndFeel(newLafClass);
                    frame.close();
                    recreateFrame();
                } catch (Exception e) {
                    logger.severe("Error handling theme change: " + e.getMessage());
                }
            });
        }
    }

    private void saveFrameState() {
        try {
            FrameState currentState = new FrameState();
            currentState.setSelectedTab(frame.getSelectedTab());
            currentState.setFrameSizeX(frame.getWidth());
            currentState.setFrameSizeY(frame.getHeight());
            currentState.setFramePosX(frame.getX());
            currentState.setFramePosY(frame.getY());
            currentState.setLookAndFeelClassName(UIManager.getLookAndFeel().getClass().getName());

            logger.info("Saving frame state: " +
                    "size=" + frame.getWidth() + "x" + frame.getHeight() +
                    ", pos=" + frame.getX() + "," + frame.getY() +
                    ", tab=" + frame.getSelectedTab());

            redisService.saveFrameState(currentState);
        } catch (Exception e) {
            logger.severe("Error saving frame state: " + e.getMessage());
        }
    }

    private void recreateFrame() {
        frame = new Frame();
        FrameState state = redisService.loadFrameState();
        if (state != null) {
            frame.setSize(state.getFrameSizeX(), state.getFrameSizeY());
            frame.setLocation(state.getFramePosX(), state.getFramePosY());
            frame.setSelectedTab(state.getSelectedTab());
        }

        // Add window listener
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveFrameState();
            }
        });

        frame.pack();
        frame.setVisible(true);

        // Add a small delay before auto-selecting player to ensure UI is ready
        javax.swing.Timer timer = new javax.swing.Timer(500, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            setupFrame();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void setupFrame() {
        // After frame is created and visible, select first player if available
        SwingUtilities.invokeLater(() -> {
            try {
                // Get all strikes/players
                java.util.List<ProxyStrike> strikes = redisService.findAllStrikes();
                if (!strikes.isEmpty()) {
                    // Select the first player
                    Command selectCommand = new Command();
                    selectCommand.setCommand(Commands.PLAYER_SELECTED);
                    selectCommand.setData(strikes.get(0));
                    CommandBus.getInstance().publish(selectCommand);
                    logger.info("Auto-selected first player: " + strikes.get(0).getName());
                }
            } catch (Exception e) {
                logger.warning("Error auto-selecting first player: " + e.getMessage());
            }
        });
    }

    private static void setupLookAndFeel() {
        try {
            // Add logging
            logger.info("Loading frame state for Look and Feel");
            FrameState state = redisService.loadFrameState();
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
            redisService = new RedisService();
            logger.info("Redis service initialized");

            tickerManager = new TickerManager();
            logger.info("TickerManager instantiated");

            // Changed to check for instruments specifically
            List<ProxyInstrument> instruments = redisService.findAllInstruments();
            if (instruments.isEmpty()) {
                logger.info("No instruments found, loading initial configuration");
                String configPath = "config/beats-config.json";
                UserConfig config = redisService.loadConfigFromXml(configPath);
                redisService.saveConfig(config);
                logger.info("Initial configuration loaded");

                App.getRedisService().createDefaultTicker();
            } else {
                logger.info("Found " + instruments.size() + " existing instruments");
            }

            logger.info("Ticker manager initialized");
        } catch (Exception e) {
            logger.severe("Error initializing services: " + e.getMessage());
            throw new RuntimeException("Failed to initialize services", e);
        }
    }

    public static RedisService getRedisService() {
        if (redisService == null) {
            logger.severe("Redis service not initialized");
            throw new IllegalStateException("Redis service not initialized");
        }
        return redisService;
    }

    public static TickerManager getTickerManager() {
        if (tickerManager == null) {
            logger.severe("Ticker manager not initialized");
            throw new IllegalStateException("Ticker manager not initialized");
        }
        return tickerManager;
    }

    public App() {
        // Register for theme changes using the correct method
        CommandBus.getInstance().register(this);
    }
}
