package com.angrysurfer.midi.model.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.angrysurfer.core.api.IPlayer;
import com.angrysurfer.core.api.ITicker;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.core.util.Comparison;
import com.angrysurfer.core.util.Operator;

public class PlayerTests {

  ExecutorService executor = Executors.newFixedThreadPool(16);

  @Test
  public void whenRuleExistsForFirstBeat_thenOnTickCalledFirstBeat() {

    AtomicBoolean play = new AtomicBoolean(false);
    IPlayer p = new Strike() {
      @Override
      public void onTick(long tick, long bar) {
        play.set(true);
      }
    };
    p.setTicker(new Ticker());

    Rule r = new Rule(Operator.BEAT, Comparison.EQUALS, 1.0, 0);
    p.getRules().add(r);
    p.call();
    assertTrue(play.get());
    play.set(false);
    p.call();
    assertTrue(!play.get());
  }

  @Test
  public void whenRuleExistsForFirstBeat_thenOnTickForAllPlayersCalledFirstBeat() {

    AtomicBoolean play1 = new AtomicBoolean(false);
    AtomicBoolean play2 = new AtomicBoolean(false);
    AtomicBoolean play3 = new AtomicBoolean(false);

    ITicker ticker = new Ticker() {
      @Override
      public double getBeat() {
        return 1.0;
      }
    };

    ticker.getBarCycler().reset();
    ticker.getBeatCycler().reset();

    IPlayer p1 = new Strike() {
      @Override
      public void onTick(long tick, long bar) {
        play1.set(true);
      }

    };
    p1.setTicker(ticker);

    IPlayer p2 = new Strike() {
      @Override
      public void onTick(long tick, long bar) {
        play2.set(true);
      }

    };
    p2.setTicker(ticker);

    IPlayer p3 = new Strike() {
      @Override
      public void onTick(long tick, long bar) {
        play3.set(true);
      }
    };
    p3.setTicker(ticker);

    p1.getRules().add(new Rule(Operator.BEAT, Comparison.EQUALS, 1.0, 0));
    p2.getRules().add(new Rule(Operator.BEAT, Comparison.EQUALS, 1.0, 0));
    p3.getRules().add(new Rule(Operator.BEAT, Comparison.EQUALS, 1.0, 0));

    try {
      executor.invokeAll(List.of(p1, p2, p3));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertTrue(play1.get());
    assertTrue(play2.get());
    assertTrue(play3.get());

  }
}
