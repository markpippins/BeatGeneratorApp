package com.angrysurfer.sequencer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.angrysurfer.sequencer.config.DeviceConfig;
import com.angrysurfer.sequencer.repo.Captions;
import com.angrysurfer.sequencer.repo.ControlCodes;
import com.angrysurfer.sequencer.repo.Instruments;
import com.angrysurfer.sequencer.repo.Pads;
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
    CommandLineRunner beatGeneratorSetup(Instruments midiInstruments,
            Pads pads,
            ControlCodes controlCodes,
            Captions captions) {
        return args -> {
            try {
                if (midiInstruments.count() == 0)
                    DeviceConfig.loadDefaults(configFilepath, midiInstruments, controlCodes, captions,
                            pads);
                // SystemConfig.saveCurrentStateToFile(configFilepath, midiInstruments);
            } catch (Exception e) {
                e.printStackTrace();
            }

        };
    }

}
