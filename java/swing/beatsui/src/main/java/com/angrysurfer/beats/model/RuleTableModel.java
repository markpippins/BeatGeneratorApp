package com.angrysurfer.beats.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.table.AbstractTableModel;

import com.angrysurfer.core.model.Rule;

/**
 * Dedicated table model for handling Rule objects in a JTable
 */
public class RuleTableModel extends AbstractTableModel {
    private static final Logger logger = Logger.getLogger(RuleTableModel.class.getName());
    
    private final String[] columnNames = { "Comparison", "Operator", "Value", "Part" };
    private final List<Rule> rules = new ArrayList<>();
    
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
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rules.size()) {
            return null;
        }
        
        Rule rule = rules.get(rowIndex);
        
        return switch (columnIndex) {
            case 0 -> rule.getOperatorText();    // Property column - "Beat", "Tick", etc.
            case 1 -> rule.getComparisonText();  // Operator column - "==", "<", etc.
            case 2 -> rule.getValue();           // Value column
            case 3 -> rule.getPartText();        // Part column
            default -> null;
        };
    }
    
    /**
     * Clear all data and load a new set of rules
     */
    public void setRules(Set<Rule> newRules) {
        rules.clear();
        
        if (newRules != null && !newRules.isEmpty()) {
            rules.addAll(newRules);
            
            // Sort rules for consistent display order
            rules.sort((r1, r2) -> {
                int comp = Integer.compare(r1.getOperator(), r2.getOperator());
                if (comp == 0) {
                    return Double.compare(r1.getValue(), r2.getValue());
                }
                return comp;
            });
            
            logger.info("Table model loaded with " + rules.size() + " rules");
        } else {
            logger.info("Table model cleared (no rules)");
        }
        
        fireTableDataChanged();
    }
    
    /**
     * Get the rule at the specified row
     */
    public Rule getRuleAt(int row) {
        if (row >= 0 && row < rules.size()) {
            return rules.get(row);
        }
        return null;
    }
    
    /**
     * Find the row index for a rule with the given ID
     */
    public int findRuleRowById(Long ruleId) {
        if (ruleId == null) return -1;
        
        for (int i = 0; i < rules.size(); i++) {
            if (ruleId.equals(rules.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Get rules at the specified rows
     */
    public Rule[] getRulesAt(int[] rows) {
        if (rows == null || rows.length == 0) {
            return new Rule[0];
        }
        
        List<Rule> selectedRules = new ArrayList<>(rows.length);
        for (int row : rows) {
            if (row >= 0 && row < rules.size()) {
                selectedRules.add(rules.get(row));
            }
        }
        
        return selectedRules.toArray(new Rule[0]);
    }
}