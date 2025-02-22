package com.angrysurfer.spring.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.core.util.Timer;
import com.angrysurfer.spring.dao.TickerStatus;
import com.angrysurfer.spring.service.SongService;
import com.angrysurfer.spring.service.TickerService;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

@CrossOrigin("*")
@Controller
@ResponseBody
@RequestMapping("/api")
public class TickerController {

    List<String> requestsToLog = new ArrayList<>();

    static Logger logger = LoggerFactory.getLogger(TickerController.class.getCanonicalName());

    private final TickerService tickerService;
    private final SongService songService;

    public TickerController(TickerService tickerService, SongService songService) {
        this.tickerService = tickerService;
        this.songService = songService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/tick")
    public Flux<TickerStatus> getTick() {

        if (Objects.isNull(tickerService.getTicker()))
            return Flux.empty();

        logger.info("GET /api/tick");
        Timer timer = tickerService.getClockSource().getTimer();
        // Calculate interval based on actual timer settings
        long intervalMillis = (long) ((60.0 / timer.getBpm() / timer.getPpq()) * 1000);
        // Use minimum of calculated interval or 50ms to ensure responsiveness
        long emitInterval = Math.max(Math.min(intervalMillis, 50), 16); // 16ms = ~60fps max

        final Flux<TickerStatus> flux = Flux
                .fromStream(Stream.generate(() -> TickerStatus.from(tickerService.getTicker(),
                        songService.getSong(),
                        tickerService.getClockSource().isRunning())));
        final Flux<Long> emmitFlux = Flux.interval(Duration.ofMillis(emitInterval));

        logger.debug("Tick stream configured with interval: {}ms (BPM: {}, PPQ: {})",
                emitInterval, timer.getBpm(), timer.getPpq());

        return Flux.zip(flux, emmitFlux).map(Tuple2::getT1);
    }

    // @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/xox")
    // public Flux<SongStatus> getSongStatus() {
    // logger.info("GET /api/xox");
    // Timer timer = tickerService.getClockSource().getTimer();
    // // Calculate interval based on beats rather than ticks
    // long beatIntervalMillis = (long) ((60.0 / timer.getBpm()) * 1000);
    // // Update roughly twice per beat, but no faster than 100ms
    // long emitInterval = Math.max(beatIntervalMillis / 2, 100);

    // final Flux<SongStatus> flux = Flux
    // .fromStream(Stream.generate(() -> songService.getSongStatus()));
    // final Flux<Long> emmitFlux = Flux.interval(Duration.ofMillis(emitInterval));

    // logger.debug("Song status stream configured with interval: {}ms (BPM: {})",
    // emitInterval, timer.getBpm());

    // return Flux.zip(flux, emmitFlux).map(Tuple2::getT1);
    // }

    @GetMapping(path = Constants.LOAD_TICKER)
    public Ticker load(@RequestParam long tickerId) {
        if (requestsToLog.contains("load"))
            logger.info(Constants.LOAD_TICKER);
        return tickerService.loadTicker(tickerId);
    }

    @GetMapping(path = Constants.START_TICKER)
    public void start() {
        if (requestsToLog.contains("start"))
            logger.info(Constants.START_TICKER);
        tickerService.play();
    }

    @GetMapping(path = Constants.PAUSE_TICKER)
    public void pause() {
        if (requestsToLog.contains("pause"))
            logger.info(Constants.PAUSE_TICKER);
        tickerService.pause();
    }

    @GetMapping(path = Constants.STOP_TICKER)
    public void stop() {
        if (requestsToLog.contains("stop"))
            logger.info(Constants.STOP_TICKER);
        tickerService.stop();
    }

    @GetMapping(path = Constants.NEXT_TICKER)
    public Ticker next(@RequestParam long currentTickerId) {
        if (requestsToLog.contains("next"))
            logger.info(Constants.NEXT_TICKER);
        return tickerService.next(currentTickerId);
    }

    @GetMapping(path = Constants.PREV_TICKER)
    public Ticker previous(@RequestParam long currentTickerId) {
        if (requestsToLog.contains("previous"))
            logger.info(Constants.PREV_TICKER);
        return tickerService.previous(currentTickerId);
    }

    @GetMapping(path = Constants.TICKER_STATUS)
    public @ResponseBody TickerStatus getTickerStatus() {
        if (requestsToLog.contains("status"))
            logger.info(Constants.TICKER_STATUS);
        return tickerService.getTickerStatus();
    }

    @GetMapping(path = Constants.TICKER_INFO)
    public @ResponseBody TickerStatus getTicker() {
        if (requestsToLog.contains("info"))
            logger.info(Constants.TICKER_INFO);

        TickerStatus result = TickerStatus.from(tickerService.getTicker(), songService.getSong(), false);
        return result;
    }

    @GetMapping(path = Constants.TICKER_LOG)
    public void toggleShowTicker(@RequestParam String requestType) {
        if (requestsToLog.contains(requestType))
            requestsToLog.remove(requestType);
        else
            requestsToLog.add(requestType);
    }

    @GetMapping(Constants.UPDATE_TICKER)
    public @ResponseBody Ticker updateTicker(@RequestParam Long tickerId, @RequestParam int updateType,
            @RequestParam Long updateValue) {
        if (requestsToLog.contains("update"))
            logger.info(Constants.UPDATE_TICKER);
        return tickerService.updateTicker(tickerId, updateType, updateValue);
    }

    // @GetMapping(value = "/greetings/{name}", produces =
    // MediaType.TEXT_EVENT_STREAM_VALUE)
    // Flux<Greeting> greetings(@PathVariable String name) {
    // return Flux
    // .fromStream(Stream.generate(() -> new Greeting("Hello, " + name + "!")))
    // .take(1000)
    // .delayElements(Duration.ofSeconds(1));
    // }

    // @GetMapping(value = "/tick", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // Flux<TickerStatus> tickerStatus() {
    // return Flux.from(tickerStatusPublisher);
    // }
    // } return Flux
    // .from(new Publisher<TickerStatus>() {

    // @Override
    // public void subscribe(Subscriber<? super TickerStatus> subscriber) {
    // IntStream.range(0, 100).forEach(i -> {
    // logger.info("onTick");
    // try {
    // Thread.sleep(100);
    // } catch (InterruptedException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // subscriber.onNext(new TickerStatus());
    // });
    // }
    // });
    // }

    // @GetMapping(value = "/tick", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // Flux<TickerStatus> tickerStatus() {

    // return Flux.from(tickerService.getTickerStatusPublisher());

    // return Flux.from(new Publisher<TickerStatus>() {
    // @Override
    // public void subscribe(Subscriber<? super TickerStatus> subscriber) {

    // tickerService.getSequenceRunner().getListeners().add(new TickListener() {
    // @Override
    // public void onTick() {
    // logger.info("onTick");
    // subscriber.onNext(TickerStatus.from(tickerService.getTicker(),
    // tickerService.getSequenceRunner().isPlaying()));
    // }

    // // send a final tick with cycled ticker status

    // @Override
    // public void onEnd() {
    // subscriber.onNext(TickerStatus.from(tickerService.getTicker(),
    // tickerService.getSequenceRunner().isPlaying()));
    // }
    // });

}