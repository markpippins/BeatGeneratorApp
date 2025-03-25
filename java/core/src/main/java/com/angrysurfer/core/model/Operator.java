package com.angrysurfer.core.model;

public class Operator {
    public static final int EQUALS = 0;
    public static final int GREATER_THAN = 1;
    public static final int LESS_THAN = 2;
    public static final int MODULO = 3;
    public static final int NOT_EQUAL_TO = 4;

    public static boolean evaluate(int comparison, long a, double b) {
        return evaluate(comparison, (double) a, b);
    }

    public static boolean evaluate(int comparison, int a, double b) {
        return evaluate(comparison, (double) a, b);
    }

    public static boolean evaluate(int comparison, long a, long b) {
        return evaluate(comparison, (double) a, (double) b);
    }

    public static boolean evaluate(int comparison, double a, double b) {
        boolean result = false;
        switch (comparison) {
            case EQUALS -> result = Math.abs(a - b) < 0.00001;
            case GREATER_THAN -> result = a > b;
            case LESS_THAN -> result = a < b;
            case MODULO -> {
                if (b == 0) return false;
                result = Math.abs(a % b) < 0.00001;
            }
            case NOT_EQUAL_TO -> result = Math.abs(a - b) >= 0.00001;
            default -> result = false;
        }
        return result;
    }
}
