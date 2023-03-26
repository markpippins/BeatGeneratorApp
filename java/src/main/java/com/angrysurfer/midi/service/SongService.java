package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.PatternRepository;
import com.angrysurfer.midi.repo.SongRepository;
import com.angrysurfer.midi.repo.StepRepository;
import com.angrysurfer.midi.util.StepUpdateType;
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

    public Step updateStep(Long stepId, int updateType, int updateValue) {
        Step step = stepDataRepository.findById(stepId).orElse(new Step());
        
        switch (updateType) {
            case StepUpdateType.ACTIVE : step.setActive(!step.isActive());;
                break;

            case StepUpdateType.GATE : step.setGate(updateValue);
                break;

            case  StepUpdateType.PITCH: step.setPitch(updateValue);
                break;

            case  StepUpdateType.PROBABILITY: step.setProbability(updateValue);
                break;

            case  StepUpdateType.VELOCITY: step.setVelocity(updateValue);
                break;
        }

        Map<Integer, Step> page = steps.containsKey(step.getPage()) ? steps.get(step.getPage()) : new HashMap<>();
        page.put(step.getPosition(), step);
        steps.put(step.getPage(), page);
         
        return stepDataRepository.save(step);
    }

    public Song loadSong(long songId) {
        this.song = songRepository.findById(songId).orElse(null);
        return song;
    }

    public Song newSong() {
        return songRepository.save(new Song());
    }
        
}