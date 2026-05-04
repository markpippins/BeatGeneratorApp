package com.angrysurfer.core.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Delta4Test {
    @Test
    void testRuleFromRowDelta4() {
        Object[] row = new Object[]{Rule.OPERATORS[0], Rule.COMPARISONS[0], 1.0, 0};
        Rule r = Rule.fromRow(row);
        assertNotNull(r);
        assertEquals(0, r.getOperator().intValue());
        assertEquals(0, r.getComparison().intValue());
        assertEquals(1.0, r.getValue(), 1e-6);
        assertEquals(0, r.getPart().intValue());
    }
}
