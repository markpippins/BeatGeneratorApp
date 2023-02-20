package com.angrysurfer.midi.controller;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConditionDTO {
    String oldOperator;
    String oldComparison;
    double oldValue;
    String newOperator;
    String newComparison;
    double newValue;
}
