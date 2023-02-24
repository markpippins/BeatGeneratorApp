package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Eval implements Serializable {

    private int id;
    public Eval() {

    }


    public Eval(int id, Operator operator, Comparison comparison, Double value) {
        setId(id);
        setOperator(operator);
        setComparison(comparison);
        setValue(value);
    }
    private Operator operator;
    private Comparison comparison;
    private Double value;

    public enum Comparison {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        MODULO;

        Comparison() {
        }

        public boolean evaluate(Integer a, Double b) {
            return evaluate(Double.valueOf(a), b);
        }
        public boolean evaluate(Integer a, Integer b) {
            return evaluate(Double.valueOf(a), Double.valueOf(b));
        }
        public boolean evaluate(Double a, Double b) {
            switch (this) {
                case EQUALS -> {
                    return a.equals(b);
                }
                case GREATER_THAN -> {
                    return a > b;
                }
                case LESS_THAN -> {
                    return a < b;
                }
                case NOT_EQUALS -> {
                    return  !a.equals(b);
                }
                case MODULO -> {
                    return a % b == 0;
                }
                default -> {
                    return false;
                }
            }
        }

    }

    public enum Operator {TICK, POSITION, BEAT, BAR, PART, RANDOM}
}
