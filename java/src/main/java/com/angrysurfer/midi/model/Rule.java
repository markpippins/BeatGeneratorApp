package com.angrysurfer.midi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
public class Rule implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    private int operatorId;
    private int comparisonId;
    private Double value;

    public Rule() {

    }

    public Rule(int operatorId, int comparisonId, Double value) {
        setOperatorId(operatorId);
        setComparisonId(comparisonId);
        setValue(value);
    }
}
