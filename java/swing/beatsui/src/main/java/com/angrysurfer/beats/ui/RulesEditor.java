package com.angrysurfer.beats.ui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

import com.angrysurfer.core.proxy.ProxyRule;

public class RulesEditor {
    
    private JComboBox<Object> partComboBox;
    
    public RulesEditor() {
        initComponents();
    }

    private void initComponents() {
        // Initialize the parts combo box with "All" and numbers
        partComboBox = new JComboBox<>();
        partComboBox.addItem("All");  // Add "All" as first item
        for (int i = 1; i <= 16; i++) {  // Add numbers 1-16 or however many parts you need
            partComboBox.addItem(i);
        }

        // Modify the part combo box renderer
        partComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof Integer && ((Integer)value) == ProxyRule.ALL_PARTS) {
                    value = "All";
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        // ...existing code...
    }

    // When setting the rule's part in the UI
    private void updateUI(ProxyRule rule) {
        // ...existing code...
        if (rule.getPart() == ProxyRule.ALL_PARTS) {
            partComboBox.setSelectedItem("All");
        } else {
            partComboBox.setSelectedItem(rule.getPart());
        }
        // ...existing code...
    }

    // When getting the part value from the UI
    private void updateRule(ProxyRule rule) {
        // ...existing code...
        Object selectedPart = partComboBox.getSelectedItem();
        if (selectedPart instanceof String && "All".equals(selectedPart)) {
            rule.setPart(ProxyRule.ALL_PARTS);
        } else if (selectedPart instanceof Integer) {
            rule.setPart((Integer) selectedPart);
        }
        // ...existing code...
    }
}
