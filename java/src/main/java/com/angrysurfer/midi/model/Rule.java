package com.angrysurfer.midi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@Entity
public class Rule implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    private int operatorId;
    private int comparisonId;
    private Double value;

    @ManyToOne()
    @JoinColumn(name = "player_id")
    @JsonIgnore
    @Transient
    Player player;
    
    public Rule() {

    }

    public Rule(int operatorId, int comparisonId, Double value) {
        setOperatorId(operatorId);
        setComparisonId(comparisonId);
        setValue(value);
    }

    public boolean isEqualTo(Rule otherRule) {
        return Objects.equals(this.id, otherRule.getId()) && Objects.equals(this.operatorId, otherRule.getOperatorId()) 
            && Objects.equals(this.comparisonId, otherRule.getComparisonId());
    }
}
