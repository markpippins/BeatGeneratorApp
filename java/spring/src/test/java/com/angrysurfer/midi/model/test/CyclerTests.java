// package com.angrysurfer.midi.model.test;

// import static org.junit.jupiter.api.Assertions.assertTrue;

// import java.util.stream.IntStream;
// import java.util.stream.LongStream;

// import org.aspectj.lang.annotation.After;
// import org.aspectj.lang.annotation.Before;
// import org.junit.jupiter.api.Test;

// import com.angrysurfer.core.util.Cycler;

// public class CyclerTests {

//     @Before
//     public void setUp() {

//     }

//     @After
//     public void tearDown() {

//     }

//     @Test
//     public void whenGetCalledOnUninitializedCycler_thenGetAlwaysReturnsOne() {
//         Cycler c = new Cycler();
//         IntStream.range(1, 1000).forEach(i -> assertTrue(c.get() == 1));
//     }

//     @Test
//     public void whenGetCalledOnCyclerWithLengthOfOne_thenGetAlwaysReturnsOne() {
//         Cycler c = new Cycler(1);
//         IntStream.range(1, 1000).forEach(i -> assertTrue(c.get() == 1));
//     }

//     @Test
//     public void whenAdvanceCalledOnUninitializedCyclerWithRunLinearTrue_thenCyclerRunsLinearly() {
//         Cycler c = new Cycler(0);
//         IntStream.range(1, 1000).forEach(i -> assertTrue(c.advance() == i + 1));
//     }

//     @Test
//     public void whenAdvanceCalledOnCyclerWithLengthOfOne_thenAdvanceAlwaysReturnsOne() {
//         Cycler c = new Cycler(1);
//         IntStream.range(1, 1000).forEach(i -> assertTrue(c.advance() == 1));
//     }

//     @Test
//     public void whenAdvanceCalledLengthTimes_thenAdvanceReturnsOne() {
//         Cycler c = new Cycler(30);
//         LongStream.range(1, c.getLength()).forEach(i -> assertTrue(c.advance() == i + 1));
//         assertTrue(c.advance() == 1);
//     }

//     @Test
//     public void whenAdvanceCalledNTimesLengthTimes_thenGetReturnsOne() {
//         Cycler c = new Cycler(4);
//         IntStream.range(1, 17).forEach(i -> c.advance());
//         assertTrue(c.get() == 1);
//     }

//     // public static void main(String[] args) {
//     // Cycler cycler = new Cycler(16);
//     // IntStream.range(0, 32).forEach(i ->
//     logger.info(String.format("%s - %s", cycler.advance(),
//     // cycler.getPosition())));

//     // Cycler cycler2 = new Cycler(16);
//     // cycler2.stepSize = .5;
//     // IntStream.range(0, 16).forEach(i ->
//     logger.info(String.format("%s - %s", cycler2.advance(),
//     // cycler2.getPosition())));

//     // Cycler cycler3 = new Cycler(16);
//     // cycler3.stepSize = .25;
//     // IntStream.range(0, 64).forEach(i ->
//     logger.info(String.format("%s - %s", cycler3.advance(), cycler3.get())));
//     // }
// }