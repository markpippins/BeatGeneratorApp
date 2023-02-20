package com.angrysurfer.midi.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Eval implements Serializable {

    public Eval() {

    }

    public Eval(Operator operator, Comparison comparison, Double value) {
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

        public boolean evaluate(Double a, Double b) {
            boolean result;
            switch (this) {
                case EQUALS -> result = a.equals(b);
                case GREATER_THAN -> result = a > b;
                case LESS_THAN -> result = a < b;
                case NOT_EQUALS -> result = !a.equals(b);
                case MODULO -> result = a % b == 0;
                default -> result = false;
            }

            return result;
        }

        public boolean evaluate(Integer a, Double b) {
            boolean result = false;

            switch (this) {
                case EQUALS -> result = a.equals(b.intValue());
                case GREATER_THAN -> result = a > b;
                case LESS_THAN -> result = a < b;
                case NOT_EQUALS -> result = !a.equals(b.intValue());
                case MODULO -> result = a % b == 0;
            }

            return result;
        }

        public boolean evaluate(Integer a, Integer b) {
            boolean result;
            switch (this) {
                case EQUALS -> result = a.equals(b);
                case GREATER_THAN -> result = a > b;
                case LESS_THAN -> result = a < b;
                case NOT_EQUALS -> result = !a.equals(b);
                case MODULO -> result = a % b == 0;
                default -> result = false;
            }

            return result;
        }
    }

    public enum Operator {TICK, POSITION, BEAT, BAR, PART, RANDOM}
}
