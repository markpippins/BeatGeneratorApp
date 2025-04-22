package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.panel.CustomControlsPanel;
import com.angrysurfer.beats.panel.EuclideanPatternPanel;
import com.angrysurfer.beats.panel.PlayerEditPanel;
import com.angrysurfer.beats.panel.RuleEditPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.config.UserConfigConverter;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;

public class DialogManager implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DialogManager.class.getName());

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
            case Commands.SHOW_MAX_LENGTH_DIALOG -> handleMaxLengthDialog((DrumSequencer) action.getData());
            case Commands.SHOW_EUCLIDEAN_DIALOG -> {
                if (action.getData() instanceof Object[] params) {
                    handleEuclideanDialog((DrumSequencer) params[0], (Integer) params[1]);
                }
            }
            case Commands.SHOW_FILL_DIALOG -> {
                if (action.getData() instanceof Object[] params) {
                    handleFillDialog((DrumSequencer) params[0], (Integer) params[1], (Integer) params[2]);
                }
            }
        }
    }

    private void handleAddPlayer() {
        logger.info("Starting handleAddPlayer");
        SwingUtilities.invokeLater(() -> {
            try {
                Session currentSession = SessionManager.getInstance().getActiveSession();

                logger.info(
                        String.format("Current session: %s", currentSession != null ? currentSession.getId() : "null"));

                if (currentSession != null) {
                    // Initialize player
                    Player newPlayer = PlayerManager.getInstance().initializeNewPlayer();
                    newPlayer.setName(newPlayer.getClass().getSimpleName() + " " + (currentSession.getPlayers().size() + 1));
                    logger.info(String.format("Created new player with ID: %d", newPlayer.getId()));

                    setNewPlayerInstrument(newPlayer);

                    // Create panel and dialog
                    PlayerEditPanel panel = new PlayerEditPanel(newPlayer);
                    Dialog<Player> dialog = frame.createDialog(newPlayer, panel);
                    dialog.setTitle("Add Player");

                    boolean result = dialog.showDialog();
                    logger.info(String.format("Dialog closed with result: %s", result));

                    if (result) {
                        Player updatedPlayer = panel.getUpdatedPlayer();
                        SessionManager.getInstance().addPlayerToSession(currentSession, updatedPlayer);
                        logger.info(String.format("Player %d added successfully", updatedPlayer.getId()));
                        CommandBus.getInstance().publish(Commands.PLAYER_ADDED, this, updatedPlayer);
                    }
                }
            } catch (Exception e) {
                logger.error("Error in handleAddPlayer: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void setNewPlayerInstrument(Player newPlayer) {
        try {
            // Get all instruments from the InstrumentManager
            List<InstrumentWrapper> instruments = com.angrysurfer.core.service.InstrumentManager.getInstance()
                    .getCachedInstruments();

            // Get list of available device names
            List<String> availableDeviceNames = com.angrysurfer.core.service.DeviceManager.getInstance()
                    .getAvailableOutputDeviceNames();

            logger.info("Setting instrument for new player. Available devices: " + availableDeviceNames);

            // Find the first instrument that has an available device
            InstrumentWrapper selectedInstrument = null;

            for (InstrumentWrapper instrument : instruments) {
                // Check if this instrument's device is available
                if (instrument != null &&
                        instrument.getDeviceName() != null &&
                        availableDeviceNames.contains(instrument.getDeviceName())) {

                    selectedInstrument = instrument;
                    logger.info("Found valid instrument: " + instrument.getName() + " with device: "
                            + instrument.getDeviceName());
                    break;
                }
            }

            // If no instrument with matching device was found, try to use the first
            // instrument
            if (selectedInstrument == null && !instruments.isEmpty()) {
                selectedInstrument = instruments.get(0);
                logger.error("No instrument with available device found. Using first instrument: " +
                        selectedInstrument.getName());
            }

            // Set the selected instrument
            if (selectedInstrument != null) {
                newPlayer.setInstrument(selectedInstrument);
                // Set default channel (usually channel 1, which is represented as 0 in MIDI)
                newPlayer.setChannel(0);
                logger.info("Set instrument for new player: " + selectedInstrument.getName());
            } else {
                logger.error("No instruments available. New player will have no instrument.");
            }
        } catch (Exception e) {
            logger.error("Error setting new player instrument: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEditPlayer(Player player) {
        if (player != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    PlayerEditPanel panel = new PlayerEditPanel(player);
                    Dialog<Player> dialog = frame.createDialog(player, panel);
                    dialog.setTitle("Edit Player: " + player.getName());

                    boolean result = dialog.showDialog();
                    
                    if (result) {
                        Player updatedPlayer = panel.getUpdatedPlayer();
                        
                        // First ensure player is still selected in PlayerManager
                        PlayerManager.getInstance().setActivePlayer(updatedPlayer);
                        
                        // Then publish events in correct order
                        commandBus.publish(Commands.SHOW_PLAYER_EDITOR_OK, this, updatedPlayer);
                        
                        // Important: This needs to come AFTER other updates
                        SwingUtilities.invokeLater(() -> {
                            // Delay selection event slightly to ensure other updates complete
                            commandBus.publish(Commands.PLAYER_SELECTED, this, updatedPlayer);
                        });
                    }
                } catch (Exception e) {
                    logger.error("Error in handleEditPlayer: " + e);
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
                            // Session session = redisService.findSessionForPlayer(refreshedPlayer);

                            // Re-select the player to update rules display
                            commandBus.publish(Commands.RULE_ADDED, this, player);
                            commandBus.publish(Commands.PLAYER_SELECTED, this, player);
                            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, player);
                            // commandBus.publish(Commands.SESSION_UPDATED, this, session);
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
                    Player player = PlayerManager.getInstance().getActivePlayer(); // redisService.findPlayerForRule(rule);
                    if (player != null) {
                        redisService.saveRule(updatedRule);
                        redisService.savePlayer(player);
                        // Get fresh state and re-select player
                        // Player refreshedPlayer = redisService.findPlayerById(player.getId());
                        commandBus.publish(Commands.RULE_EDITED, this, updatedRule);
                        // commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                        commandBus.publish(Commands.PLAYER_SELECTED, this, player);
                        // commandBus.publish(Commands.RULE_SELECTED, this, updatedRule);
                        CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, player);
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
                    CustomControlsPanel controlsPanel = new CustomControlsPanel(false);

                    // Pre-select the player's instrument
                    controlsPanel.selectInstrument(player.getInstrument());
                    // Create dialog using Frame's createDialog method
                    Dialog<Player> dialog = frame.createDialog(player, controlsPanel);
                    dialog.setTitle("Controls - " + player.getName() + " (" + player.getInstrument().getName() + ")");

                    // Show dialog
                    dialog.setResizable(true);

                    // Delay the refresh until after dialog is visible
                    SwingUtilities.invokeLater(() -> {
                        controlsPanel.refreshControlsPanel();
                    });
                    dialog.showDialog();

                    logger.info("Showing controls dialog for player: " + player.getName() +
                            " with instrument: " + player.getInstrument().getName());
                } catch (Exception e) {
                    logger.error("Error showing controls dialog: " + e.getMessage());
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
                for (InstrumentWrapper instrument : config.getInstruments()) {
                    logger.info("Saving instrument: " + instrument.getName());
                    redisService.saveInstrument(instrument);
                }

                // Save the entire config
                redisService.saveConfig(config);

                // Verify the save
                List<InstrumentWrapper> savedInstruments = redisService.findAllInstruments();
                logger.info("Found " + savedInstruments.size() + " instruments in Redis after save");

                // Refresh the UI
                // refreshInstrumentsTable();
                // setStatus("Database updated successfully from " + filePath);
            } catch (Exception e) {
                logger.error("Error loading and saving database: " + e.getMessage());
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
                List<InstrumentWrapper> instruments = redisService.findAllInstruments();
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
                logger.error("Error saving configuration: " + e.getMessage());
                JOptionPane.showMessageDialog(frame,
                        "Error saving configuration: " + e.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleMaxLengthDialog(DrumSequencer sequencer) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(frame,
                    "Set Maximum Pattern Length",
                    java.awt.Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            JPanel dialogPanel = new JPanel(new BorderLayout());
            dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            spinnerPanel.add(new JLabel("Maximum Pattern Length:"));

            SpinnerNumberModel model = new SpinnerNumberModel(
                    sequencer.getDefaultPatternLength(),
                    1,
                    sequencer.getMaxPatternLength(),
                    1
            );
            JSpinner lengthSpinner = new JSpinner(model);
            spinnerPanel.add(lengthSpinner);

            dialogPanel.add(spinnerPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> dialog.dispose());

            JButton applyButton = new JButton("Apply");
            applyButton.addActionListener(e -> {
                int maxLength = (Integer) lengthSpinner.getValue();
                commandBus.publish(Commands.MAX_LENGTH_SELECTED, this, maxLength);
                dialog.dispose();
            });

            buttonPanel.add(cancelButton);
            buttonPanel.add(applyButton);
            dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setContentPane(dialogPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });
    }

    private void handleEuclideanDialog(DrumSequencer sequencer, int drumIndex) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(frame,
                    "Euclidean Pattern Generator",
                    java.awt.Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            JPanel dialogPanel = new JPanel(new BorderLayout());
            dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            EuclideanPatternPanel patternPanel = new EuclideanPatternPanel(false);

            int patternLength = sequencer.getPatternLength(drumIndex);
            patternPanel.getStepsDial().setValue(patternLength);
            patternPanel.getHitsDial().setValue(Math.max(1, patternLength / 4));
            patternPanel.getRotationDial().setValue(0);
            patternPanel.getWidthDial().setValue(0);

            dialogPanel.add(patternPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> dialog.dispose());

            JButton applyButton = new JButton("Apply Pattern");
            applyButton.addActionListener(e -> {
                boolean[] pattern = patternPanel.getPattern();
                Object[] result = new Object[] { drumIndex, pattern };
                commandBus.publish(Commands.EUCLIDEAN_PATTERN_SELECTED, this, result);
                dialog.dispose();
            });

            buttonPanel.add(cancelButton);
            buttonPanel.add(applyButton);
            dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setContentPane(dialogPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });
    }

    private void handleFillDialog(DrumSequencer sequencer, int drumIndex, int startStep) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(frame,
                    "Fill Pattern",
                    java.awt.Dialog.ModalityType.APPLICATION_MODAL);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 5, 5));

            ButtonGroup group = new ButtonGroup();
            JRadioButton allButton = new JRadioButton("Fill All", true);
            JRadioButton everyOtherButton = new JRadioButton("Every Other Step");
            JRadioButton every4thButton = new JRadioButton("Every 4th Step");
            JRadioButton decayButton = new JRadioButton("Velocity Decay");

            group.add(allButton);
            group.add(everyOtherButton);
            group.add(every4thButton);
            group.add(decayButton);

            optionsPanel.add(allButton);
            optionsPanel.add(everyOtherButton);
            optionsPanel.add(every4thButton);
            optionsPanel.add(decayButton);

            panel.add(optionsPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> dialog.dispose());

            JButton applyButton = new JButton("Apply");
            applyButton.addActionListener(e -> {
                String fillType = "all";
                if (everyOtherButton.isSelected()) fillType = "everyOther";
                else if (every4thButton.isSelected()) fillType = "every4th";
                else if (decayButton.isSelected()) fillType = "decay";

                Object[] result = new Object[] { drumIndex, startStep, fillType };
                commandBus.publish(Commands.FILL_PATTERN_SELECTED, this, result);
                dialog.dispose();
            });

            buttonPanel.add(cancelButton);
            buttonPanel.add(applyButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setContentPane(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });
    }
}
