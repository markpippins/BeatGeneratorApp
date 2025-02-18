package com.angrysurfer.beats.panel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.angrysurfer.core.proxy.ProxyCaption;

import lombok.Getter;

@Getter
public class CaptionEditPanel extends JPanel {
    private final ProxyCaption caption;
    private final JSpinner codeSpinner;
    private final JTextField descriptionField;

    public CaptionEditPanel(ProxyCaption caption) {
        super(new GridBagLayout());
        this.caption = caption;

        // Fix: Use Long value for the spinner
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel();
        spinnerModel.setValue(caption.getCode());
        spinnerModel.setMinimum(0L);
        spinnerModel.setMaximum(127L);
        spinnerModel.setStepSize(1L);
        codeSpinner = new JSpinner(spinnerModel);

        descriptionField = new JTextField(caption.getDescription(), 20);
        setupLayout();
    }

    private void setupLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        addFormField("Code:", codeSpinner, gbc, 0);
        addFormField("Description:", descriptionField, gbc, 1);
    }

    private void addFormField(String label, JComponent field, 
                            GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(field, gbc);
    }

    public ProxyCaption getUpdatedCaption() {
        // No need to cast, we're already using Long
        caption.setCode((Long) codeSpinner.getValue());
        caption.setDescription(descriptionField.getText());
        return caption;
    }
}
