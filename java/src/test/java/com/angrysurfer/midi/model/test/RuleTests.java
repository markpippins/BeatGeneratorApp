package com.angrysurfer.midi.model.test;

import com.angrysurfer.midi.repo.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.angrysurfer.midi.model.Comparison;
import com.angrysurfer.midi.model.Operator;
import com.angrysurfer.midi.model.Rule;

public class RuleTests {

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void whenRuleComparedToRuleWithSameValues_thenEqualToReturnsTrue() {
      Rule a = new Rule(Operator.BAR, Comparison.EQUALS, 1.0);
      Rule b = new Rule(Operator.BAR, Comparison.EQUALS, 1.0);
      assertTrue(a.isEqualTo(b));
      assertTrue(b.isEqualTo(a));
    }

    @Test
    public void whenRuleComparedToRuleWithDifferentValues_thenEqualToReturnsFalse() {
      Rule a = new Rule(Operator.BAR, Comparison.MODULO, 1.0);
      Rule b = new Rule(Operator.BAR, Comparison.EQUALS, 2.0);
      assertTrue(!a.isEqualTo(b));
      assertTrue(!b.isEqualTo(a));
    }
}
