package com.angrysurfer.beatsui.panel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MockPlayer {
    private String name;
    private int channel;
    private Long swing = 0L;
    private Long level = 100L;
    private Long note = 60L;
    private Long minVelocity = 100L;
    private Long maxVelocity = 110L;
    private Long preset = 1L;
    private Boolean stickyPreset = false;
    private Long probability = 100L;
    private Long randomDegree = 0L;
    private Long ratchetCount = 0L;
    private Long ratchetInterval = 1L;
    private Long internalBars = 4L;
    private Long internalBeats = 4L;
    private Boolean useInternalBeats = false;
    private Boolean useInternalBars = false;
    private Long panPosition = 63L;
    private Boolean preserveOnPurge = false;
    private double sparse = 0.0;
    private Long subDivisions = 4L;
    private Long beatFraction = 1L;
    private Long fadeOut = 0L;
    private Long fadeIn = 0L;
    private Boolean accent = false;

    public MockPlayer(String name, int channel, long note) {
        this.name = name;
        this.channel = Math.min(12, Math.max(1, channel));
        this.note = Math.min(127, Math.max(1, note));
    }

    public Object[] toRow() {
        return new Object[] {
                name, channel, swing, level, note, minVelocity, maxVelocity,
                preset, stickyPreset, probability, randomDegree, ratchetCount,
                ratchetInterval, useInternalBeats, useInternalBars, panPosition,
                preserveOnPurge, sparse
        };
    }

    public static MockPlayer getSampleData() {
        MockPlayer player = new MockPlayer("Test Drum", 2, 36);
        player.setSwing(25L);
        player.setLevel(100L);
        player.setMinVelocity(80L);
        player.setMaxVelocity(120L);
        player.setPreset(3L);
        player.setStickyPreset(true);
        player.setProbability(85L);
        player.setRandomDegree(15L);
        player.setRatchetCount(2L);
        player.setRatchetInterval(4L);
        player.setInternalBars(4L);
        player.setInternalBeats(4L);
        player.setUseInternalBeats(true);
        player.setPanPosition(64L);
        player.setPreserveOnPurge(false);
        player.setSparse(0.3);
        player.setSubDivisions(4L);
        player.setBeatFraction(1L);
        player.setFadeIn(0L);
        player.setFadeOut(0L);
        player.setAccent(false);
        return player;
    }
}