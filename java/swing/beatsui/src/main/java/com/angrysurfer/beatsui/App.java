package com.angrysurfer.beatsui;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.angrysurfer.beatsui.api.Command;
import com.angrysurfer.beatsui.api.CommandBus;
import com.angrysurfer.beatsui.api.CommandListener;  // Changed import
import com.angrysurfer.beatsui.api.Commands;
import com.angrysurfer.beatsui.config.UserConfig;
import com.angrysurfer.beatsui.data.FrameState;
import com.angrysurfer.beatsui.data.RedisService;
import com.angrysurfer.beatsui.ui.Frame;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * Hello world!
 *
 */
public class App implements CommandListener {  // Changed to implement our ActionListener interface
    private Frame frame;
    private static RedisService redisService;
    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        try {
            initializeRedis();
            if (redisService != null) {
                setupLookAndFeel();
                SwingUtilities.invokeLater(() -> {
                    try {
                        App app = new App();
                        app.frame = new Frame();
                        
                        // Load and apply frame state
                        FrameState state = redisService.loadFrameState();
                        if (state != null) {
                            app.frame.setSize(state.frameSizeX, state.frameSizeY);
                            app.frame.setLocation(state.framePosX, state.framePosY);
                            app.frame.setSelectedTab(state.selectedTab);
                        }
                        
                        // Add window listener to save state before closing
                        app.frame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosing(java.awt.event.WindowEvent e) {
                                FrameState currentState = new FrameState(
                                    app.frame.getSelectedTab(),
                                    app.frame.getWidth(),
                                    app.frame.getHeight(),
                                    app.frame.getX(),
                                    app.frame.getY(),
                                    UIManager.getLookAndFeel().getClass().getName()
                                );
                                redisService.saveFrameState(currentState);
                            }
                        });

                        app.frame.pack();
                        if (state == null) {
                            app.frame.setLocationRelativeTo(null);
                        }
                        app.frame.setVisible(true);
                        
                        // Call setupFrame after frame is visible
                        app.setupFrame();
                        
                    } catch (Exception e) {
                        logger.severe("Error creating UI: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                logger.severe("Failed to initialize Redis service");
                System.exit(1);
            }
        } catch (Exception e) {
            logger.severe("Critical error during startup: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public App() {
        // Register for theme changes using the correct method
        CommandBus.getInstance().register(this);
    }

    @Override
    public void onAction(Command action) {  // This is now the correct method from our interface
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
        FrameState currentState = new FrameState(
            frame.getSelectedTab(),
            frame.getWidth(),
            frame.getHeight(),
            frame.getX(),
            frame.getY(),
            UIManager.getLookAndFeel().getClass().getName()
        );
        redisService.saveFrameState(currentState);
    }

    private void recreateFrame() {
        frame = new Frame();
        FrameState state = redisService.loadFrameState();
        if (state != null) {
            frame.setSize(state.frameSizeX, state.frameSizeY);
            frame.setLocation(state.framePosX, state.framePosY);
            frame.setSelectedTab(state.selectedTab);
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

    private static void initializeRedis() {
        try {
            logger.info("Initializing Redis service...");
            redisService = new RedisService();

            // Check if Redis is empty - only initialize if it is
            boolean isEmpty = redisService.isDatabaseEmpty();
            logger.info("Redis database is empty: " + isEmpty);

            if (isEmpty) {
                logger.info("Loading initial configuration...");
                String configPath = "C:/Users/MarkP/dev/BeatGeneratorApp/java/swing/beatsui/src/main/java/com/angrysurfer/beatsui/config/beats-config.json";
                UserConfig config = redisService.loadConfigFromXml(configPath);
                redisService.saveConfig(config);
            }

            // Check for existing Ticker
            try {
                ProxyTicker ticker = redisService.loadTicker();
                logger.info("Loaded existing ticker: BPM=" + ticker.getTempoInBPM() +
                        ", Bars=" + ticker.getBars());

                // Publish ticker selection after a short delay
                SwingUtilities.invokeLater(() -> {
                    Command action = new Command();
                    action.setCommand(Commands.TICKER_SELECTED);
                    action.setData(ticker);
                    CommandBus.getInstance().publish(action);
                });

            } catch (Exception e) {
                logger.info("No existing ticker found, creating default ticker");
                ProxyTicker newTicker = new ProxyTicker();
                // Set default values
                newTicker.setTempoInBPM(120.0f);
                newTicker.setBars(4);
                newTicker.setBeatsPerBar(4);
                newTicker.setTicksPerBeat(24);
                newTicker.setParts(1);
                newTicker.setPartLength(4L);
                redisService.saveTicker(newTicker);
                logger.info("Created and saved default ticker");
            }

        } catch (Exception e) {
            logger.severe("Fatal error initializing Redis: " + e.getMessage());
            e.printStackTrace();
            redisService = null;
        }
    }

    public static RedisService getRedisService() {
        return redisService;
    }
}
