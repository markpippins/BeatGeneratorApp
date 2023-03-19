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
    public void whenPPQ_thenOnTickCalledFirstBeat() {

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

}
