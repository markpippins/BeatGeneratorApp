package com.angrysurfer.beats.service;

import java.awt.Dimension;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.angrysurfer.beats.Dialog;
import com.angrysurfer.beats.Frame;
import com.angrysurfer.beats.panel.ControlsPanel;
import com.angrysurfer.beats.panel.PlayerEditPanel;
import com.angrysurfer.beats.panel.RuleEditPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.config.UserConfigConverter;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;

public class DialogManager implements CommandListener {

    private static final Logger logger = Logger.getLogger(DialogManager.class.getName());

    private static DialogManager instance;
    private final CommandBus commandBus = CommandBus.getInstance();
    private final RedisService redisService = RedisService.getInstance();
    private final Frame frame;

    public static DialogManager initialize(Frame frame) {
        if (instance == null) {
            instance = new DialogManager(frame);
        }
        return instance;
    }

    private DialogManager(Frame frame) {
        this.frame = frame;
        commandBus.register(this);
    }

    @Override
    public void onAction(Command action) {
        logger.info("DialogManager received command: " + action.getCommand());
        switch (action.getCommand()) {
            case Commands.PLAYER_ADD_REQUEST -> handleAddPlayer();
            case Commands.PLAYER_EDIT_REQUEST -> handleEditPlayer((Player) action.getData());
            case Commands.RULE_ADD_REQUEST -> handleAddRule((Player) action.getData());
            case Commands.RULE_EDIT_REQUEST -> handleEditRule((Rule) action.getData());
            case Commands.EDIT_PLAYER_PARAMETERS -> handlePlayerParameters((Player) action.getData());
            case Commands.LOAD_CONFIG -> SwingUtilities.invokeLater(() -> showConfigFileChooserDialog());
            case Commands.SAVE_CONFIG -> SwingUtilities.invokeLater(() -> showConfigFileSaverDialog());
        }
    }

