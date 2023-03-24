package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.PatternRepository;
import com.angrysurfer.midi.repo.SongRepository;
import com.angrysurfer.midi.repo.StepRepository;
import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Getter
@Setter
@Service
public class SongService {

    static Logger log = LoggerFactory.getLogger(SongService.class.getCanonicalName());
    
    private StepRepository stepDataRepository;
    private SongRepository songRepository;
    private PatternRepository patternRepository;

    private Song song;
    
    private Map<Integer, Map<Integer, Step>> steps = new HashMap<>();

    public SongService(PatternRepository patternRepository, StepRepository stepRepository,
                    SongRepository songRepository) {
        this.stepDataRepository = stepRepository;
        this.songRepository = songRepository;
    }

    public Step updateStep(Step step) {
        Step result;

        if (Objects.isNull(step.getId()) || step.getId() == 0)
            result = stepDataRepository.save(step);            

        else {
            result = stepDataRepository.findById(step.getId()).orElseThrow();
            result.copyValues(step);
            result = stepDataRepository.save(step);
        }

        
        Map<Integer, Step> page;

        if (steps.containsKey(step.getPage()))
            page = steps.get(step.getPage());
        else page = new HashMap<>();

        steps.put(step.getPage(), page);
        page.put(step.getPosition(), step);

        return result;
    }

    public Song loadSong(long songId) {
        this.song = songRepository.findById(songId).orElse(null);
        return song;
    }

    public Song newSong() {
        return songRepository.save(new Song());
    }
        
}