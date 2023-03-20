package com.angrysurfer.midi.model.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.angrysurfer.midi.util.Comparison;
import com.angrysurfer.midi.util.Operator;
import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.Ticker;

public class PlayerTests {

    @Test
     public void whenRuleExistsForFirstBeat_thenOnTickCalledFirstBeat() {

      AtomicBoolean play = new AtomicBoolean(false);

      Player p = new Player() {

        Ticker ticker = new Ticker();

        @Override
        public Ticker getTicker() {
          return ticker;
        }

        @Override
        public void setTicker(Ticker ticker) {
          this.ticker = ticker;
        }

        @Override
        public void onTick(long tick, int bar) {
          play.set(true);
        }
   
      };

      Rule r = new Rule(Operator.BEAT, Comparison.EQUALS, 1.0);
      p.getRules().add(r);
      p.call();
      assertTrue(play.get());
      play.set(false);
      p.call();
      assertTrue(!play.get());
    }
}
