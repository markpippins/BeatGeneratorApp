package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;

import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.proxy.ProxyRule;

public class RuleEditPanel extends StatusProviderPanel {
    private final ProxyRule rule;
    private final JComboBox<String> operatorCombo;
    private final JComboBox<String> comparisonCombo;
    private final JSpinner valueSpinner;
    private final JSpinner partSpinner;

    public RuleEditPanel(ProxyRule rule) {
        this(rule, null);
    }

    public RuleEditPanel(ProxyRule rule, StatusConsumer statusConsumer) {
        super(new GridBagLayout(), statusConsumer);
        this.rule = rule;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        operatorCombo = new JComboBox<>(ProxyRule.OPERATORS);
        comparisonCombo = new JComboBox<>(ProxyRule.COMPARISONS);
        
        // Set default value to 1.0 with no upper limit
        double initialValue = (rule != null && rule.getValue() != null) ? rule.getValue() : 1.0;
        valueSpinner = new JSpinner(new SpinnerNumberModel(initialValue, 0.0, null, 0.5));

        // Special SpinnerListModel for Part with "All" option
        String[] partValues = new String[17]; // 0-16 where 0 is "All"
        partValues[0] = "All";
        for (int i = 1; i < 17; i++) {
            partValues[i] = String.valueOf(i);
        }
        partSpinner = new JSpinner(new SpinnerListModel(partValues));

        if (rule != null) {
            operatorCombo.setSelectedIndex(rule.getOperator());
            comparisonCombo.setSelectedIndex(rule.getComparison());
            valueSpinner.setValue(rule.getValue());
            partSpinner.setValue(rule.getPart() == 0 ? "All" : String.valueOf(rule.getPart()));
        }

        layoutComponents();
        setPreferredSize(new Dimension(300, 350));
    }

    private void layoutComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addComponent("Operator", operatorCombo, 0, 0, gbc);
        addComponent("Comparison", comparisonCombo, 0, 1, gbc);
        addComponent("Value", valueSpinner, 0, 2, gbc);
        addComponent("Part", partSpinner, 0, 3, gbc);
    }

    private void addComponent(String label, JComponent component, int x, int y, GridBagConstraints gbc) {
        gbc = (GridBagConstraints) gbc.clone();
        gbc.gridx = x;
        gbc.gridy = y;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        add(panel, gbc);
    }

    public ProxyRule getUpdatedRule() {
        rule.setOperator(operatorCombo.getSelectedIndex());
        rule.setComparison(comparisonCombo.getSelectedIndex());
        rule.setValue((Double) valueSpinner.getValue());
        
        // Convert "All" to 0, otherwise parse the string to integer
        String partValue = (String) partSpinner.getValue();
        int part = "All".equals(partValue) ? 0 : Integer.parseInt(partValue);
        rule.setPart(part);
        
        return rule;
    }
}
