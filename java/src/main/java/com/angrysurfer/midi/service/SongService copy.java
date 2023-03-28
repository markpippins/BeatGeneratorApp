// package com.angrysurfer.midi.service;

// import com.angrysurfer.midi.model.*;
// import com.angrysurfer.midi.repo.PatternRepository;
// import com.angrysurfer.midi.repo.SongRepository;
// import com.angrysurfer.midi.repo.StepRepository;
// import com.angrysurfer.midi.util.CyclerListener;
// import com.angrysurfer.midi.util.StepUpdateType;
// import lombok.Getter;
// import lombok.Setter;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.stereotype.Service;

// import java.util.*;
// import java.util.concurrent.ConcurrentHashMap;

// import javax.sound.midi.ShortMessage;

// @Getter
// @Setter
// @Service
// public class SongService {

//     private final class BarCyclerListenerImplementation implements CyclerListener {
//         @Override
//         public void advanced(long position) {
//         }

//         @Override
//         public void cycleComplete() {
//             logger.info("bars complete");
//         }

//         @Override
//         public void starting() {
//             this.advanced(0);
//         }
//     }

//     private final class BeatCyclerListenerImplementation implements CyclerListener {
//         @Override
//         public void advanced(long position) {
//             if (songStepsMap.containsKey(0)) {
//                 Map<Integer, Step> stepMap = songStepsMap.get(0);
//                 if (stepMap.containsKey((int) position)) {
//                     Step step = stepMap.get((int) position);
//                     if (step.getActive()) {
//                         midiService.sendMessageToChannel(4, ShortMessage.NOTE_ON, step.getPitch(), step.getVelocity());
//                     }
//                 }
//             }
//         }

//         @Override
//         public void cycleComplete() {
//             int position = 1;
//             if (songStepsMap.containsKey(0)) {
//                 Map<Integer, Step> stepMap = songStepsMap.get(0);
//                 if (stepMap.containsKey(position)) {
//                     Step step = stepMap.get(position);
//                     if (step.getActive()) {
//                         midiService.sendMessageToChannel(4, ShortMessage.NOTE_ON, step.getPitch(), step.getVelocity());
//                     }
//                 }
//             }
//         }

//         @Override
//         public void starting() {
//             logger.info("beat advanced");
//         }
//     }

//     static Logger logger = LoggerFactory.getLogger(SongService.class.getCanonicalName());
    
//     private StepRepository stepDataRepository;
//     private SongRepository songRepository;
//     private PatternRepository patternRepository;
//     private MIDIService midiService;

//     private Song song;
//     private Map<Integer, Map<Integer, Step>> songStepsMap = new ConcurrentHashMap<>();

//     private CyclerListener beatListener = new BeatCyclerListenerImplementation();
//     private CyclerListener barListener = new BarCyclerListenerImplementation();


//     public SongService(PatternRepository patternRepository, StepRepository stepRepository,
//                     SongRepository songRepository, MIDIService midiService) {
//         this.stepDataRepository = stepRepository;
//         this.songRepository = songRepository;
//         this.patternRepository = patternRepository;
//         this.midiService = midiService;
//     }

//     public Step updateStep(Long stepId, int position, int updateType, int updateValue) {
//         Step step = stepDataRepository.findById(stepId).orElse(new Step());
//         step.setPage(Objects.isNull(step.getPage()) ? 0 : step.getPage());
//         if (Objects.isNull(step.getPosition()))
//             step.setPosition(position);

//         switch (updateType) {
//             case StepUpdateType.ACTIVE : step.setActive(!step.getActive());;
//                 break;

//             case StepUpdateType.GATE : step.setGate(updateValue);
//                 break;

//             case  StepUpdateType.PITCH: step.setPitch(updateValue);
//                 break;

//             case  StepUpdateType.PROBABILITY: step.setProbability(updateValue);
//                 break;

//             case  StepUpdateType.VELOCITY: step.setVelocity(updateValue);
//                 break;
//         }

//         Map<Integer, Step> page = songStepsMap.containsKey(step.getPage()) ? 
//             songStepsMap.get(step.getPage()) : new ConcurrentHashMap<>();
//         page.put(step.getPosition(), step);
//         songStepsMap.put(step.getPage(), page);
         
//         return stepDataRepository.save(step);
//     }

//     public Song loadSong(long songId) {
//         songRepository.flush();
//         setSong(null);
//         this.song = songRepository.findById(songId).orElse(null);
//         return this.song;
//     }

//     public Song newSong() {
//         songRepository.flush();
//         setSong(songRepository.save(new Song()));
//         return getSong();
//     }

//     public Song next(long currentSongId) {
//         songRepository.flush();
//         if (currentSongId == 0 || getSong().getSteps().size() > 0) {       
//             Long maxSongId = getSongRepository().getMaximumSongId();
//             setSong(Objects.nonNull(maxSongId) && currentSongId < maxSongId ?
//                 getSongRepository().getNextSong(currentSongId) :
//                 null);
//             getSong().getSteps().addAll(getStepDataRepository().findBySongId(getSong().getId()));
//             getSong().getSteps().forEach(s -> s.setSong(getSong()));
//         }
    
//         return getSong();
//     }

//     public synchronized Song previous(long currentSongId) {
//         songRepository.flush();
//         if (currentSongId >  (getSongRepository().getMinimumSongId())) {
//             setSong(getSongRepository().getPreviousSong(currentSongId));
//             getSong().getSteps().addAll(getStepDataRepository().findBySongId(getSong().getId()));
//             getSong().getSteps().forEach(s -> s.setSong(getSong()));
//         }

//         return getSong();
//     }

//     public Song getSong() {
//         if (Objects.isNull(song))
//             setSong(getSongRepository().save(new Song()));
        
//         return this.song;
//     }

//     public Step addStep(int page) {
//         getSongRepository().flush();

//         Step step = new Step();
//         step.setPosition(getSong().getSteps().size() + 1);
//         step.setPage(page);
//         step.setSong(getSong());
//         step = getStepDataRepository().save(step);
//         getSong().getSteps().add(step);
//         return step;
//     }

//     public Set<Step> removeStep(Long stepId) {
//         Step step = getSong().getSteps().stream().filter(s -> s.getId().equals(stepId)).findAny().orElseThrow();
//         getSong().getSteps().remove(step);
//         getStepDataRepository().delete(step);
//         return getSong().getSteps();
//     }


// }