package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RulesPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(RulesPanel.class.getName());
    private final JTable table;
    private final StatusConsumer status;

    public RulesPanel(StatusConsumer status) {
        super(new BorderLayout());
        this.status = status;
        this.table = new JTable();
        setupTable();
        setupLayout();
        setupCommandBusListener();
    }

    private void setupLayout() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setupTable() {
        String[] columnNames = {"Operator", "Value"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void setupCommandBusListener() {
        CommandBus.getInstance().register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                switch (action.getCommand()) {
                    case Commands.TICKER_SELECTED -> clearRules();
                    case Commands.PLAYER_SELECTED -> {
                        if (action.getData() instanceof ProxyStrike player) {
                            loadRules(player);
                        } else {
                            clearRules();
                        }
                    }
                }
            }
        });
    }

    private void clearRules() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
    }

    public void loadRules(ProxyStrike player) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        if (player != null && player.getRules() != null) {
            for (ProxyRule rule : player.getRules()) {
                model.addRow(new Object[]{
                    rule.getOperator(),
                    rule.getValue()
                });
            }
        }
    }
}
