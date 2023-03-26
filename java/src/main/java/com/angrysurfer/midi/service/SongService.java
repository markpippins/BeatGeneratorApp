package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.*;
import com.angrysurfer.midi.repo.PatternRepository;
import com.angrysurfer.midi.repo.SongRepository;
import com.angrysurfer.midi.repo.StepRepository;
import com.angrysurfer.midi.util.CyclerListener;
import com.angrysurfer.midi.util.StepUpdateType;
import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.midi.ShortMessage;

@Getter
@Setter
@Service
public class SongService {

    static Logger logger = LoggerFactory.getLogger(SongService.class.getCanonicalName());
    
    private StepRepository stepDataRepository;
    private SongRepository songRepository;
    private PatternRepository patternRepository;
    private MIDIService midiService;

    private Song song;
    private Map<Integer, Map<Integer, Step>> songStepsMap = new ConcurrentHashMap<>();

    private CyclerListener beatListener = new CyclerListener() {

        @Override
        public void advanced(long position) {
            if (songStepsMap.containsKey(0)) {
                Map<Integer, Step> stepMap = songStepsMap.get(0);
                if (stepMap.containsKey((int) position)) {
                    Step step = stepMap.get((int) position);
                    if (step.getActive()) {
                        midiService.sendMessageToChannel(4, ShortMessage.NOTE_ON, step.getPitch(), step.getVelocity());
                    }
                }
            }
        }

        @Override
        public void cycleComplete() {
            int position = 1;
            if (songStepsMap.containsKey(0)) {
                Map<Integer, Step> stepMap = songStepsMap.get(0);
                if (stepMap.containsKey(position)) {
                    Step step = stepMap.get(position);
                    if (step.getActive()) {
                        midiService.sendMessageToChannel(4, ShortMessage.NOTE_ON, step.getPitch(), step.getVelocity());
                    }
                }
            }
        }

        @Override
        public void starting() {
            logger.info("beat advanced");
        }
        
    };
    private CyclerListener barListener = new CyclerListener() {

        @Override
        public void advanced(long position) {
        }

        @Override
        public void cycleComplete() {
            logger.info("bars complete");
        }

        @Override
        public void starting() {
            this.advanced(0);
        }
        
    };


    public SongService(PatternRepository patternRepository, StepRepository stepRepository,
                    SongRepository songRepository, MIDIService midiService) {
        this.stepDataRepository = stepRepository;
        this.songRepository = songRepository;
        this.patternRepository = patternRepository;
        this.midiService = midiService;
    }

    public Step updateStep(Long stepId, int position, int updateType, int updateValue) {
        Step step = stepDataRepository.findById(stepId).orElse(new Step());
        step.setPage(Objects.isNull(step.getPage()) ? 0 : step.getPage());
        if (Objects.isNull(step.getPosition()))
            step.setPosition(position);

        switch (updateType) {
            case StepUpdateType.ACTIVE : step.setActive(!step.getActive());;
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

        Map<Integer, Step> page = songStepsMap.containsKey(step.getPage()) ? 
            songStepsMap.get(step.getPage()) : new ConcurrentHashMap<>();
        page.put(step.getPosition(), step);
        songStepsMap.put(step.getPage(), page);
         
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