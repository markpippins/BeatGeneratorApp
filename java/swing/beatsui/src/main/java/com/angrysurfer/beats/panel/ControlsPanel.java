package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.ToggleSwitch;
import com.angrysurfer.core.proxy.ProxyCaption;
import com.angrysurfer.core.proxy.ProxyControlCode;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.service.RedisService;

public class ControlsPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(ControlsPanel.class.getName());
    private final JComboBox<ProxyInstrument> instrumentSelector;
    private final RedisService redisService;
    private final JPanel controlsContainer;
    private static final int CONTROLS_PER_ROW = 8; // Changed from 6 to 12
    private static final int MIN_CONTROLS_PER_ROW = 3;

    public ControlsPanel() {
        setLayout(new BorderLayout());
        this.redisService = RedisService.getInstance();
        
        // Create toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // Create and configure instrument selector
        instrumentSelector = new JComboBox<>();
        instrumentSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, 
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ProxyInstrument) {
                    setText(((ProxyInstrument) value).getName());
                }
                return this;
            }
        });
        
        // Add components to toolbar
        toolBar.add(new JLabel("Instrument: "));
        toolBar.add(instrumentSelector);
        
        // Create scrollable container for controls
        controlsContainer = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(controlsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Add components to panel
        add(toolBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Load instruments
        refreshInstruments();
        
        // Add selection listener
        instrumentSelector.addActionListener(e -> {
            ProxyInstrument selected = (ProxyInstrument) instrumentSelector.getSelectedItem();
            if (selected != null) {
                logger.info("Selected instrument: " + selected.getName());
                updateControlsDisplay(selected);
            }
        });
    }

    private void updateControlsDisplay(ProxyInstrument instrument) {
        controlsContainer.removeAll();
        
        if (instrument.getControlCodes() == null || instrument.getControlCodes().isEmpty()) {
            controlsContainer.add(new JLabel("No controls available"));
            return;
        }

        List<ProxyControlCode> sortedControls = new ArrayList<>(instrument.getControlCodes());
        sortedControls.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        
        int row = 0;
        int col = 0;
        String currentPrefix = "";
        
        for (int i = 0; i < sortedControls.size(); i++) {
            ProxyControlCode controlCode = sortedControls.get(i);
            String name = controlCode.getName();
            String prefix = name.split(" ")[0];
            
            // Look ahead to see if breaking here would leave too few controls
            boolean wouldLeaveTooFew = false;
            if (!prefix.equals(currentPrefix) && col != 0) {
                int remainingInCurrentRow = CONTROLS_PER_ROW - col;
                int remainingInGroup = 0;
                String nextPrefix = prefix;
                
                // Count controls in next group
                for (int j = i; j < sortedControls.size() && nextPrefix.equals(prefix); j++) {
                    remainingInGroup++;
                    if (j + 1 < sortedControls.size()) {
                        nextPrefix = sortedControls.get(j + 1).getName().split(" ")[0];
                    }
                }
                
                // Would this break leave either the current row or next group with too few items?
                wouldLeaveTooFew = remainingInCurrentRow < MIN_CONTROLS_PER_ROW || 
                                 (remainingInGroup > 0 && remainingInGroup < MIN_CONTROLS_PER_ROW);
            }
            
            // Start new row if appropriate
            if (!prefix.equals(currentPrefix) && col != 0 && !wouldLeaveTooFew) {
                // Fill remaining columns
                while (col < CONTROLS_PER_ROW) {
                    gbc.gridx = col;
                    gbc.gridy = row * 2;
                    gbc.gridheight = 2;
                    controlsContainer.add(Box.createHorizontalStrut(50), gbc);
                    col++;
                }
                col = 0;
                row++;
            }
            currentPrefix = prefix;
            
            // Add label and control
            gbc.gridx = col;
            gbc.gridy = row * 2;
            gbc.gridheight = 1;
            JLabel label = new JLabel(controlCode.getName(), SwingConstants.CENTER);
            label.setPreferredSize(new Dimension(70, 20));
            controlsContainer.add(label, gbc);
            
            gbc.gridy = row * 2 + 1;
            Component control = createControl(controlCode);
            controlsContainer.add(control, gbc);
            
            col++;
            if (col >= CONTROLS_PER_ROW) {
                col = 0;
                row++;
                currentPrefix = "";
            }
        }

        // Fill final row appropriately
        if (col != 0 && col < MIN_CONTROLS_PER_ROW) {
            // If less than minimum controls in last row, move them to previous row if possible
            int finalCol = col;
            if (row > 0) {
                // Remove the last few controls and add them to the previous row
                for (int i = 0; i < finalCol; i++) {
                    controlsContainer.remove(controlsContainer.getComponentCount() - 2); // Remove both label and control
                }
                // Re-add them to the previous row
                // This will be handled in the next update cycle
            }
        } else if (col != 0) {
            // Fill remaining space in last row
            while (col < CONTROLS_PER_ROW) {
                gbc.gridx = col;
                gbc.gridy = row * 2;
                gbc.gridheight = 2;
                controlsContainer.add(Box.createHorizontalStrut(50), gbc);
                col++;
            }
        }

        // Add bottom filler
        gbc.gridx = 0;
        gbc.gridy = (row + 1) * 2;
        gbc.gridwidth = CONTROLS_PER_ROW;
        gbc.gridheight = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        controlsContainer.add(Box.createVerticalGlue(), gbc);

        revalidate();
        repaint();
    }

    private Component createControl(ProxyControlCode controlCode) {
        int lowerBound = controlCode.getLowerBound() != null ? controlCode.getLowerBound() : 0;
        int upperBound = controlCode.getUpperBound() != null ? controlCode.getUpperBound() : 127;
        
        if (controlCode.getCaptions() != null) {
            int captionCount = controlCode.getCaptions().size();
            logger.info("Creating control for " + controlCode.getName() + 
                       " with " + captionCount + " captions");
            
            if (captionCount == 2) {
                // Create a toggle switch instead of a toggle button
                ToggleSwitch toggle = new ToggleSwitch();
                
                // Sort captions by code to ensure consistent ordering
                List<ProxyCaption> sortedCaptions = new ArrayList<>(controlCode.getCaptions());
                sortedCaptions.sort((a, b) -> a.getCode().compareTo(b.getCode()));
                
                String offState = sortedCaptions.get(0).getDescription();
                String onState = sortedCaptions.get(1).getDescription();
                
                logger.info("Created toggle switch for " + controlCode.getName() + 
                          " with states: " + offState + " / " + onState);
                
                // Set tool tip to show both states
                toggle.setToolTipText(offState + " / " + onState);
                toggle.setPreferredSize(new Dimension(60, 30));
                
                // Add label panel to show current state
                JPanel togglePanel = new JPanel(new BorderLayout(5, 2));
                JLabel stateLabel = new JLabel(offState, SwingConstants.CENTER);
                togglePanel.add(toggle, BorderLayout.CENTER);
                togglePanel.add(stateLabel, BorderLayout.SOUTH);
                
                toggle.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        stateLabel.setText(toggle.isSelected() ? onState : offState);
                    }
                });
                
                return togglePanel;
            } else if (captionCount > 2) {
                // Create slider for more than 2 captions
                logger.info("Creating slider for " + controlCode.getName() + 
                          " with " + captionCount + " captions");
                JSlider slider = new JSlider(JSlider.VERTICAL, lowerBound, upperBound, lowerBound);
                slider.setPaintTicks(true);
                slider.setPaintLabels(true);
                slider.setMajorTickSpacing((upperBound - lowerBound) / 4);
                slider.setPreferredSize(new Dimension(50, 120));
                return slider;
            }
        }
        
        // Default to dial for continuous control
        logger.info("Creating dial for " + controlCode.getName());
        Dial dial = new Dial();
        dial.setMinimum(lowerBound);
        dial.setMaximum(upperBound);
        dial.setPreferredSize(new Dimension(50, 50));
        return dial;
    }

    public void refreshInstruments() {
        instrumentSelector.removeAllItems();
        List<ProxyInstrument> instruments = redisService.findAllInstruments();
        
        // Sort instruments by name
        instruments.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        
        for (ProxyInstrument instrument : instruments) {
            instrumentSelector.addItem(instrument);
        }
        
        logger.info("Loaded " + instruments.size() + " instruments into selector");
    }
}
