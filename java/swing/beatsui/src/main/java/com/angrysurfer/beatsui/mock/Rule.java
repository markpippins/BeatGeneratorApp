package com.angrysurfer.beatsui.mock;

import java.io.Serializable;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.player.IPlayer;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Rule implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Rule.class);

    private Long id;
    private Long playerId;
    private Integer operator;
    private Integer comparison;
    private Double value;
    private Integer part = 0;

    private Long start = 0L;
    private Long end = 0L;

    @JsonIgnore
    private transient boolean unsaved = false;

    @JsonIgnore
    private transient IPlayer player;

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

    public Rule(int operator, int comparison, Double value, int part, boolean unsaved) {
        this(operator, comparison, value, part);
        setUnsaved(unsaved);
    }

    public Rule(IPlayer player, int operator, int comparison, Double value, int part) {
        setPlayer(player);
        setOperator(operator);
        setComparison(comparison);
        setValue(value);
        setPart(part);
    }

    public void setPlayer(IPlayer player) {
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
