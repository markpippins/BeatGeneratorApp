package com.angrysurfer.midi.model.condition;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
public class Condition implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    private Operator operator;
    private Comparison comparison;
    private Double value;

    public Condition() {

    }

    public Condition(Operator operator, Comparison comparison, Double value) {
        setId(id);
        setOperator(operator);
        setComparison(comparison);
        setValue(value);
    }

}
