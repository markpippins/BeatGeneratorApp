// package com.angrysurfer.core.engine;

// import java.util.ArrayList;
// import java.util.Objects;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import com.angrysurfer.core.api.db.FindOne;
// import com.angrysurfer.core.api.db.Save;
// import com.angrysurfer.core.model.Ticker;
// import com.angrysurfer.core.util.ClockSource;
// import com.angrysurfer.core.util.update.TickerUpdateType;

// import lombok.Getter;
// import lombok.Setter;

// @Getter
// @Setter
// public class TickerEngine {

//     static Logger logger = LoggerFactory.getLogger(TickerEngine.class.getCanonicalName());

//     private Ticker ticker;
//     private ClockSource clockSource;
//     private ArrayList<ClockSource> clocks = new ArrayList<>();

//     private Long lastTickId;

//     public TickerEngine() {
//         setTicker(newTicker());
//     }

//     public Ticker getNewTicker(Save<Ticker> tickerSaver) {

//         if (Objects.isNull(ticker)) {
//             stopRunningClocks();
//             ticker = tickerSaver.save(newTicker());
//             newClockSource(ticker);
//         }

//         return ticker;
//     }

//     // public void play() {

//     // stopRunningClocks();

//     // //
//     // getClockSource().getCycleListeners().add(getSongService().getTickListener());
//     // // getSongService().getSong().setBeatDuration(ticker.getBeatDuration());
//     // // getSongService().getSong().setTicksPerBeat(ticker.getTicksPerBeat());

//     // List<MidiDevice> devices = MIDIEngine.getMidiOutDevices();

//     // getTicker().getPlayers().forEach(p -> {

//     // Instrument instrument = p.getInstrument();
//     // if (Objects.nonNull(instrument)) {
//     // Optional<MidiDevice> device = devices.stream()
//     // .filter(d ->
//     // d.getDeviceInfo().getName().equals(instrument.getDeviceName())).findFirst();

//     // if (device.isPresent() && !device.get().isOpen())
//     // try {
//     // device.get().open();
//     // instrument.setDevice(device.get());
//     // } catch (MidiUnavailableException e) {
//     // logger.error(e.getMessage(), e);
//     // }

//     // else
//     // logger.error(instrument.getDeviceName() + " not initialized");
//     // } else
//     // logger.error("Instrument not initialized");
//     // });

//     // new Thread(newClockSource(getTicker())).start();

//     // ticker.getPlayers().forEach(p -> {
//     // try {
//     // if (p.getPreset() > -1)
//     // p.getInstrument().programChange(p.getChannel(), p.getPreset(), 0);
//     // } catch (InvalidMidiDataException | MidiUnavailableException e) {
//     // logger.error(e.getMessage(), e);
//     // }
//     // });

//     // }

//     private ClockSource newClockSource(Ticker ticker) {
//         stopRunningClocks();
//         clockSource = new ClockSource(ticker);
//         clocks.add(clockSource);
//         ticker.setClockSource(clockSource);
//         return clockSource;
//     }

//     private void stopRunningClocks() {
//         clocks.forEach(sr -> sr.stop());
//         clocks.clear();
//     }

//     public void pause() {
//         stopRunningClocks();
//     }

//     public Ticker updateTicker(Ticker ticker, int updateType, long updateValue) {

//         switch (updateType) {
//             case TickerUpdateType.PPQ:
//                 ticker.setTicksPerBeat((int) updateValue);
//                 break;

//             case TickerUpdateType.BEATS_PER_BAR:
//                 ticker.setBeatsPerBar((int) updateValue);
//                 break;

//             case TickerUpdateType.BPM:
//                 ticker.setTempoInBPM(Float.valueOf(updateValue));
//                 if (Objects.nonNull(ticker.getClockSource()) &&
//                         ticker.getId().equals(ticker.getId()))
//                     ticker.getClockSource().setTempoInBPM(updateValue);
//                 // getSongService().getSong().setTicksPerBeat(ticker.getTicksPerBeat());
//                 break;

//             case TickerUpdateType.PARTS:
//                 ticker.setParts((int) updateValue);
//                 ticker.getPartCycler().reset();
//                 break;

//             case TickerUpdateType.BASE_NOTE_OFFSET:
//                 ticker.setNoteOffset((double) ticker.getNoteOffset() + updateValue);
//                 break;

//             case TickerUpdateType.BARS:
//                 ticker.setBars((int) updateValue);
//                 break;

//             case TickerUpdateType.PART_LENGTH:
//                 ticker.setPartLength(updateValue);
//                 break;

//             case TickerUpdateType.MAX_TRACKS:
//                 ticker.setMaxTracks((int) updateValue);
//                 break;
//         }

//         // return getTickerRepo().save(ticker);
//         return ticker;
//     }

//     public Ticker loadTicker(long tickerId, FindOne<Ticker> tickerFindById) {
//         stopRunningClocks();
//         return tickerFindById.find(tickerId).orElseThrow();
//     }

//     public Ticker newTicker() {
//         stopRunningClocks();
//         Ticker ticker = new Ticker();
//         newClockSource(ticker);
//         setTicker(ticker);
//         return ticker;
//     }
// }