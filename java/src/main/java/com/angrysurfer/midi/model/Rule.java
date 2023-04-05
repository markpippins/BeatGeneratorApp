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

    private Long playerId;
    private Integer operator;
    private Integer comparison;
    private Double value;
    private Integer part = 0;

    private Long start = 0L;
    private Long end = 0L;

    @Transient
    @JsonIgnore
    Player player;
    
    public Rule() {

    }

    public Rule(int operator, int comparison, Double value, int part) {
        setOperator(operator);
        setComparison(comparison);
        setValue(value);
        setPart(part);
    }

    public Rule(Player player, int operator, int comparison, Double value, int part) {
        setPlayer(player);
        setOperator(operator);
        setComparison(comparison);
        setValue(value);
        setPart(part);
    }

    public void setPlayer(Player player) {
        this.player = player;
        if (Objects.nonNull(player))
            this.playerId = player.getId();
        else
            this.playerId = null;
    }

    public boolean isEqualTo(Rule rule) {
        return (getValue().equals(rule.getValue()) &&
            (this.getComparison() == rule.getComparison()) && 
            (this.getOperator() == rule.getOperator()));
    }
}
