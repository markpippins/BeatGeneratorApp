package com.angrysurfer.midi.model.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.angrysurfer.sequencer.model.Rule;
import com.angrysurfer.sequencer.util.Comparison;
import com.angrysurfer.sequencer.util.Operator;

public class RuleTests {

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void whenRuleComparedToRuleWithSameValues_thenEqualToReturnsTrue() {
      Rule a = new Rule(Operator.BAR, Comparison.EQUALS, 1.0, 0);
      Rule b = new Rule(Operator.BAR, Comparison.EQUALS, 1.0, 0);
      assertTrue(a.isEqualTo(b));
      assertTrue(b.isEqualTo(a));
    }

    @Test
    public void whenRuleComparedToRuleWithDifferentValues_thenEqualToReturnsFalse() {
      Rule a = new Rule(Operator.BAR, Comparison.MODULO, 1.0, 0);
      Rule b = new Rule(Operator.BAR, Comparison.EQUALS, 2.0, 0);
      assertTrue(!a.isEqualTo(b));
      assertTrue(!b.isEqualTo(a));
    }
}
