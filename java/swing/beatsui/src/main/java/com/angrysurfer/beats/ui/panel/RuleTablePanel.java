package com.angrysurfer.beats.ui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import com.angrysurfer.beats.App;
import com.angrysurfer.beats.ui.widget.Dialog;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.proxy.IProxyPlayer;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RuleTablePanel extends JPanel implements CommandListener {
    private static final Logger logger = Logger.getLogger(RuleTablePanel.class.getName());
    private final JTable table;
    private final StatusConsumer status;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JMenuItem editMenuItem;
    private final JMenuItem deleteMenuItem;
    private IProxyPlayer currentPlayer; // rename from selectedPlayer
    private JPopupMenu popup; // Add this field
    private JMenuItem addPopupMenuItem; // Add this field
    private final CommandBus actionBus = CommandBus.getInstance();

    public RuleTablePanel(StatusConsumer status) {
        super(new BorderLayout());
        this.status = status;
        this.table = new JTable();
        this.addButton = new JButton("Add");
        this.editButton = new JButton("Edit");
        this.deleteButton = new JButton("Delete");
        this.editMenuItem = new JMenuItem("Edit...");
        this.deleteMenuItem = new JMenuItem("Delete");
        
        actionBus.register(this); // Register for events
        setupComponents();
    }

    @Override
    public void onAction(Command action) {
        if (action == null) {
            logger.warning("Received null action");
            return;
        }

        logger.info("RuleTablePanel received action: " + action.getCommand());
        
        switch (action.getCommand()) {
            case Commands.PLAYER_SELECTED -> {
                IProxyPlayer player = (IProxyPlayer) action.getData();
                if (player != null) {
                    logger.info("Processing PLAYER_SELECTED for: " + player.getName());
                    currentPlayer = player;
                    refreshRuleList(player);
                } else {
                    logger.warning("PLAYER_SELECTED event had null player data");
                }
            }
            case Commands.PLAYER_UNSELECTED -> {
                logger.info("Processing PLAYER_UNSELECTED");
                currentPlayer = null;
                clearRules();
            }
            case Commands.RULE_ADDED_TO_PLAYER, Commands.RULE_REMOVED_FROM_PLAYER -> {
                if (currentPlayer != null) {
                    refreshRuleList(currentPlayer);
                }
            }
        }
    }

    private void updateButtonStates(boolean enabled) {
        addButton.setEnabled(enabled);
        editButton.setEnabled(enabled && table.getSelectedRow() >= 0);
        deleteButton.setEnabled(enabled && table.getSelectedRow() >= 0);
    }

    private void handlePlayerSelected(ProxyStrike player) {
        logger.info("Player selected: " + player.getName() + " (ID: " + player.getId() + ")");
        this.currentPlayer = player;
        addButton.setEnabled(true);
        addPopupMenuItem.setEnabled(true);
        
        // Clear and load rules
        clearTable();
        loadRulesFromRedis();
    }

    private void handlePlayerUnselected() {
        this.currentPlayer = null;
        clearTable();
        addButton.setEnabled(false);
        addPopupMenuItem.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        status.setStatus("No player selected");
    }

    private void clearTable() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
    }

    private void setupLayout() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        
        // Set minimum width for button panel
        int buttonPanelWidth = 240;
        buttonPanel.setPreferredSize(new Dimension(buttonPanelWidth, buttonPanel.getPreferredSize().height));
        buttonPanel.setMinimumSize(new Dimension(buttonPanelWidth, buttonPanel.getPreferredSize().height));

        add(buttonPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Force panel to be as skinny as the buttons
        setPreferredSize(new Dimension(buttonPanelWidth, 0));
        setMinimumSize(new Dimension(buttonPanelWidth, 0));
        setMaximumSize(new Dimension(buttonPanelWidth, Integer.MAX_VALUE));
    }

    private void setupButtons() {
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        addButton.setEnabled(false); // Initially disabled until player is selected

        addButton.addActionListener(e -> showRuleDialog(null));
        editButton.addActionListener(e -> editSelectedRule());
        deleteButton.addActionListener(e -> deleteSelectedRule());
    }

    private void setupPopupMenu() {
        popup = new JPopupMenu();
        addPopupMenuItem = new JMenuItem("Add...");
        
        addPopupMenuItem.addActionListener(e -> showRuleDialog(null));
        editMenuItem.addActionListener(e -> editSelectedRule());
        deleteMenuItem.addActionListener(e -> deleteSelectedRule());

        popup.add(addPopupMenuItem);
        popup.add(editMenuItem);
        popup.add(deleteMenuItem);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    // Update enabled state before showing
                    boolean hasSelection = table.getSelectedRow() >= 0;
                    editMenuItem.setEnabled(hasSelection);
                    deleteMenuItem.setEnabled(hasSelection);
                    addPopupMenuItem.setEnabled(currentPlayer != null);
                    
                    popup.show(table, e.getX(), e.getY());
                }
            }
        });
    }

    private void setupTable() {
        String[] ruleColumns = { "Operator", "Comparison", "Value", "Part" };

        DefaultTableModel model = new DefaultTableModel(ruleColumns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0:
                    case 1: return String.class;
                    case 2: return Double.class;
                    case 3: return Integer.class;
                    default: return Object.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        // Setup combo boxes and spinners
        JComboBox<String> operatorCombo = new JComboBox<>(ProxyRule.OPERATORS);
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(operatorCombo));

        JComboBox<String> comparisonCombo = new JComboBox<>(ProxyRule.COMPARISONS);
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(comparisonCombo));

        setupValueSpinner();
        setupPartSpinner();

        // Column widths
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(40);

        // Fix the cell renderer implementation
        DefaultTableCellRenderer partRenderer = new DefaultTableCellRenderer();
        partRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Integer && ((Integer)value) == ProxyRule.ALL_PARTS) {
                    setText("All");
                }
                setHorizontalAlignment(JLabel.CENTER);
                return c;
            }
        });

        // Single selection listener that updates everything
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() >= 0;
                boolean hasPlayer = currentPlayer != null;

                // Update button states
                editButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
                addButton.setEnabled(hasPlayer);

                if (hasSelection) {
                    int row = table.getSelectedRow();
                    String operator = (String) table.getValueAt(row, 0);
                    String comparison = (String) table.getValueAt(row, 1);
                    Object value = table.getValueAt(row, 2);
                    status.setStatus(String.format("Selected rule: %s %s %s", operator, comparison, value));
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedRule();
                }
            }
        });
    }

    private void loadRulesFromRedis() {
        try {
            if (currentPlayer == null || currentPlayer.getId() == null) {
                logger.warning("Cannot load rules - player is null or has no ID");
                return;
            }

            List<ProxyRule> rules = App.getRedisService().findRulesByPlayer(currentPlayer);
            logger.info("Found " + rules.size() + " rules for player: " + currentPlayer.getName());
            
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            rules.forEach(rule -> {
                Object[] rowData = rule.toRow();
                model.addRow(rowData);
            });
            
            // Select the first row if there are any rules
            if (model.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
                
                // Enable edit/delete buttons
                editButton.setEnabled(true);
                deleteButton.setEnabled(true);
                editMenuItem.setEnabled(true);
                deleteMenuItem.setEnabled(true);
            }
            
            status.setStatus("Loaded " + rules.size() + " rules for " + currentPlayer.getName());
        } catch (Exception e) {
            logger.severe("Error loading rules: " + e.getMessage());
            status.setStatus("Error loading rules: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupValueSpinner() {
        JSpinner valueSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, null, 1.0));
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JTextField()) {
            private final JSpinner spinner = valueSpinner;

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                spinner.setValue(value == null ? 0.0 : value);
                status.setStatus("Set value at row " + row);
                return spinner;
            }

            @Override
            public Object getCellEditorValue() {
                return spinner.getValue();
            }
        });
    }

    private void setupPartSpinner() {
        JSpinner partSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 16, 1));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(partSpinner);
        partSpinner.setEditor(editor);

        // Add custom formatter to show "All" for 0
        JFormattedTextField ftf = ((JSpinner.DefaultEditor) partSpinner.getEditor()).getTextField();
        ftf.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter() {
            @Override
            public String valueToString(Object value) throws ParseException {
                if (value instanceof Integer && ((Integer)value) == ProxyRule.ALL_PARTS) {
                    return "All";
                }
                return super.valueToString(value);
            }
            
            @Override
            public Object stringToValue(String text) throws ParseException {
                if ("All".equalsIgnoreCase(text)) {
                    return ProxyRule.ALL_PARTS;
                }
                return super.stringToValue(text);
            }
        }));

        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JTextField()) {
            private final JSpinner spinner = partSpinner;

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                spinner.setValue(value == null ? 0 : value);
                status.setStatus("Set value at row " + row);
                return spinner;
            }

            @Override
            public Object getCellEditorValue() {
                return spinner.getValue();
            }
        });
    }

    private void showRuleDialog(ProxyRule rule) {
        if (currentPlayer == null) {
            status.setStatus("No player selected");
            return;
        }

        if (rule == null) {
            rule = new ProxyRule();
        }

        RuleEditorPanel editorPanel = new RuleEditorPanel(rule);
        Dialog<ProxyRule> dialog = new Dialog<>(rule, editorPanel);
        dialog.setTitle(rule.getId() == null ? "Add Rule" : "Edit Rule");

        if (dialog.showDialog()) {
            ProxyRule updatedRule = editorPanel.getUpdatedRule();
            saveRuleToRedis(updatedRule);
            updateRuleTable(updatedRule, table.getSelectedRow());
        }
    }

    private void saveRuleToRedis(ProxyRule rule) {
        try {
            if (rule.getId() == null) {
                // Let Redis service handle ID generation
                ProxyRule savedRule = App.getRedisService().saveRule(rule, currentPlayer);
                // Update the rule with generated ID
                rule.setId(savedRule.getId());
                status.setStatus("Created new rule for player: " + currentPlayer.getName());
            } else {
                App.getRedisService().saveRule(rule, currentPlayer);
                status.setStatus("Updated rule for player: " + currentPlayer.getName());
            }
        } catch (Exception e) {
            logger.severe("Error saving rule: " + e.getMessage());
            status.setStatus("Error saving rule: " + e.getMessage());
        }
    }

    private void editSelectedRule() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            ProxyRule rule = getRuleFromRow(row);
            showRuleDialog(rule);
        }
    }

    private void deleteSelectedRule() {
        int row = table.getSelectedRow();
        if (row >= 0 && currentPlayer != null) {
            ProxyRule rule = getRuleFromRow(row);
            try {
                App.getRedisService().deleteRule(rule, currentPlayer);
                ((DefaultTableModel) table.getModel()).removeRow(row);
                status.setStatus("Deleted rule from player: " + currentPlayer.getName());
                
                // Publish rule removed event
                Command cmd = new Command();
                cmd.setCommand(Commands.RULE_REMOVED_FROM_PLAYER);
                cmd.setData(rule);
                cmd.setSender(this);
                actionBus.publish(cmd);
                
            } catch (Exception e) {
                logger.severe("Error deleting rule: " + e.getMessage());
                status.setStatus("Error deleting rule: " + e.getMessage());
            }
        }
    }

    private ProxyRule getRuleFromRow(int row) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        return ProxyRule.fromRow(new Object[] {
            model.getValueAt(row, 0),
            model.getValueAt(row, 1),
            model.getValueAt(row, 2),
            model.getValueAt(row, 3)
        });
    }

    private void updateRuleTable(ProxyRule rule, int selectedRow) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        Object[] rowData = rule.toRow();

        if (selectedRow >= 0) {
            for (int i = 0; i < rowData.length; i++) {
                model.setValueAt(rowData[i], selectedRow, i);
            }
        } else {
            model.addRow(rowData);
        }
    }

    private void refreshRuleList(IProxyPlayer player) {
        try {
            clearTable();
            if (player == null || player.getId() == null) {
                logger.warning("Cannot load rules - player is null or has no ID");
                return;
            }

            List<ProxyRule> rules = App.getRedisService().findRulesByPlayer(player);
            logger.info("Found " + rules.size() + " rules for player: " + player.getName());
            
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            rules.forEach(rule -> {
                Object[] rowData = rule.toRow();
                model.addRow(rowData);
            });
            
            // Select the first row if there are any rules
            if (model.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            }
            
            updateButtonStates(true);
            status.setStatus("Loaded " + rules.size() + " rules for " + player.getName());
            
        } catch (Exception e) {
            logger.severe("Error loading rules: " + e.getMessage());
            status.setStatus("Error loading rules: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearRules() {
        clearTable();
        updateButtonStates(false);
        status.setStatus("Rules cleared");
    }

    private void setupComponents() {
        setupTable();
        setupButtons();
        setupLayout();
        setupPopupMenu();
        
        // Initialize in disabled state
        updateButtonStates(false);
    }
}
