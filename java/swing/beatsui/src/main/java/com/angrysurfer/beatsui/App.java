package com.angrysurfer.beatsui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.angrysurfer.beatsui.config.BeatsUIConfig;
import java.util.logging.Logger;

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
                redisService.loadConfigFromXml(configPath);
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
