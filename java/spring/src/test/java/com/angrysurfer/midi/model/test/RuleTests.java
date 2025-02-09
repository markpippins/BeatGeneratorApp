// package com.angrysurfer.midi.model.test;

// import static org.junit.jupiter.api.Assertions.assertTrue;

// import org.aspectj.lang.annotation.After;
// import org.aspectj.lang.annotation.Before;
// import org.junit.jupiter.api.Test;

// import com.angrysurfer.core.model.Rule;
// import com.angrysurfer.core.util.Comparison;
// import com.angrysurfer.core.util.Operator;

// public class RuleTests {

//     @Before
//     public void setUp() {

//     }

//     @After
//     public void tearDown() {

//     }

//     @Test
//     public void whenRuleComparedToRuleWithSameValues_thenEqualToReturnsTrue() {
//       Rule a = new Rule(Operator.BAR, Comparison.EQUALS, 1.0, 0);
//       Rule b = new Rule(Operator.BAR, Comparison.EQUALS, 1.0, 0);
//       assertTrue(a.isEqualTo(b));
//       assertTrue(b.isEqualTo(a));
//     }

//     @Test
//     public void whenRuleComparedToRuleWithDifferentValues_thenEqualToReturnsFalse() {
//       Rule a = new Rule(Operator.BAR, Comparison.MODULO, 1.0, 0);
//       Rule b = new Rule(Operator.BAR, Comparison.EQUALS, 2.0, 0);
//       assertTrue(!a.isEqualTo(b));
//       assertTrue(!b.isEqualTo(a));
//     }
// }
