package com.angrysurfer.midi.model.condition;

public enum Comparison {
    EQUALS(0),
    GREATER_THAN(1),
    LESS_THAN(2),

    MODULO(3),

    RANDOM(4);

    Comparison(int i) {
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
            case MODULO -> {
                return a % b == 0;
            }
            default -> {
                return false;
            }
        }
    }

}
