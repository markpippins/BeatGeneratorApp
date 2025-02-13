package com.angrysurfer.beatsui;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.angrysurfer.beatsui.api.Action;
import com.angrysurfer.beatsui.api.ActionBus;
import com.angrysurfer.beatsui.api.Commands;
import com.angrysurfer.beatsui.config.BeatsUIConfig;
import com.angrysurfer.beatsui.mock.Ticker;
import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Hello world!
 *
 */
public class App {
    private Frame frame;
    private static RedisService redisService;
    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                setupLookAndFeel();
                initializeRedis();

                App app = new App();
                app.frame = new Frame();
                app.frame.setLocationRelativeTo(null);
                app.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
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
                BeatsUIConfig config = redisService.loadConfigFromXml(configPath);
                redisService.saveConfig(config);
            }

            // Check for existing Ticker
            try {
                Ticker ticker = redisService.loadTicker();
                logger.info("Loaded existing ticker: BPM=" + ticker.getTempoInBPM() + 
                          ", Bars=" + ticker.getBars());
                
                // Publish ticker selection after a short delay
                SwingUtilities.invokeLater(() -> {
                    Action action = new Action();
                    action.setCommand(Commands.TICKER_SELECTED);
                    action.setData(ticker);
                    ActionBus.getInstance().publish(action);
                });

            } catch (Exception e) {
                logger.info("No existing ticker found, creating default ticker");
                Ticker newTicker = new Ticker();
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
            logger.severe("Error initializing Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static RedisService getRedisService() {
        return redisService;
    }
}
