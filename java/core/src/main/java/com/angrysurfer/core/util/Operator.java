package com.angrysurfer.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Operator {

    static int EQUALS = 0;

    static int GREATER_THAN = 1;

    static int LESS_THAN = 2;

    static int MODULO = 3;

    static int NOT_EQUAL_TO = 4;

    static Logger logger = LoggerFactory.getLogger(Operator.class);

    public static boolean evaluate(int operatorTypeType, Long a, Double b) {
        return evaluate(operatorTypeType, Double.valueOf(a), b);
    }

    public static boolean evaluate(int operatorTypeType, Integer a, Double b) {
        return evaluate(operatorTypeType, Long.valueOf(a), b);
    }

    public static boolean evaluate(int operatorTypeType, Long a, Long b) {
        return evaluate(operatorTypeType, Double.valueOf(a), Double.valueOf(b));
    }


    public static boolean evaluate(int operatorTypeType, Double a, Double b) {
        logger.info("evaluate() - operatorTypeType: {}, a: {}, b: {}", operatorTypeType, a, b);
        
        boolean result = false;

        switch (operatorTypeType) {
            case EQUALS -> {
                result = a.equals(b);
                logger.debug("EQUALS operatorType result: {}", result);
                break;
            }
            case GREATER_THAN -> {
                result = a > b;
                logger.debug("GREATER_THAN operatorType result: {}", result);
                break;
            }
            case LESS_THAN -> {
                result = a < b;
                logger.debug("LESS_THAN operatorType result: {}", result);
                break;
            }
            case MODULO -> {
                result = a % b == 0;
                logger.debug("MODULO operatorType result: {}", result);
                break;
            }
            default -> {
                result = false;
                logger.warn("Unknown operatorType type: {}", operatorTypeType);
            }
        }

        return result;
    }

}
