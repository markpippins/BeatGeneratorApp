package com.angrysurfer.beats.panel.player;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.core.service.PlayerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;

/**
 * Panel for editing player properties using CommandBus pattern
 */
public class PlayerEditPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(PlayerEditPanel.class);
    
    // Player reference - treated as transient, always get fresh from PlayerManager
    private Player player;
    private Long playerId;
    
    // UI Components
    private PlayerEditBasicPropertiesPanel basicPropertiesPanel;
    private PlayerEditDetailPanel detailPanel;
    private SoundParametersPanel soundParametersPanel;
    private JTable rulesTable;
    
    // Services
    private final CommandBus commandBus = CommandBus.getInstance();
    private final PlayerManager playerManager = PlayerManager.getInstance();
    
    /**
     * Constructor
     */
    public PlayerEditPanel(Player player) {
        super(new BorderLayout());
        this.player = player;
        
        if (player != null) {
            this.playerId = player.getId();
        }
        
        initComponents();
        layoutComponents();
        registerForEvents();
    }
    
    /**
     * Register for command bus events
     */
    private void registerForEvents() {
        commandBus.register(this);
    }
    
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) return;
        
        // Only process events relevant to our player
        if (playerId == null) return;
        
        switch (action.getCommand()) {
            case Commands.PLAYER_UPDATED -> handlePlayerUpdated(action);
        }
    }
    
    /**
     * Handle player update events
     */
    private void handlePlayerUpdated(Command action) {
        if (action.getData() instanceof Player updatedPlayer && 
            playerId.equals(updatedPlayer.getId())) {
            
            // Update our player reference with fresh data
            SwingUtilities.invokeLater(() -> {
                player = updatedPlayer;
                updatePanels();
            });
        }
    }
    
    /**
     * Initialize UI components
     */
    private void initComponents() {
        // Create panels with player reference
        basicPropertiesPanel = new PlayerEditBasicPropertiesPanel(player);
        detailPanel = new PlayerEditDetailPanel(player);
        soundParametersPanel = new SoundParametersPanel(player);
        
        // Create tabbed pane for organization if it doesn't exist
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Basic info panel (contains existing fields)
        JPanel basicInfoPanel = new JPanel(new BorderLayout());
        basicInfoPanel.add(basicPropertiesPanel, BorderLayout.NORTH);
        tabbedPane.addTab("Basic Info", basicInfoPanel);
        
        // Rules panel
        JPanel rulesPanel = createRulesPanel();
        tabbedPane.addTab("Rules", rulesPanel);
        
        // Add tabbed pane to main container
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    /**
     * Layout components
     */
    private void layoutComponents() {
        // Create main panel with all components
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Add basic properties at top
        mainPanel.add(basicPropertiesPanel, BorderLayout.NORTH);
        
        // Add sound parameters panel below basics
        mainPanel.add(soundParametersPanel, BorderLayout.CENTER);
        
        // Add detail panel at bottom
        mainPanel.add(detailPanel, BorderLayout.SOUTH);
        
        // Add to this panel
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Update all panels with latest player data
     */
    private void updatePanels() {
        if (player == null) return;
        
        // Update each panel with fresh player data
        basicPropertiesPanel.updateFromPlayer(player);
        detailPanel.updateFromPlayer(player);
        soundParametersPanel.updateFromPlayer(player);
        refreshRulesTable();
    }
    
    /**
     * Returns the player with all current UI changes applied
     */
    public Player getUpdatedPlayer() {
        // Make sure we apply any pending changes from all panels
        applyAllChanges();
        
        // Get fresh player from manager to ensure we have latest data
        Player updatedPlayer = playerManager.getPlayerById(playerId);
        
        if (updatedPlayer != null && updatedPlayer.getInstrument() != null) {
            logger.debug("Returning player with instrument settings - Name: {}, Bank: {}, Preset: {}",
                updatedPlayer.getInstrument().getName(),
                updatedPlayer.getInstrument().getBankIndex(),
                updatedPlayer.getInstrument().getCurrentPreset());
        }
        
        // Get changes from rules table - enabled status might have changed
        updatePlayerRulesFromTable();
        
        return updatedPlayer;
    }
    
    /**
     * Apply all changes from UI components to player model
     */
    private void applyAllChanges() {
        // First apply changes in each panel
        basicPropertiesPanel.applyAllChanges();
        detailPanel.applyChanges();
        
        // Then request a player update through command bus
        commandBus.publish(Commands.PLAYER_UPDATE_REQUEST, this, player);
    }
    
    /**
     * Update from player
     */
    public void updateFromPlayer(Player newPlayer) {
        if (newPlayer == null) return;
        
        // Update our reference and ID
        this.player = newPlayer;
        this.playerId = newPlayer.getId();
        
        // Update all child panels
        updatePanels();
    }
    
    /**
     * Create panel for rules management
     */
    private JPanel createRulesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create table model for rules
        DefaultTableModel rulesModel = new DefaultTableModel(
            new Object[][]{}, 
            new String[]{"ID", "Type", "Value", "Target", "Enabled"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Only allow editing the "Enabled" column
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4) return Boolean.class;
                return Object.class;
            }
        };
        
        // Create table with model
        rulesTable = new JTable(rulesModel);
        rulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Set column widths
        rulesTable.getColumnModel().getColumn(0).setPreferredWidth(40);  // ID
        rulesTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Type
        rulesTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Value
        rulesTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Target
        rulesTable.getColumnModel().getColumn(4).setPreferredWidth(60);  // Enabled
        
        // Hide the ID column
        rulesTable.getColumnModel().getColumn(0).setMinWidth(0);
        rulesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        rulesTable.getColumnModel().getColumn(0).setWidth(0);
        
        // Add double-click listener for editing
        rulesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedRule();
                }
            }
        });
        
        // Add to scroll pane
        JScrollPane scrollPane = new JScrollPane(rulesTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton addButton = new JButton("Add Rule");
        addButton.addActionListener(e -> addNewRule());
        
        JButton editButton = new JButton("Edit");
        editButton.addActionListener(e -> editSelectedRule());
        
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteSelectedRule());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // If player has rules, populate the table
        populateRulesTable();
        
        return panel;
    }
    
    /**
     * Fill the rules table with data from the player
     */
    private void populateRulesTable() {
        if (player == null || rulesTable == null) return;
        
        DefaultTableModel model = (DefaultTableModel) rulesTable.getModel();
        model.setRowCount(0); // Clear existing rows
        
        if (player.getRules() != null) {
            for (Rule rule : player.getRules()) {
                model.addRow(new Object[]{
                    rule.getId(),
                    rule.getType(),
                    rule.getValue(),
                    rule.getTarget(),
                    rule.isEnabled()
                });
            }
        }
    }
    
    /**
     * Add a new rule to the player
     */
    private void addNewRule() {
        // This will be handled by DialogManager, we just need to trigger the event
        CommandBus.getInstance().publish(Commands.RULE_ADD_REQUEST, this, player);
    }
    
    /**
     * Edit the selected rule
     */
    private void editSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow >= 0) {
            Long ruleId = (Long) rulesTable.getValueAt(selectedRow, 0);
            
            // Find the rule in the player's rules
            Rule selectedRule = null;
            if (player.getRules() != null) {
                for (Rule rule : player.getRules()) {
                    if (rule.getId().equals(ruleId)) {
                        selectedRule = rule;
                        break;
                    }
                }
            }
            
            if (selectedRule != null) {
                CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
            }
        }
    }
    
    /**
     * Delete the selected rule
     */
    private void deleteSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow >= 0) {
            Long ruleId = (Long) rulesTable.getValueAt(selectedRow, 0);
            
            // Find the rule in the player's rules
            Rule selectedRule = null;
            if (player.getRules() != null) {
                for (Rule rule : player.getRules()) {
                    if (rule.getId().equals(ruleId)) {
                        selectedRule = rule;
                        break;
                    }
                }
            }
            
            // Confirm deletion
            int response = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this rule?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (response == JOptionPane.YES_OPTION && selectedRule != null) {
                // Remove from player
                player.getRules().remove(selectedRule);
                
                // Remove from table
                ((DefaultTableModel)rulesTable.getModel()).removeRow(selectedRow);
                
                // Notify about rule removal
                CommandBus.getInstance().publish(
                    Commands.RULE_REMOVED_FROM_PLAYER, 
                    this, 
                    new Object[]{player, selectedRule}
                );
            }
        }
    }
    
    /**
     * Update the rules table after changes
     */
    public void refreshRulesTable() {
        populateRulesTable();
    }
    
    /**
     * Update rule enabled status from the table
     */
    private void updatePlayerRulesFromTable() {
        if (player == null || player.getRules() == null || rulesTable == null) return;
        
        for (int i = 0; i < rulesTable.getRowCount(); i++) {
            Long ruleId = (Long) rulesTable.getValueAt(i, 0);
            Boolean enabled = (Boolean) rulesTable.getValueAt(i, 4);
            
            // Update the actual rule in the player
            for (Rule rule : player.getRules()) {
                if (rule.getId().equals(ruleId)) {
                    rule.setEnabled(enabled);
                    break;
                }
            }
        }
    }
}
