package com.angrysurfer.midi.model.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.Ticker;

public class PlayerTests {

    @Test
    public void whenX_thenY() {
      Player p = new Player(){
        
        Set<Rule> ruleSet = new HashSet<>();

        @Override
        public void onTick(long tick, int bar) {
        }

        @Override
        public Set<Rule> getRules() {
          return this.ruleSet;
        }

        @Override
        public void setRules(Set<Rule> rules) {
          this.ruleSet = rules;
        }

        @Override
        public Ticker getTicker() {
          // TODO Auto-generated method stub
          throw new UnsupportedOperationException("Unimplemented method 'getTicker'");
        }

        @Override
        public void setTicker(Ticker ticker) {
          // TODO Auto-generated method stub
          throw new UnsupportedOperationException("Unimplemented method 'setTicker'");
        }};
      


        assertTrue(1 > 0);
    }
}
