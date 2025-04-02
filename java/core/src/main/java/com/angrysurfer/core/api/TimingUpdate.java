package com.angrysurfer.core.api;

public record TimingUpdate(Long tick, Double beat, Long bar, Long part, Long tickCount, Long beatCount, Long barCount, Long partCount) {
    /**
     * Constructor for updating tick and beat only
     */
    public TimingUpdate(Long tick, Double beat) {
        this(tick, beat, null, null, null, null, null, null);
    }

    /**
     * Constructor for updating just the tick
     */
    public TimingUpdate(Long tick) {
        this(tick, null, null, null, null, null, null, null);
    }

}
