package com.angrysurfer.beatsui.panel;

import javax.swing.*;

import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.mock.Rule;

import java.awt.*;

public class RuleEditorPanel extends StatusProviderPanel {
    private final Rule rule;
    private final JComboBox<String> operatorCombo;
    private final JComboBox<String> comparisonCombo;
    private final JSpinner valueSpinner;
    private final JSpinner partSpinner;

    public RuleEditorPanel(Rule rule) {
        this(rule, null);
    }

    public RuleEditorPanel(Rule rule, StatusConsumer statusConsumer) {
        super(new GridBagLayout(), statusConsumer);
        this.rule = rule;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        operatorCombo = new JComboBox<>(Rule.OPERATORS);
        operatorCombo.setSelectedIndex(rule.getOperator());

        comparisonCombo = new JComboBox<>(Rule.COMPARISONS);
        comparisonCombo.setSelectedIndex(rule.getComparison());

        // Fix ambiguous constructor calls by explicitly specifying types
        valueSpinner = new JSpinner(new SpinnerNumberModel(
                rule.getValue().doubleValue(), // current value
                0.0d, // minimum value
                100.0d, // maximum value
                1.0d // step size
        ));

        partSpinner = new JSpinner(new SpinnerNumberModel(
                rule.getPart().intValue(), // current value
                0, // minimum value
                16, // maximum value
                1 // step size
        ));

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

    public Rule getUpdatedRule() {
        rule.setOperator(operatorCombo.getSelectedIndex());
        rule.setComparison(comparisonCombo.getSelectedIndex());
        rule.setValue((Double) valueSpinner.getValue());
        rule.setPart((Integer) partSpinner.getValue());
        return rule;
    }
}
