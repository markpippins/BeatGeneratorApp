package com.angrysurfer.beatsui.grid.visualizer;

import java.awt.Color;
import com.angrysurfer.beatsui.grid.Visualization;
import com.angrysurfer.beatsui.widget.GridButton;

public class ChordProgressionVisualization implements Visualization {
    private double phase = 0.0;
    private final int[][] chords = {
        {0, 4, 7},    // C major
        {5, 9, 12},   // F major
        {7, 11, 14},  // G major
        {0, 4, 7}     // C major
    };

    @Override
    public void update(GridButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        int chordIndex = ((int)(phase * 2)) % chords.length;
        int[] currentChord = chords[chordIndex];

        // Display chord notes
        for (int note : currentChord) {
            int row = note % buttons.length;
            Color noteColor = Color.CYAN;
            
            // Make root note brighter
            if (note == currentChord[0]) {
                noteColor = Color.WHITE;
            }

            // Draw chord note across columns
            for (int col = 0; col < buttons[0].length; col++) {
                if (col % 4 == 0) { // Pulsing effect
                    double pulse = Math.sin(phase * 4 + col * 0.1);
                    if (pulse > 0) {
                        buttons[row][col].setBackground(noteColor);
                    }
                }
            }
        }
        phase += 0.02;
    }

    @Override
    public String getName() {
        return "Chord Progression";
    }
}
