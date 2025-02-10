package com.angrysurfer.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.angrysurfer.core.config.DeviceConfig;
import com.angrysurfer.spring.repo.Captions;
import com.angrysurfer.spring.repo.ControlCodes;
import com.angrysurfer.spring.repo.Instruments;
import com.angrysurfer.spring.repo.Pads;
import com.angrysurfer.spring.service.DBService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@EntityScan("com.angrysurfer.core.model")
@EnableJpaRepositories("com.angrysurfer.spring.repo")
public class Application {

    static ObjectMapper mapper = new ObjectMapper();

    @Value("${sys.config.filepath}")
    private String configFilepath;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner beatGeneratorSetup(Instruments instruments,
            DBService dbUtils) {
        return args -> {
            try {
                if (instruments.count() == 0)
                    DeviceConfig.loadDefaults(configFilepath, instruments, controlCodes, captions,
                            pads);
                // SystemConfig.saveCurrentStateToFile(configFilepath, instruments);
            } catch (Exception e) {
                e.printStackTrace();
            }

        };
    }

}
