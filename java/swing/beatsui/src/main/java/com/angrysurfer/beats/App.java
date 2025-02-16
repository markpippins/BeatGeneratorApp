package com.angrysurfer.beats;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.angrysurfer.beats.service.RedisMidiDeviceService;
import com.angrysurfer.beats.service.TickerManager;
import com.angrysurfer.beats.ui.Frame;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.data.RedisService;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.service.InstrumentManager;
import com.formdev.flatlaf.FlatLightLaf;

public class App implements CommandListener {

    private Frame frame;
    private static RedisService redisService;
    private static TickerManager tickerManager;
    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        try {
            setupLookAndFeel();
            initializeServices();
            
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
        FrameState state = redisService.loadFrameState();
        if (state != null) {
            frame.setSize(state.getFrameSizeX(), state.getFrameSizeY());
            frame.setLocation(state.getFramePosX(), state.getFramePosY());
            frame.setSelectedTab(state.getSelectedTab());
        }
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
        FrameState currentState = new FrameState();
        currentState.setSelectedTab(frame.getSelectedTab());
        currentState.setFrameSizeX(frame.getWidth());
        currentState.setFrameSizeY(frame.getHeight());
        currentState.setFramePosX(frame.getX());
        currentState.setFramePosY(frame.getY());
        currentState.setLookAndFeelClassName(UIManager.getLookAndFeel().getClass().getName());
        redisService.saveFrameState(currentState);
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
            FrameState state = redisService.loadFrameState();
            if (state != null && state.getLookAndFeelClassName() != null) {
                UIManager.setLookAndFeel(state.getLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
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

            if (redisService.isDatabaseEmpty()) {
                String configPath = "config/beats-config.json";
                UserConfig config = redisService.loadConfigFromXml(configPath);
                redisService.saveConfig(config);
                logger.info("Initial configuration loaded");
            }

            tickerManager = new TickerManager();
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
