package com.angrysurfer.core.sequencer;

public record TimingUpdate(Long tick, Double beat, Integer bar, Integer part, Long tickCount, Integer beatCount, Integer barCount, Integer partCount) {
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
