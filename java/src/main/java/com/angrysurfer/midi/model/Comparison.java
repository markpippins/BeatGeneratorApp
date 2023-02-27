package com.angrysurfer.midi.model;

public interface Comparison {

    static int EQUALS = 0;

    static int GREATER_THAN = 1;

    static int LESS_THAN = 2;

    static int MODULO = 3;

    public static boolean evaluate(int comparisonType, Integer a, Double b) {
        return evaluate(comparisonType, Double.valueOf(a), b);
    }

    public static boolean evaluate(int comparisonType, Integer a, Integer b) {
        return evaluate(comparisonType, Double.valueOf(a), Double.valueOf(b));
    }

    public static boolean evaluate(int comparisonType, Double a, Double b) {
        switch (comparisonType) {
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
