package com.angrysurfer.beatsui.surface.visualization;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.beatsui.surface.VisualizationHandler;
import com.angrysurfer.beatsui.widget.GridButton;

public class RainbowJapaneseMatrixVisualization implements VisualizationHandler {
    private int[] dropY;
    private boolean[] active;
    private Random random = new Random();
    private static final Color[] RAINBOW_COLORS = {
        Color.RED,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.BLUE,
        new Color(75, 0, 130), // Indigo
        new Color(148, 0, 211) // Violet
    };
    
    // Japanese Hiragana and Katakana characters
    private static final String[] JAPANESE_CHARS = {
        "あ", "い", "う", "え", "お", "か", "き", "く", "け", "こ",
        "ア", "イ", "ウ", "エ", "オ", "カ", "キ", "ク", "ケ", "コ",
        "さ", "し", "す", "せ", "そ", "た", "ち", "つ", "て", "と",
        "サ", "シ", "ス", "セ", "ソ", "タ", "チ", "ツ", "テ", "ト",
        "な", "に", "ぬ", "ね", "の", "は", "ひ", "ふ", "へ", "ほ",
        "ナ", "ニ", "ヌ", "ネ", "ノ", "ハ", "ヒ", "フ", "ヘ", "ホ"
    };

    public RainbowJapaneseMatrixVisualization() {
        dropY = new int[36];
        active = new boolean[36];
        for (int i = 0; i < dropY.length; i++) {
            dropY[i] = random.nextInt(8);
            active[i] = random.nextBoolean();
        }
    }

    @Override
    public void update(GridButton[][] buttons) {
        // Clear all buttons
        for (int x = 0; x < buttons[0].length; x++) {
            for (int y = 0; y < buttons.length; y++) {
                buttons[y][x].setBackground(Color.BLACK);
                buttons[y][x].setText("");
            }
        }

        // Update and draw drops
        for (int x = 0; x < dropY.length; x++) {
            if (active[x]) {
                // Draw the trail
                for (int trail = 0; trail < 3; trail++) {
                    int y = (dropY[x] - trail + 8) % 8;
                    if (y >= 0 && y < buttons.length) {
                        // Get rainbow color based on position
                        Color color = RAINBOW_COLORS[(x + trail) % RAINBOW_COLORS.length];
                        // Fade color based on trail position
                        float fade = 1.0f - (trail * 0.3f);
                        color = new Color(
                            (int)(color.getRed() * fade),
                            (int)(color.getGreen() * fade),
                            (int)(color.getBlue() * fade)
                        );
                        
                        buttons[y][x].setBackground(color);
                        buttons[y][x].setText(JAPANESE_CHARS[random.nextInt(JAPANESE_CHARS.length)]);
                    }
                }

                // Move drop down
                dropY[x] = (dropY[x] + 1) % 8;

                // Randomly deactivate
                if (random.nextInt(50) == 0) {
                    active[x] = false;
                }
            } else {
                // Randomly activate
                if (random.nextInt(10) == 0) {
                    active[x] = true;
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Rainbow Japanese Matrix";
    }
}
