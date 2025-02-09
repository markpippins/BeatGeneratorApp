package com.angrysurfer.midi.model.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Ticker;

public class TickerTests {

  
  static Logger logger = LoggerFactory.getLogger(TickerTests.class.getCanonicalName());

    @Test
    public void whenTime_thenBarChanges() {

      AtomicInteger bar = new AtomicInteger(1);
      Ticker ticker = new Ticker() {
        @Override
        public void onBarChange() {
            super.onBarChange();
            bar.incrementAndGet();
        }
      };

      ticker.setBeatsPerBar(4);
      ticker.setTicksPerBeat(4);
      ticker.setPartLength(16);

      int expectedBar = 2;

      ticker.beforeStart();
      
      // 
      LongStream.range(0, ticker.getBeatsPerBar() * ticker.getTicksPerBeat()).forEach(i -> {
        ticker.beforeTick();
        ticker.afterTick();
      });

      // ticker.afterEnd();
      assertEquals(expectedBar, bar.get());
    }

    @Test
    public void whenTime_thenPartChanges() {

      Ticker ticker = new Ticker();

      ticker.setParts(4);
      ticker.setBeatsPerBar(4);
      ticker.setTicksPerBeat(1);
      ticker.setPartLength(4);

      int expectedPart = 1;

      ticker.beforeStart();
      for (int i = 0; i < 8; i++) {
        ticker.beforeTick();
        logger.info(String.format("%s", ticker.getPart()));
        ticker.afterTick();
      };

    //   ticker.afterEnd();
      assertEquals(expectedPart, ticker.getPart());
    }

    // @Test
    // public void whenTime_thenPartCyclerChanges() {

    //   Ticker ticker = new Ticker();

    //   ticker.setBeatsPerBar(4);
    //   ticker.setTicksPerBeat(4);
    //   ticker.setPartLength(16);

    //   int expectedPart = 2;

    //   ticker.beforeStart();
      
    //   // 
    //   IntStream.range(0, 16 * ticker.getBeatsPerBar() * ticker.getTicksPerBeat()).forEach(i -> {
    //     ticker.beforeTick();
    //     ticker.afterTick();
    //   });

    // //   ticker.afterEnd();
    //   assertEquals(expectedPart, ticker.getPartCycler().get());
    // }
    
    @Test
    public void whenTime_thenPartCyclesBackToOne() {

      Ticker ticker = new Ticker();

      ticker.setBeatsPerBar(4);
      ticker.setTicksPerBeat(4);
      ticker.setPartLength(4);

      int expectedPart = 1;
      ticker.beforeStart();

      LongStream.range(0, 16 * ticker.getBeatsPerBar() * ticker.getTicksPerBeat()).forEach(i -> {
        ticker.beforeTick();
        ticker.afterTick();
      });

      assertEquals(expectedPart, ticker.getPart());
    }

}
