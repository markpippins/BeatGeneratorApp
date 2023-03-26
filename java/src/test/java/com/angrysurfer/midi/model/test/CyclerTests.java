package com.angrysurfer.midi.model.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.angrysurfer.midi.util.Cycler;

public class CyclerTests {

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void whenGetCalledOnUninitializedCycler_thenGetAlwaysReturnsOne() {
      Cycler c = new Cycler();
      IntStream.range(1, 1000).forEach(i -> assertTrue(c.get() == 1));
    }

    @Test
    public void whenGetCalledOnCyclerWithLengthOfOne_thenGetAlwaysReturnsOne() {
      Cycler c = new Cycler(1);
      IntStream.range(1, 1000).forEach(i -> assertTrue(c.get() == 1));
    }

    @Test
    public void whenAdvanceCalledOnUninitializedCycler_thenCyclerRunsLinearly() {
      Cycler c = new Cycler();
      IntStream.range(1, 1000).forEach(i -> assertTrue(c.advance() == i + 1));
    }

    @Test
    public void whenAdvanceCalledOnCyclerWithLengthOfOne_thenAdvanceAlwaysReturnsOne() {
      Cycler c = new Cycler(1);
      IntStream.range(1, 1000).forEach(i -> assertTrue(c.advance() == 1));
    }


    @Test
    public void whenAdvanceCalledLengthTimes_thenAdvanceReturnsOne() {
      Cycler c = new Cycler(30);
      LongStream.range(1,c.getLength()).forEach(i -> assertTrue(c.advance() == i + 1));
      assertTrue(c.advance() == 1);
    }

    @Test
    public void whenAdvanceCalledNTimesLengthTimes_thenGetReturnsOne() {
      Cycler c = new Cycler(4);
      IntStream.range(1, 17).forEach(i -> c.advance());
      assertTrue(c.get() == 1);
    }


}
