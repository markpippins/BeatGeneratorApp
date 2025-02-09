package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.player.AbstractPlayer;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// @RedisHash("rule")
@Entity

public class Rule implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Rule.class);

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
    AbstractPlayer player;
    
    public Rule() {

    }

    public Rule(int operator, int comparison, Double value, int part) {
        logger.debug("Creating new Rule - operator: {}, comparison: {}, value: {}, part: {}", 
            operator, comparison, value, part);
        setOperator(operator);
        setComparison(comparison);
        setValue(value);
        setPart(part);
    }

    public Rule(AbstractPlayer player, int operator, int comparison, Double value, int part) {
        setPlayer(player);
        setOperator(operator);
        setComparison(comparison);
        setValue(value);
        setPart(part);
    }

    public void setPlayer(AbstractPlayer player) {
        logger.debug("setPlayer() - player: {}", player != null ? player.getName() : "null");
        this.player = player;
        if (Objects.nonNull(player))
            this.playerId = player.getId();
        else
            this.playerId = null;
    }

    public boolean isEqualTo(Rule rule) {
        boolean result = (getValue().equals(rule.getValue()) &&
            (this.getComparison() == rule.getComparison()) && 
            (this.getOperator() == rule.getOperator()));
        logger.debug("isEqualTo() - comparing with rule: {}, result: {}", rule.getId(), result);
        return result;
    }
}
