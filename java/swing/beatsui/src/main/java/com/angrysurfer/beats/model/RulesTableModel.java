package com.angrysurfer.beats.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.table.AbstractTableModel;
import com.angrysurfer.core.proxy.ProxyRule;

public class RulesTableModel extends AbstractTableModel {
    private final List<ProxyRule> rules = new ArrayList<>();
    private final String[] columnNames = {"Operator", "Comparison", "Value", "Part"};

    public RulesTableModel(Set<ProxyRule> rules) {
        if (rules != null) {
            this.rules.addAll(rules);
        }
    }

    @Override
    public int getRowCount() {
        return rules.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ProxyRule rule = rules.get(rowIndex);
        Object[] row = rule.toRow();
        return row[columnIndex];
    }

    public ProxyRule getRuleAt(int row) {
        return rules.get(row);
    }
}
