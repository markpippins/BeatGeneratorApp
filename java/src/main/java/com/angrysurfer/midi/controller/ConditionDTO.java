package com.angrysurfer.midi.controller;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConditionDTO {
    Long conditionId;
    String newOperator;
    String newComparison;
    double newValue;
}
