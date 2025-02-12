package com.angrysurfer.beatsui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.UIManager;

public class AppToolBar extends JToolBar {

    public AppToolBar() {
        super();
        setup();
    }

    private void setup() {
        setFloatable(false);

        // Left status fields
        JPanel leftStatusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        String[] leftLabels = { "Tick", "Beat", "Bar", "Part", "Players", "Ticks", "Beats", "Bars" };
        for (String label : leftLabels) {
            // Create panel for vertical stacking
            JPanel fieldPanel = new JPanel();
            fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));

            // Create and add label
            JLabel nameLabel = new JLabel(label);
            nameLabel.setForeground(Color.GRAY);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            fieldPanel.add(nameLabel);

            // Create and add text field
            JTextField field = new JTextField("0");
            field.setColumns(4);
            field.setEditable(false);
            field.setEnabled(false);
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setBackground(new Color(240, 240, 240));
            field.setToolTipText("Current " + label.toLowerCase() + " value");
            field.setMaximumSize(new Dimension(50, 25));
            field.setAlignmentX(Component.CENTER_ALIGNMENT);
            fieldPanel.add(field);

            leftStatusPanel.add(fieldPanel);
            leftStatusPanel.add(Box.createHorizontalStrut(5));
        }
        add(leftStatusPanel);

        // Center glue
        add(Box.createHorizontalGlue());

        // Transport controls with correct Unicode characters
        JButton rewindBtn = createToolbarButton("⏮", "Rewind");
        JButton pauseBtn = createToolbarButton("⏸", "Pause");
        JButton recordBtn = createToolbarButton("⏺", "Record");
        JButton stopBtn = createToolbarButton("⏹", "Stop");
        JButton playBtn = createToolbarButton("▶", "Play");
        JButton forwardBtn = createToolbarButton("⏭", "Forward");

        add(rewindBtn);
        add(pauseBtn);
        add(stopBtn);
        add(recordBtn);
        add(playBtn);
        add(forwardBtn);

        addSeparator();

        // Use arrow icons instead of plus/minus
        // JButton upButton = createToolbarButton("↓", "Down");
        // JButton downButton = createToolbarButton("↑", "Up");

        // Or alternatively, if you prefer different arrows:
        JButton upButton = new JButton(UIManager.getIcon("ScrollBar.northButtonIcon"));
        JButton downButton = new JButton(UIManager.getIcon("ScrollBar.southButtonIcon"));

        upButton.setFocusPainted(false);
        downButton.setFocusPainted(false);

        add(upButton);
        add(downButton);
        // Center glue
        add(Box.createHorizontalGlue());

        // Right status fields
        JPanel rightStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        String[][] rightFields = {
                { "Ticker", "1" },
                { "Ticks", "0" },
                { "BPM", "120" },
                { "B/Bar", "4" },
                { "Bars", "4" },
                { "Parts", "1" },
                { "Length", "0" },
                { "Offset", "0" }
        };

        for (String[] field : rightFields) {
            // Create panel for vertical stacking
            JPanel fieldPanel = new JPanel();
            fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));

            // Create and add label
            JLabel nameLabel = new JLabel(field[0]);
            nameLabel.setForeground(Color.GRAY);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            fieldPanel.add(nameLabel);

            // Create and add text field
            JTextField textField = new JTextField(field[1]);
            textField.setColumns(4);
            textField.setEditable(false);
            textField.setEnabled(false);
            textField.setHorizontalAlignment(JTextField.CENTER);
            textField.setBackground(new Color(240, 240, 240));
            textField.setToolTipText(field[0] + " value");
            textField.setMaximumSize(new Dimension(50, 25));
            textField.setAlignmentX(Component.CENTER_ALIGNMENT);
            fieldPanel.add(textField);

            rightStatusPanel.add(fieldPanel);
            rightStatusPanel.add(Box.createHorizontalStrut(5));
        }

        add(rightStatusPanel);
    }

    private JButton createToolbarButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);

        // Increase button size
        int size = 32;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));

        // Use a font that supports Unicode symbols
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        // Fallback fonts if Segoe UI Symbol isn't available
        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }

        // Optional: Add some padding around the text
        button.setMargin(new Insets(5, 5, 5, 5));

        return button;
    }
}
