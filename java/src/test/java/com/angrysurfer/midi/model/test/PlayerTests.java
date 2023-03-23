package com.angrysurfer.midi.model.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import org.junit.Test;

import com.angrysurfer.midi.util.Comparison;
import com.angrysurfer.midi.util.Operator;
import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.Ticker;

public class PlayerTests {

  ExecutorService executor = Executors.newFixedThreadPool(16);

  @Test
  public void whenRuleExistsForFirstBeat_thenOnTickCalledFirstBeat() {


    AtomicBoolean play = new AtomicBoolean(false);
    Player p = new Player() {
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

    Ticker ticker = new Ticker() {
      @Override
      public double getBeat() { 
        return 1.0;
      }
    };

    ticker.getBarCycler().reset();
    ticker.getBeatCycler().reset();

    Player p1 = new Player() {
      @Override
      public void onTick(long tick, long bar) {
        play1.set(true);
      }

    };
    p1.setTicker(ticker);

    Player p2 = new Player() {
      @Override
      public void onTick(long tick, long bar) {
        play2.set(true);
      }

    };
    p2.setTicker(ticker);

    Player p3 = new Player() {
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
