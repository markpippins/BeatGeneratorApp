package com.angrysurfer.spring.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.spring.service.SongService;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class SongController {
    // double numberOfTicksToWait = getBeatFraction() *
    // (getSession().getTicksPerBeat() / getSubDivisions());
    List<String> requestsToLog = new ArrayList<>();

    static Logger logger = LoggerFactory.getLogger(SongController.class.getCanonicalName());

    private final SongService service;

    public SongController(SongService service) {
        this.service = service;
    }

    @GetMapping(path = "/steps/update")
    public Step updateStep(@RequestParam long stepId, @RequestParam int updateType, @RequestParam int updateValue) {
        return service.updateStep(stepId, updateType, updateValue);
    }

    @GetMapping(path = "/patterns/update")
    public Pattern updatePatter(@RequestParam long patternId, @RequestParam int updateType,
            @RequestParam int updateValue) {
        return service.updatePattern(patternId, updateType, updateValue);
    }

    @GetMapping(path = Constants.SONG_INFO)
    public Song songInfo() {
        return service.getSongInfo();
    }

    @GetMapping(path = "/song/new")
    public Song newSong() {
        if (requestsToLog.contains("new"))
            logger.info("/song/new");
        return service.newSong();
    }


    @GetMapping(path = Constants.NEXT_SONG)
    public Song next(@RequestParam long currentSongId) {
        if (requestsToLog.contains("next"))
            logger.info(Constants.NEXT_SONG);
        return service.next(currentSongId);
    }

    @GetMapping(path = Constants.PREV_SONG)
    public Song previous(@RequestParam long currentSongId) {
        if (requestsToLog.contains("previous"))
            logger.info(Constants.PREV_SONG);
        return service.previous(currentSongId);
    }
}
