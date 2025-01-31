package com.angrysurfer.sequencer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

import com.angrysurfer.sequencer.config.SystemConfig;
import com.angrysurfer.sequencer.repo.CaptionRepo;
import com.angrysurfer.sequencer.repo.ControlCodeRepo;
import com.angrysurfer.sequencer.repo.MidiInstrumentRepo;
import com.angrysurfer.sequencer.repo.PadRepo;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class Application {

    static ObjectMapper mapper = new ObjectMapper();
    
    @Value("${sys.config.filepath}")
    private String configFilepath;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner beatGeneratorSetup(MidiInstrumentRepo midiInstrumentRepo,
            PadRepo padRepo,
            ControlCodeRepo controlCodeRepo,
            CaptionRepo captionRepo) {
        return args -> {
            SystemConfig.loadDefaults(configFilepath, midiInstrumentRepo, controlCodeRepo, captionRepo, padRepo);
        };
    }

}
