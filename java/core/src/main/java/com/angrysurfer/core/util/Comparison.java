package com.angrysurfer.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Comparison {

    static int EQUALS = 0;

    static int GREATER_THAN = 1;

    static int LESS_THAN = 2;

    static int MODULO = 3;

    static int NOT_EQUAL_TO = 4;

    static Logger logger = LoggerFactory.getLogger(Comparison.class);

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
        logger.info("evaluate() - comparisonType: {}, a: {}, b: {}", comparisonType, a, b);
        
        boolean result = false;

        switch (comparisonType) {
            case EQUALS -> {
                result = a.equals(b);
                logger.debug("EQUALS comparison result: {}", result);
                break;
            }
            case GREATER_THAN -> {
                result = a > b;
                logger.debug("GREATER_THAN comparison result: {}", result);
                break;
            }
            case LESS_THAN -> {
                result = a < b;
                logger.debug("LESS_THAN comparison result: {}", result);
                break;
            }
            case MODULO -> {
                result = a % b == 0;
                logger.debug("MODULO comparison result: {}", result);
                break;
            }
            default -> {
                result = false;
                logger.warn("Unknown comparison type: {}", comparisonType);
            }
        }

        return result;
    }

}
