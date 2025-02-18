package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

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
        operatorCombo.setSelectedIndex(rule.getOperator());

        comparisonCombo = new JComboBox<>(ProxyRule.COMPARISONS);
        comparisonCombo.setSelectedIndex(rule.getComparison());

        valueSpinner = new JSpinner(new SpinnerNumberModel(
                rule.getValue().doubleValue(),
                0.0d,
                100.0d,
                1.0d));

        // Setup part spinner with custom formatter
        SpinnerNumberModel partModel = new SpinnerNumberModel(
                Integer.valueOf(rule.getPart()),  // current value as Integer
                Integer.valueOf(0),               // minimum as Integer
                Integer.valueOf(16),              // maximum as Integer
                Integer.valueOf(1)                // step size as Integer
        );
        partSpinner = new JSpinner(partModel);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(partSpinner);
        partSpinner.setEditor(editor);

        // Create a basic NumberFormatter
        NumberFormatter formatter = new NumberFormatter();
        // Allow negative values in case needed
        formatter.setMinimum(0);
        formatter.setMaximum(16);
        // Override the format method to show "All" for 0
        formatter.setFormat(new java.text.DecimalFormat() {
            @Override
            public StringBuffer format(double number, StringBuffer result, java.text.FieldPosition fieldPosition) {
                if (number == ProxyRule.ALL_PARTS) {
                    result.append("All");
                } else {
                    super.format(number, result, fieldPosition);
                }
                return result;
            }
        });

        JFormattedTextField ftf = ((JSpinner.DefaultEditor) partSpinner.getEditor()).getTextField();
        ftf.setFormatterFactory(new DefaultFormatterFactory(formatter));

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
        rule.setPart((Integer) partSpinner.getValue());
        return rule;
    }
}
