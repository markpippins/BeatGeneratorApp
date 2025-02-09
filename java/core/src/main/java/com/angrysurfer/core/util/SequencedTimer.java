package com.angrysurfer.core.util;
// package com.angrysurfer.sequencer.util;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Objects;

// import javax.sound.midi.InvalidMidiDataException;
// import javax.sound.midi.MetaEventListener;
// import javax.sound.midi.MetaMessage;
// import javax.sound.midi.MidiEvent;
// import javax.sound.midi.MidiMessage;
// import javax.sound.midi.MidiSystem;
// import javax.sound.midi.MidiUnavailableException;
// import javax.sound.midi.Receiver;
// import javax.sound.midi.Sequence;
// import javax.sound.midi.Sequencer;
// import javax.sound.midi.ShortMessage;
// import javax.sound.midi.Sequencer.SyncMode;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import com.angrysurfer.sequencer.model.Ticker;

// import lombok.Getter;
// import lombok.Setter;

// @Getter
// @Setter
// public class SequencedTimer implements Timer {

//     static Logger logger = LoggerFactory.getLogger(SequencedTimer.class.getCanonicalName());

//     static Sequencer sequencer;

//     private volatile boolean running = true;

//     static {
//         try {
//             sequencer = MidiSystem.getSequencer();
//         } catch (MidiUnavailableException e) {
//             logger.error(e.getMessage(), e);
//             throw new RuntimeException(e);
//         }
//     }

//     private Ticker ticker;

//     private float pendingBpm = -1;

//     private float pendingTempoFactor = -1;

//     private float pendingMpq = -1;

//     // private final AtomicInteger pendingPpq = new AtomicInteger(-1);

//     private List<Runnable> tickListeners = new ArrayList<>();

//     private List<Runnable> beatListeners = new ArrayList<>();

//     private int bpm;

//     // private int ppq;

//     public SequencedTimer(Ticker ticker) {
//         this.ticker = ticker;
//         try {
//             Sequence master = getMasterSequence();
//             sequencer.setSequence(master);
//             sequencer.setLoopCount(getTicker().getLoopCount());
//             sequencer.setTempoInBPM(300);
//             sequencer.open();
//         } catch (InvalidMidiDataException | MidiUnavailableException e) {
//             e.printStackTrace();
//         }
//     }

//     @Override
//     public void run() {

//         long tickNum = 0;
//         sequencer.start();

//         while (running) {
//             tickNum++;
//             logger.info("tick " + Long.toString(tickNum));
//             // Check for pending changes at beat boundaries
//             if (tickNum % ticker.getTicksPerBeat() == 0) {
//                 float newBpm = pendingBpm;
//                 if (newBpm > 0) {
//                     sequencer.setTempoInBPM(newBpm);
//                     pendingBpm = -1;
//                 }

//                 float newTempoFactor = pendingTempoFactor;
//                 if (newBpm > 0) {
//                     sequencer.setTempoFactor(newTempoFactor);
//                     pendingTempoFactor = -1;
//                 }

//                 float newMpq = pendingMpq;
//                 if (pendingMpq > 0) {
//                     sequencer.setTempoInMPQ(newMpq);
//                     pendingMpq = -1;
//                 }

//                 beatListeners.forEach(Runnable::run);
//             }

//             tickListeners.forEach(Runnable::run);
            
//             Thread.yield();
//         }

//         sequencer.close();
//         getTicker().afterEnd();
//     }

//     public Sequence getMasterSequence() throws InvalidMidiDataException {
//         Sequence sequence = new Sequence(Sequence.PPQ, getTicker().getTicksPerBeat());
//         sequence.createTrack().add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, 0, 0),
//                 ticker.getTicksPerBeat() * ticker.getBeatsPerBar() * 4 * 1000));
//         return sequence;
//     }

//     @Override
//     public void addTickListener(Runnable listener) {
//         this.tickListeners.add(listener);
//     }

//     @Override
//     public void addBeatListener(Runnable listener) {
//         this.beatListeners.add(listener);
//     }

//     @Override
//     public void stop() {
//         if (Objects.nonNull(sequencer) && sequencer.isRunning())
//             sequencer.stop();

//         getTicker().setPaused(false);
//         getTicker().getBeatCycler().reset();
//         getTicker().getBarCycler().reset();
//     }

//     @Override
//     public synchronized void setBpm(float newBpm) {
//         if (Objects.nonNull(sequencer))
//             sequencer.setTempoInBPM((float) newBpm);

//         pendingBpm = newBpm;
//     }

//     @Override
//     public float getBpm() {
//         if (Objects.nonNull(sequencer))
//             return sequencer.getTempoInBPM();

//         return Constants.DEFAULT_BPM;
//     }

//     @Override
//     public synchronized void setTempoInMPQ(float mpq) {
//         if (Objects.nonNull(sequencer))
//             sequencer.setTempoInMPQ(mpq);
//     }

//     @Override
//     public float getTempoInMPQ() {

//         if (Objects.nonNull(sequencer))
//             return sequencer.getTempoInMPQ();

//         return Constants.DEFAULT_TEMPO_IN_MPQ;
//     }

//     @Override
//     public synchronized void setTempoFactor(float factor) {
//         if (Objects.nonNull(sequencer))
//             sequencer.setTempoFactor(factor);
//     }

//     @Override
//     public float getTempoFactor() {
//         if (Objects.nonNull(sequencer))
//             return sequencer.getTempoFactor();

//         return Constants.DEFAULT_TEMPO_FACTOR;
//     }

//     @Override
//     public long getCurrentTick() {
//         return Objects.nonNull(sequencer) && sequencer.isRunning() ? sequencer.getTickPosition() : 0;
//     }

// }
