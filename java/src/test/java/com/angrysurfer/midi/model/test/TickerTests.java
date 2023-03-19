package com.angrysurfer.midi.model.test;

import com.angrysurfer.midi.repo.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.Test;

import com.angrysurfer.midi.model.Ticker;
public class TickerTests {

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
      IntStream.range(0, ticker.getBeatsPerBar() * ticker.getTicksPerBeat()).forEach(i -> {
        ticker.beforeTick();
        ticker.afterTick();
      });

      ticker.afterEnd();
      assertEquals(expectedBar, bar.get());
    }

    @Test
    public void whenTime_thenPartChanges() {

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

      int expectedPart = 2;

      ticker.beforeStart();
      
      // 
      IntStream.range(0, 16 * ticker.getBeatsPerBar() * ticker.getTicksPerBeat()).forEach(i -> {
        ticker.beforeTick();
        ticker.afterTick();
      });

    //   ticker.afterEnd();
      assertEquals(expectedPart, ticker.getPart());
    }

    @Test
    public void whenTime_thenPartCyclesBackToOne() {

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
      ticker.setPartLength(4);

      int expectedPart = 1;

      ticker.beforeStart();
      
      // 
      IntStream.range(0, 16 * ticker.getBeatsPerBar() * ticker.getTicksPerBeat()).forEach(i -> {
        ticker.beforeTick();
        ticker.afterTick();
      });

    //   ticker.afterEnd();
      assertEquals(expectedPart, ticker.getPart());
    }

}
