package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.angrysurfer.beats.widget.Dial;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DialPanel extends JPanel {

    private Dial dial;
    private JLabel label;

    public DialPanel(String name, String labelText, int min, int max, int value) {
        super(new BorderLayout());

        label = new JLabel(labelText);
        label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        // label.setForeground(Color.GRAY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        labelPanel.add(label);
        // Add label to the column
        add(labelPanel, BorderLayout.NORTH);

        dial = new Dial(name);
        dial.setMinimum(min);   
        dial.setMaximum(max);
        dial.setValue(value);
        dial.setSize(80, 80);
        dial.setName(name);
        
        // dial.setPreferredSize(dial.getSize());
        // dial.setMinimumSize(dial.getSize());
        // dial.setMaximumSize(dial.getSize());

        add(dial, BorderLayout.CENTER);
    }

    public DialPanel(String name, String labelText) {
        this(name, labelText, 0, 126, 64);
    }
}
