package com.angrysurfer.midi.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.angrysurfer.midi.model.TickListener;
import com.angrysurfer.midi.model.Ticker;
import com.angrysurfer.midi.model.TickerStatus;
import com.angrysurfer.midi.service.TickerService;
import com.angrysurfer.midi.util.Constants;

import lombok.AllArgsConstructor;
import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

@CrossOrigin("*")
@Controller
@ResponseBody
@RequestMapping("/api")
public class TickerController {

    @AllArgsConstructor
    @Data
    public class Foo {

        private long id;
        private String name;

    }

    List<String> requestsToLog = new ArrayList<>();

    static Logger logger = LoggerFactory.getLogger(TickerController.class.getCanonicalName());

    private final TickerService tickerService;

    public TickerController(TickerService tickerService) {
        this.tickerService = tickerService;
    }

    // private Publisher<TickerStatus> tickerStatusPublisher = new
    // Publisher<TickerStatus>() {

    // @Override
    // public void subscribe(Subscriber<? super TickerStatus> subscriber) {

    // tickerService.getSequenceRunner().getListeners().add(new TickListener() {

    // @Override
    // public void onTick() {
    // subscriber.onNext(TickerStatus.from(tickerService.getTicker(),
    // tickerService.getSequenceRunner().isPlaying()));
    // }

    // @Override
    // public void onEnd() {
    // subscriber.onNext(TickerStatus.from(tickerService.getTicker(),
    // tickerService.getSequenceRunner().isPlaying()));
    // }
    // });
    // }
    // };

    // @GetMapping("/foos/{id}")
    // public Mono<Foo> getFoo(@PathVariable("id") long id) {
    // return Mono.just(new Foo(id, randomAlphabetic(6)));
    // }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/foos")
    public Flux<Foo> getAllFoos2() {
        final Flux<Foo> foosFlux = Flux
                .fromStream(Stream.generate(() -> new Foo(new Random().nextLong(), randomAlphabetic(6))));
        final Flux<Long> emmitFlux = Flux.interval(Duration.ofSeconds(1));
        return Flux.zip(foosFlux, emmitFlux).map(Tuple2::getT1);
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/foos2")
    public Flux<TickerStatus> getAllFoos() {
        final Flux<TickerStatus> flux = Flux.<TickerStatus>create(fluxSink -> {
            while (true) {
                fluxSink.next(
                        TickerStatus.from(tickerService.getTicker(), tickerService.getSequenceRunner().isPlaying()));
            }
        }).sample(Duration.ofMillis(50)).log();

        return flux;
    }

    // @GetMapping(value = "/tick", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // Flux<TickerStatus> tickerStatus() {
    // return Flux.from(tickerStatusPublisher);
    // }

    // @GetMapping(value = "/tick2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // Publisher<TickerStatus> tickerStatus2() {
    // return tickerStatusPublisher;
    // }

    // @GetMapping(path = Constants.ADD_TICKER)
    // public Ticker newTicker() {
    // if (requestsToLog.contains("new"))
    // logger.info(Constants.ADD_TICKER);
    // return service.newTicker();
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
    public Ticker stop() {
        if (requestsToLog.contains("stop"))
            logger.info(Constants.STOP_TICKER);
        return tickerService.stop();
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
    public @ResponseBody Ticker getTicker() {
        if (requestsToLog.contains("info"))
            logger.info(Constants.TICKER_INFO);
        return tickerService.getTicker();
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
            @RequestParam int updateValue) {
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