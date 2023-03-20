package com.angrysurfer.midi.model.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import org.junit.Test;

import com.angrysurfer.midi.util.SequenceRunner;
import com.angrysurfer.midi.model.Ticker;

public class SequenceRunnerTests {

    @Test
    public void whenGetMasterSequenceCalled_thenSequenceIsReturned() {

      Ticker ticker = new Ticker();
      SequenceRunner runner = new SequenceRunner(ticker);
      try {
        assertTrue(Objects.nonNull(runner.getMasterSequence()));
      } catch (InvalidMidiDataException e) {
        e.printStackTrace();
      }
    }

    @Test
    public void whenBeforeStartCalled_thenRunnerHasOpenSequencer() {

      Ticker ticker = new Ticker();
      SequenceRunner runner = new SequenceRunner(ticker);

      try {
        runner.beforeStart();
      } catch (InvalidMidiDataException | MidiUnavailableException e) {
        e.printStackTrace();
      }

      assertTrue(runner.getSequencer().isOpen());
    }

    

    // @Test
    // public void whenRunCalled_thenSequencerIsRunning() {

    //   Ticker ticker = new Ticker();
    //   SequenceRunner runner = new SequenceRunner(ticker);
    //   new Thread(runner).start();
    //   // assertTrue(ticker.isPlaying());
    //   // runner.stop();
    // }
}
