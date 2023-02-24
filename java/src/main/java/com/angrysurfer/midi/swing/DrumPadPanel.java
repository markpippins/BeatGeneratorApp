package com.angrysurfer.midi.swing;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;

public class DrumPadPanel extends JPanel {

    String[][] rows = new String[][]{{"Ride", "Clap", "Perc", "Bass"}, {"Tom", "Clap", "Wood", "P1"}, {"Ride", "fx", "Perc", "Bass"}, {"Kick", "Snare", "Closed Hat", "Open Hat"},};

    int rowCount = 4;
    private int bottomNote = 36;

    public DrumPadPanel() {
        super();
        add(new JLabel("Drum Pad"), BorderLayout.NORTH);
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        JPanel drumPad = new JPanel(new GridLayout(4, 4));
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                drumPad.add(new DrumPadButton(Integer.toString(getNote(row, col))));
        add(drumPad, BorderLayout.CENTER);
//        setMaximumSize(new Dimension(800, 800));
        setMinimumSize(new Dimension(1600, 1600));
    }

    private int getNote(int row, int column) {
        return this.bottomNote + column + Math.abs(this.rows.length - 1 - row) * this.rows.length;
    }
}

//        add(drumSelectorPanel);
//        JPanel drumSelectorPanel = new JPanel(new GridLayout(1, 1));
//        JComboBox combo = new JComboBox();
//        drumSelectorPanel.add(combo);
//    <div * ngFor = "let drum of row; let index = index" class="grid-item" >
//
//
//                getNote(row:string[],column:
//        number){
//            return (
//                    this.firstNote +
//                            column +
//                            Math.abs(this.rows.length - 1 - this.rows.indexOf(row)) * this.rows.length
//            );
//        }

//        HttpRequest request = null;
//        try {
//            request = HttpRequest.newBuilder()
//                    .uri(new URI("https://postman-echo.com/get"))
//                    .headers("key1", "value1", "key2", "value2")
//                    .GET()
//                    .build();
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//
//        try {
//            HttpRequest request2 = HttpRequest.newBuilder()
//                    .uri(new URI("https://postman-echo.com/get"))
//                    .header("key1", "value1")
//                    .header("key2", "value2")
//                    .GET()
//                    .build();
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//        HttpRequest.newBuilder(request, (name, value) -> !name.equalsIgnoreCase("Foo-Bar"));

