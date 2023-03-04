package com.angrysurfer.midi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StepData {
    @JsonProperty
    boolean active;
    @JsonProperty
    int step;
    @JsonProperty
    int pitch;
    @JsonProperty
    int velocity;
    @JsonProperty
    int probability;
    @JsonProperty
    int gate;
}
