package com.angrysurfer.core.proxy;

import java.io.Serializable;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProxyRule implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ProxyRule.class);

    public static final int ALL_PARTS = 0;

    private Long id;
    private Long playerId;
    private Integer operator = 0;
    private Integer comparison = 0;;
    private Double value = 0.0;
    private Integer part = 0;

    private Long start = 0L;
    private Long end = 0L;

    @JsonIgnore
    private transient boolean unsaved = false;

    @JsonIgnore
    private transient IProxyPlayer player;

    // Update COMPARISONS array to match Comparison interface
    public static final String[] COMPARISONS = {
            "=", ">", "<", "%", "!="
    };

    // Update OPERATORS array to match Operator interface
    public static final String[] OPERATORS = {
            "Tick", "Beat", "Bar", "Part", "Beat Duration",
            "Tick Count", "Beat Count", "Bar Count", "Part Count"
    };

    public ProxyRule() {

    }

    public ProxyRule(int operator, int comparison, Double value, int part) {
        logger.debug("Creating new Rule - operator: {}, comparison: {}, value: {}, part: {}",
                operator, comparison, value, part);
        setOperator(operator);
        setComparison(comparison);
        setValue(value);
        setPart(part);
    }

    public ProxyRule(int operator, int comparison, Double value, int part, boolean unsaved) {
        this(operator, comparison, value, part);
        setUnsaved(unsaved);
    }

    public ProxyRule(IProxyPlayer player, int operator, int comparison, Double value, int part) {
        setPlayer(player);
        setOperator(operator);
        setComparison(comparison);
        setValue(value);
        setPart(part);
    }

    public void setPlayer(IProxyPlayer player) {
        logger.debug("setPlayer() - player: {}", player != null ? player.getName() : "null");
        this.player = player;
        if (Objects.nonNull(player))
            this.playerId = player.getId();
        else
            this.playerId = null;
    }

    public boolean isEqualTo(ProxyRule rule) {
        boolean result = (getValue().equals(rule.getValue()) &&
                (this.getComparison() == rule.getComparison()) &&
                (this.getOperator() == rule.getOperator()));
        logger.debug("isEqualTo() - comparing with rule: {}, result: {}", rule.getId(), result);
        return result;
    }

    public Object[] toRow() {
        logger.debug("Converting rule to row - ID: {}, Operator: {}, Comparison: {}, Value: {}, Part: {}",
            getId(), getOperator(), getComparison(), getValue(), getPart());
            
        return new Object[] {
            OPERATORS[getOperator()],
            COMPARISONS[getComparison()],
            getValue(),
            getPart()
        };
    }

    public static ProxyRule fromRow(Object[] row) {
        ProxyRule rule = new ProxyRule();
        
        // Find operator index
        for (int i = 0; i < OPERATORS.length; i++) {
            if (OPERATORS[i].equals(row[0])) {
                rule.setOperator(i);
                break;
            }
        }
        
        // Find comparison index
        for (int i = 0; i < COMPARISONS.length; i++) {
            if (COMPARISONS[i].equals(row[1])) {
                rule.setComparison(i);
                break;
            }
        }
        
        rule.setValue(((Number) row[2]).doubleValue());
        rule.setPart((Integer) row[3]);
        
        logger.debug("Created rule from row - Operator: {}, Comparison: {}, Value: {}, Part: {}",
            rule.getOperator(), rule.getComparison(), rule.getValue(), rule.getPart());
            
        return rule;
    }
}
