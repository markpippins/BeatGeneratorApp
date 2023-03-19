package com.angrysurfer.midi.model.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.angrysurfer.midi.model.Comparison;
import com.angrysurfer.midi.model.Operator;
import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.Ticker;

public class PlayerTests {

    @Test
     public void whenRuleExistsForFirstBeat_thenOnTickCalledFirstBeat() {

      boolean[] play = {false};

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
          play[0] = true;
        }
   
      };

      Rule r = new Rule(Operator.BEAT, Comparison.EQUALS, 1.0);
      p.getRules().add(r);
      p.call();
      assertTrue(play[0]);
      play[0] = false;
      p.call();
      assertTrue(!play[0]);
    }
}
