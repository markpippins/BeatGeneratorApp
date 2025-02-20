package com.angrysurfer.core.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.db.FindOne;
import com.angrysurfer.core.api.db.FindSet;
import com.angrysurfer.core.api.db.Next;
import com.angrysurfer.core.api.db.Prior;
import com.angrysurfer.core.api.db.Save;
import com.angrysurfer.core.model.Instrument;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.player.Strike;
import com.angrysurfer.core.util.ClockSource;
import com.angrysurfer.core.util.update.TickerUpdateType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerEngine {

    static Logger logger = LoggerFactory.getLogger(TickerEngine.class.getCanonicalName());

    private Ticker ticker;
    private ClockSource clockSource;
    private ArrayList<ClockSource> clocks = new ArrayList<>();

    private Long lastTickId;

    public TickerEngine() {
        setTicker(newTicker());
    }

    public Ticker getNewTicker(Save<Ticker> tickerSaver) {

        if (Objects.isNull(ticker)) {
            stopRunningClocks();
            ticker = tickerSaver.save(newTicker());
            newClockSource(ticker);
        }

        return ticker;
    }

    public void play() {

        stopRunningClocks();

        // getClockSource().getCycleListeners().add(getSongService().getTickListener());
        // getSongService().getSong().setBeatDuration(ticker.getBeatDuration());
        // getSongService().getSong().setTicksPerBeat(ticker.getTicksPerBeat());

        List<MidiDevice> devices = MIDIEngine.getMidiOutDevices();

        getTicker().getPlayers().forEach(p -> {

            Instrument instrument = p.getInstrument();
            if (Objects.nonNull(instrument)) {
                Optional<MidiDevice> device = devices.stream()
                        .filter(d -> d.getDeviceInfo().getName().equals(instrument.getDeviceName())).findFirst();

                if (device.isPresent() && !device.get().isOpen())
                    try {
                        device.get().open();
                        instrument.setDevice(device.get());
                    } catch (MidiUnavailableException e) {
                        logger.error(e.getMessage(), e);
                    }

                else
                    logger.error(instrument.getDeviceName() + " not initialized");
            } else
                logger.error("Instrument not initialized");
        });

        new Thread(newClockSource(getTicker())).start();

        ticker.getPlayers().forEach(p -> {
            try {
                if (p.getPreset() > -1)
                    p.getInstrument().programChange(p.getChannel(), p.getPreset(), 0);
            } catch (InvalidMidiDataException | MidiUnavailableException e) {
                logger.error(e.getMessage(), e);
            }
        });

    }

    private ClockSource newClockSource(Ticker ticker) {
        stopRunningClocks();
        clockSource = new ClockSource(ticker);
        clocks.add(clockSource);
        ticker.setClockSource(clockSource);
        return clockSource;
    }

    private void stopRunningClocks() {
        clocks.forEach(sr -> sr.stop());
        clocks.clear();
    }

    public void stop() {
        stopRunningClocks();
        getTicker().setPaused(false);
        getTicker().getBeatCycler().reset();
        getTicker().getBarCycler().reset();
        getTicker().setDone(false);
        getTicker().reset();
    }

    public void pause() {
        stopRunningClocks();
    }

    public Ticker updateTicker(Ticker ticker, int updateType, long updateValue) {

        switch (updateType) {
            case TickerUpdateType.PPQ:
                ticker.setTicksPerBeat((int) updateValue);
                break;

            case TickerUpdateType.BEATS_PER_BAR:
                ticker.setBeatsPerBar((int) updateValue);
                break;

            case TickerUpdateType.BPM:
                ticker.setTempoInBPM(Float.valueOf(updateValue));
                if (Objects.nonNull(ticker.getClockSource()) &&
                        ticker.getId().equals(ticker.getId()))
                    ticker.getClockSource().setTempoInBPM(updateValue);
                // getSongService().getSong().setTicksPerBeat(ticker.getTicksPerBeat());
                break;

            case TickerUpdateType.PARTS:
                ticker.setParts((int) updateValue);
                ticker.getPartCycler().reset();
                break;

            case TickerUpdateType.BASE_NOTE_OFFSET:
                ticker.setNoteOffset((double) ticker.getNoteOffset() + updateValue);
                break;

            case TickerUpdateType.BARS:
                ticker.setBars((int) updateValue);
                break;

            case TickerUpdateType.PART_LENGTH:
                ticker.setPartLength(updateValue);
                break;

            case TickerUpdateType.MAX_TRACKS:
                ticker.setMaxTracks((int) updateValue);
                break;
        }

        // return getTickerRepo().save(ticker);
        return ticker;
    }

    public synchronized Ticker next(long currentTickerId, Long maxTickerId, Next<Ticker> tickerNav,
            FindSet<Strike> tickerPlayerFinder, FindSet<Rule> playerRuleFinder, Save<Ticker> tickerSaver) {

        if (Objects.isNull(getTicker()))
            return tickerSaver.save(newTicker());

        if ((maxTickerId > -1 && currentTickerId < maxTickerId )|| getTicker().getPlayers().size() > 0) {

            stopRunningClocks();

            ticker.getTickCounter().getListeners().clear();
            ticker.getTickCycler().getListeners().clear();
            ticker.getBeatCycler().getListeners().clear();
            ticker.getBarCycler().getListeners().clear();

            setTicker(tickerNav.next(currentTickerId));

            ticker.getPlayers().addAll(tickerPlayerFinder.find(ticker.getId()));
            ticker.getPlayers().forEach(p -> p.getRules().addAll(playerRuleFinder.find(p.getId())));

            newClockSource(ticker);
        }
        else 
            setTicker(tickerSaver.save(newTicker()));

        return getTicker();
    }

    public synchronized Ticker previous(long currentTickerId, Long minimumTickerId, Prior<Ticker> tickerNav,
            FindSet<Strike> tickerStrikeFinder, FindSet<Rule> playerRuleFinder, Save<Ticker> tickerSaver) {

        if (Objects.isNull(getTicker()))
            return tickerSaver.save(newTicker());

        if (currentTickerId > minimumTickerId) {
            stopRunningClocks();

            ticker.getTickCounter().getListeners().clear();
            ticker.getTickCycler().getListeners().clear();
            ticker.getBeatCycler().getListeners().clear();
            ticker.getBarCycler().getListeners().clear();

            setTicker(tickerNav.prior(currentTickerId));

            ticker.getPlayers().addAll(tickerStrikeFinder.find(ticker.getId()));
            ticker.getPlayers().forEach(p -> p.getRules().addAll(playerRuleFinder.find(p.getId())));
            newClockSource(ticker);
        }

        return ticker;
    }

    // public void clearPlayers() {
    // ticker.getPlayers().clear();
    // }

    public Ticker loadTicker(long tickerId, FindOne<Ticker> tickerFindById) {
        stopRunningClocks();
        return tickerFindById.find(tickerId).orElseThrow();
    }

    public Ticker newTicker() {
        stopRunningClocks();
        Ticker ticker = new Ticker();
        newClockSource(ticker);
        setTicker(ticker);
        return ticker;
    }
}