    private void handleAddPlayer() {
        logger.info("Starting handleAddPlayer");
        SwingUtilities.invokeLater(() -> {
            try {
                Ticker currentTicker = SessionManager.getInstance().getActiveTicker();

                logger.info(
                        String.format("Current ticker: %s", currentTicker != null ? currentTicker.getId() : "null"));

                if (currentTicker != null) {
                    // Initialize player
                    Player newPlayer = PlayerManager.getInstance().initializeNewPlayer();
                    logger.info(String.format("Created new player with ID: %d", newPlayer.getId()));

                    // Create panel and dialog
                    PlayerEditPanel panel = new PlayerEditPanel(newPlayer);
                    Dialog<Player> dialog = frame.createDialog(newPlayer, panel);
                    dialog.setTitle("Add Player");

                    boolean result = dialog.showDialog();
                    logger.info(String.format("Dialog closed with result: %s", result));

                    if (result) {
                        Player updatedPlayer = panel.getUpdatedPlayer();
                        SessionManager.getInstance().addPlayerToTicker(currentTicker, updatedPlayer);
                        logger.info(String.format("Player %d added successfully", updatedPlayer.getId()));
                        CommandBus.getInstance().publish(Commands.PLAYER_ADDED, this, updatedPlayer);
                    }
                }
            } catch (Exception e) {
                logger.severe("Error in handleAddPlayer: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleEditPlayer(Player player) {
        if (player != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    PlayerEditPanel panel = new PlayerEditPanel(player);
                    Dialog<Player> dialog = frame.createDialog(player, panel);
                    dialog.setTitle("Edit Player: " + player.getName());

                    logger.info("Showing edit player dialog");
                    boolean result = dialog.showDialog();
                    logger.info("Dialog result: " + result);

                    if (result)
                        commandBus.publish(Commands.SHOW_PLAYER_EDITOR_OK, this, panel.getUpdatedPlayer());

                } catch (Exception e) {
                    logger.severe("Error in handleEditPlayer: " + e);
                    e.printStackTrace();
                }
            });
        }
    }

    private void handleAddRule(Player player) {
        if (player != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Rule newRule = redisService.newRule();
                    RuleEditPanel panel = new RuleEditPanel(newRule);
                    Dialog<Rule> dialog = frame.createDialog(newRule, panel);
                    dialog.setTitle("Add Rule");

                    if (dialog.showDialog()) {
                        Rule updatedRule = panel.getUpdatedRule();
                        if (redisService.isValidNewRule(player, updatedRule)) {
                            redisService.addRuleToPlayer(player, updatedRule);
                            // Get fresh state
                            // Player refreshedPlayer = redisService.findPlayerById(player.getId());
                            // Ticker ticker = redisService.findTickerForPlayer(refreshedPlayer);

                            // Re-select the player to update rules display
                            commandBus.publish(Commands.PLAYER_SELECTED, this, player);
                            commandBus.publish(Commands.RULE_ADDED, this, player);
                            // commandBus.publish(Commands.TICKER_UPDATED, this, ticker);
                        } else {
                            // ... existing error handling ...
                        }
                    }
                } catch (Exception e) {
                    // ... existing error handling ...
                }
            });
        }
    }

    private void handleEditRule(Rule rule) {
        if (rule != null) {
            SwingUtilities.invokeLater(() -> {
                RuleEditPanel panel = new RuleEditPanel(rule);
                Dialog<Rule> dialog = frame.createDialog(rule, panel);
                dialog.setTitle("Edit Rule");

                if (dialog.showDialog()) {
                    Rule updatedRule = panel.getUpdatedRule();
                    Player player = redisService.findPlayerForRule(rule);
                    if (player != null) {
                        redisService.saveRule(updatedRule);
                        // Get fresh state and re-select player
                        Player refreshedPlayer = redisService.findPlayerById(player.getId());
                        commandBus.publish(Commands.PLAYER_SELECTED, this, refreshedPlayer);
                    }
                }
            });
        }
    }

    private void handlePlayerParameters(Player player) {
        if (player != null && player.getInstrument() != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    // Create controls panel with instrument context
                    ControlsPanel controlsPanel = new ControlsPanel();

                    // Pre-select the player's instrument
                    controlsPanel.selectInstrument(player.getInstrument());

                    // Create dialog using Frame's createDialog method
                    Dialog<Player> dialog = frame.createDialog(player, controlsPanel);
                    dialog.setTitle("Controls - " + player.getName() + " (" + player.getInstrument().getName() + ")");

                    // Show dialog
                    dialog.setResizable(true);
                    dialog.showDialog();
                    logger.info("Showing controls dialog for player: " + player.getName() +
                            " with instrument: " + player.getInstrument().getName());
                } catch (Exception e) {
                    logger.severe("Error showing controls dialog: " + e.getMessage());
                    JOptionPane.showMessageDialog(frame,
                            "Could not show controls: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        } else {
            JOptionPane.showMessageDialog(frame,
                    "No instrument assigned to player",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showConfigFileChooserDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Instruments JSON File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            logger.info("Selected file: " + filePath);
            try {
                RedisService redisService = RedisService.getInstance();

                // Load and validate the config
                UserConfig config = redisService.loadConfigFromJSON(filePath);
                if (config == null || config.getInstruments() == null || config.getInstruments().isEmpty()) {
                    // setStatus("Error: No instruments found in config file");
                    return;
                }

                // Log what we're about to save
                logger.info("Loaded " + config.getInstruments().size() + " instruments from file");

                // Save instruments to Redis
                for (Instrument instrument : config.getInstruments()) {
                    logger.info("Saving instrument: " + instrument.getName());
                    redisService.saveInstrument(instrument);
                }

                // Save the entire config
                redisService.saveConfig(config);

                // Verify the save
                List<Instrument> savedInstruments = redisService.findAllInstruments();
                logger.info("Found " + savedInstruments.size() + " instruments in Redis after save");

                // Refresh the UI
                // refreshInstrumentsTable();
                // setStatus("Database updated successfully from " + filePath);
            } catch (Exception e) {
                logger.severe("Error loading and saving database: " + e.getMessage());
                // setStatus("Error updating database: " + e.getMessage());
            }
        }
    }

    private void showConfigFileSaverDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Configuration File");

        // Add filters for both JSON and XML
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("JSON Files (*.json)", "json");
        FileNameExtensionFilter xmlFilter = new FileNameExtensionFilter("XML Files (*.xml)", "xml");
        fileChooser.addChoosableFileFilter(jsonFilter);
        fileChooser.addChoosableFileFilter(xmlFilter);
        fileChooser.setFileFilter(jsonFilter); // Default to JSON
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                FileNameExtensionFilter selectedFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();

                // Ensure proper file extension
                if (!filePath.toLowerCase().endsWith("." + selectedFilter.getExtensions()[0])) {
                    filePath += "." + selectedFilter.getExtensions()[0];
                }

                // Create UserConfig from current Redis state
                UserConfig config = new UserConfig();

                // Get instruments from Redis
                List<Instrument> instruments = redisService.findAllInstruments();
                config.setInstruments(instruments);
                logger.info("Found " + instruments.size() + " instruments to save");

                // Save based on selected format
                if (selectedFilter == jsonFilter) {
                    redisService.getObjectMapper().writerWithDefaultPrettyPrinter()
                            .writeValue(new File(filePath), config);
                } else {
                    // Use converter for XML
                    UserConfigConverter converter = new UserConfigConverter();
                    // First save as JSON
                    // String tempJson = redisService.getObjectMapper().writeValueAsString(config);
                    File tempFile = File.createTempFile("config", ".json");
                    redisService.getObjectMapper().writeValue(tempFile, config);
                    // Then convert to XML
                    converter.convertJsonToXml(tempFile.getPath(), filePath);
                    tempFile.delete();
                }

                logger.info("Configuration saved to: " + filePath);
                JOptionPane.showMessageDialog(frame,
                        "Configuration saved successfully",
                        "Save Complete",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                logger.severe("Error saving configuration: " + e.getMessage());
                JOptionPane.showMessageDialog(frame,
                        "Error saving configuration: " + e.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}
