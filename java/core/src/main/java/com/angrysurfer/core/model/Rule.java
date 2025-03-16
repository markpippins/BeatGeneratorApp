package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Rule implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Rule.class);

    public static final int ALL_PARTS = 0;

    private Long id;

    @JsonIgnore
    @Transient
    public boolean isSelected = false;

    private Long playerId;
    private Integer operator = 0;
    private Integer comparison = 0;;
    private Double value = 1.0;
    private Integer part = 0;

    private Long start = 0L;
    private Long end = 0L;

    @JsonIgnore
    private transient boolean unsaved = false;

    @JsonIgnore
    private transient boolean isFirst = false;

    @JsonIgnore
    private transient boolean isLast = false;

    @JsonIgnore
    private transient Player player;

    // Update COMPARISONS array to match Comparison interface
    public static final String[] COMPARISONS = {
            "=", ">", "<", "%", "!="
    };

    // Update OPERATORS array to match Operator interface
    public static final String[] OPERATORS = {
            "Tick", "Beat", "Bar", "Part", "Beat Duration",
            "Tick Count", "Beat Count", "Bar Count", "Part Count"
    };

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

    public Rule(Player player, int operator, int comparison, Double value, int part) {
        setPlayer(player);
        setOperator(operator);
        setComparison(comparison);
        setValue(value);
        setPart(part);
    }

    public void setPlayer(Player player) {
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

    public static Rule fromRow(Object[] row) {
        Rule rule = new Rule();

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

        // Handle "All" for part value
        Object partValue = row[3];
        if (partValue instanceof String && "All".equals(partValue)) {
            rule.setPart(0);
        } else if (partValue instanceof Number) {
            rule.setPart(((Number) partValue).intValue());
        } else {
            rule.setPart(Integer.parseInt(partValue.toString()));
        }

        logger.debug("Created rule from row - Operator: {}, Comparison: {}, Value: {}, Part: {}",
                rule.getOperator(), rule.getComparison(), rule.getValue(), rule.getPart());

        return rule;
    }

    // Add these helper methods to Rule class for safer formatting
    public String getComparisonText() {
        return getComparison() >= 0 && getComparison() < COMPARISONS.length ? 
               COMPARISONS[getComparison()] : "Unknown";
    }

    public String getOperatorText() {
        return getOperator() >= 0 && getOperator() < OPERATORS.length ? 
               OPERATORS[getOperator()] : "Unknown";
    }

    public String getPartText() {
        return getPart() == 0 ? "All" : String.valueOf(getPart());
    }

    @Override
    public String toString() {
        String compStr = getComparison() >= 0 && getComparison() < COMPARISONS.length ? 
                        COMPARISONS[getComparison()] : "Unknown";
        String opStr = getOperator() >= 0 && getOperator() < OPERATORS.length ? 
                      OPERATORS[getOperator()] : "Unknown";
        String partStr = getPart() == 0 ? "All" : String.valueOf(getPart());
        
        // Change %d to %s for getValue() to handle any numeric type
        return String.format("Rule[id=%d, %s %s %s, Part=%s]", 
                getId(), compStr, opStr, String.valueOf(getValue()), partStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return Objects.equals(id, rule.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean functionallyEquals(Rule other) {
        if (this == other) return true;
        if (other == null) return false;
        
        return this.operator == other.operator &&
               this.comparison == other.comparison &&
               this.part == other.part &&
               Math.abs(this.value - other.value) < 0.00001;
    }
}
