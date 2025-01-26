package com.angrysurfer.sequencer.util;

public interface Comparison {

    static int EQUALS = 0;

    static int GREATER_THAN = 1;

    static int LESS_THAN = 2;

    static int MODULO = 3;

    static int NOT_EQUAL_TO = 4;

    public static boolean evaluate(int comparisonType, Long a, Double b) {
        return evaluate(comparisonType, Double.valueOf(a), b);
    }

    public static boolean evaluate(int comparisonType, Integer a, Double b) {
        return evaluate(comparisonType, Long.valueOf(a), b);
    }

    public static boolean evaluate(int comparisonType, Long a, Long b) {
        return evaluate(comparisonType, Double.valueOf(a), Double.valueOf(b));
    }


    public static boolean evaluate(int comparisonType, Double a, Double b) {
        
        boolean result = false;

        switch (comparisonType) {
            case EQUALS -> {
                result =  a.equals(b);
                break;
            }
            case GREATER_THAN -> {
                result = a > b;
                break;
            }
            case LESS_THAN -> {
                result =  a < b;
                break;
            }
            case MODULO -> {
                result =  a % b == 0;
                break;
            }
            default -> {
                result = false;
            }
        }

        return result;
    }

}
