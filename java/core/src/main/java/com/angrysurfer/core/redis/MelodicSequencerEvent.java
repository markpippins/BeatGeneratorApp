package com.angrysurfer.core.redis;

/**
 * Class to hold sequencer ID and sequence ID for events
 */
public class MelodicSequencerEvent {
    private final Integer sequencerId;
    private final Long sequenceId;
    
    public MelodicSequencerEvent(Integer sequencerId, Long sequenceId) {
        this.sequencerId = sequencerId;
        this.sequenceId = sequenceId;
    }
    
    public Integer getSequencerId() {
        return sequencerId;
    }
    
    public Long getSequenceId() {
        return sequenceId;
    }
}