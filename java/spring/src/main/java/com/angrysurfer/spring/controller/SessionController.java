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

import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.core.util.Timer;
import com.angrysurfer.spring.dao.SessionStatus;
import com.angrysurfer.spring.service.SongService;
import com.angrysurfer.spring.service.SessionService;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

@CrossOrigin("*")
@Controller
@ResponseBody
@RequestMapping("/api")
public class SessionController {

    List<String> requestsToLog = new ArrayList<>();

    static Logger logger = LoggerFactory.getLogger(SessionController.class.getCanonicalName());

    private final SessionService sessionService;
    private final SongService songService;

    public SessionController(SessionService sessionService, SongService songService) {
        this.sessionService = sessionService;
        this.songService = songService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/tick")
    public Flux<SessionStatus> getTick() {

        // if (Objects.isNull(sessionService.getSession()))
        //     return Flux.empty();

        // logger.info("GET /api/tick");
        // Timer timer = sessionService.getClockSource().getTimer();
        // // Calculate interval based on actual timer settings
        // long intervalMillis = (long) ((60.0 / timer.getBpm() / timer.getPpq()) * 1000);
        // // Use minimum of calculated interval or 50ms to ensure responsiveness
        // long emitInterval = Math.max(Math.min(intervalMillis, 50), 16); // 16ms = ~60fps max

        // final Flux<SessionStatus> flux = Flux
        //         .fromStream(Stream.generate(() -> SessionStatus.from(sessionService.getSession(),
        //                 songService.getSong(),
        //                 sessionService.getClockSource().isRunning())));
        // final Flux<Long> emmitFlux = Flux.interval(Duration.ofMillis(emitInterval));

        // logger.debug("Tick stream configured with interval: {}ms (BPM: {}, PPQ: {})",
        //         emitInterval, timer.getBpm(), timer.getPpq());

        // return Flux.zip(flux, emmitFlux).map(Tuple2::getT1);

        return Flux.empty();
    }

    // @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/xox")
    // public Flux<SongStatus> getSongStatus() {
    // logger.info("GET /api/xox");
    // Timer timer = sessionService.getClockSource().getTimer();
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

    @GetMapping(path = Constants.LOAD_SESSION)
    public Session load(@RequestParam long sessionId) {
        if (requestsToLog.contains("load"))
            logger.info(Constants.LOAD_SESSION);
        return sessionService.loadSession(sessionId);
    }

    @GetMapping(path = Constants.START_SESSION)
    public void start() {
        if (requestsToLog.contains("start"))
            logger.info(Constants.START_SESSION);
        sessionService.play();
    }

    @GetMapping(path = Constants.PAUSE_SESSION)
    public void pause() {
        if (requestsToLog.contains("pause"))
            logger.info(Constants.PAUSE_SESSION);
        sessionService.pause();
    }

    @GetMapping(path = Constants.STOP_SESSION)
    public void stop() {
        if (requestsToLog.contains("stop"))
            logger.info(Constants.STOP_SESSION);
        sessionService.stop();
    }

    @GetMapping(path = Constants.NEXT_SESSION)
    public Session next(@RequestParam long currentSessionId) {
        if (requestsToLog.contains("next"))
            logger.info(Constants.NEXT_SESSION);
        return sessionService.next(currentSessionId);
    }

    @GetMapping(path = Constants.PREV_SESSION)
    public Session previous(@RequestParam long currentSessionId) {
        if (requestsToLog.contains("previous"))
            logger.info(Constants.PREV_SESSION);
        return sessionService.previous(currentSessionId);
    }

    @GetMapping(path = Constants.SESSION_STATUS)
    public @ResponseBody SessionStatus getSessionStatus() {
        if (requestsToLog.contains("status"))
            logger.info(Constants.SESSION_STATUS);
        return sessionService.getSessionStatus();
    }

    @GetMapping(path = Constants.SESSION_INFO)
    public @ResponseBody SessionStatus getSession() {
        if (requestsToLog.contains("info"))
            logger.info(Constants.SESSION_INFO);

        SessionStatus result = SessionStatus.from(sessionService.getSession(), songService.getSong(), false);
        return result;
    }

    @GetMapping(path = Constants.SESSION_LOG)
    public void toggleShowSession(@RequestParam String requestType) {
        if (requestsToLog.contains(requestType))
            requestsToLog.remove(requestType);
        else
            requestsToLog.add(requestType);
    }

    @GetMapping(Constants.UPDATE_SESSION)
    public @ResponseBody Session updateSession(@RequestParam Long sessionId, @RequestParam int updateType,
            @RequestParam Long updateValue) {
        if (requestsToLog.contains("update"))
            logger.info(Constants.UPDATE_SESSION);
        return sessionService.updateSession(sessionId, updateType, updateValue);
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
    // Flux<SessionStatus> sessionStatus() {
    // return Flux.from(sessionStatusPublisher);
    // }
    // } return Flux
    // .from(new Publisher<SessionStatus>() {

    // @Override
    // public void subscribe(Subscriber<? super SessionStatus> subscriber) {
    // IntStream.range(0, 100).forEach(i -> {
    // logger.info("onTick");
    // try {
    // Thread.sleep(100);
    // } catch (InterruptedException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // subscriber.onNext(new SessionStatus());
    // });
    // }
    // });
    // }

    // @GetMapping(value = "/tick", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // Flux<SessionStatus> sessionStatus() {

    // return Flux.from(sessionService.getSessionStatusPublisher());

    // return Flux.from(new Publisher<SessionStatus>() {
    // @Override
    // public void subscribe(Subscriber<? super SessionStatus> subscriber) {

    // sessionService.getSequenceRunner().getListeners().add(new TickListener() {
    // @Override
    // public void onTick() {
    // logger.info("onTick");
    // subscriber.onNext(SessionStatus.from(sessionService.getSession(),
    // sessionService.getSequenceRunner().isPlaying()));
    // }

    // // send a final tick with cycled session status

    // @Override
    // public void onEnd() {
    // subscriber.onNext(SessionStatus.from(sessionService.getSession(),
    // sessionService.getSequenceRunner().isPlaying()));
    // }
    // });

}