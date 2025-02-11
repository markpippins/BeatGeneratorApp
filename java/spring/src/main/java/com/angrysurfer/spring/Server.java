package com.angrysurfer.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.angrysurfer.core.config.DeviceConfig;
import com.angrysurfer.spring.repo.Instruments;
import com.angrysurfer.spring.service.DBService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@EntityScan("com.angrysurfer.core.model")
@EnableJpaRepositories("com.angrysurfer.spring.repo")
public class Server {

    static ObjectMapper mapper = new ObjectMapper();

    @Value("${sys.config.filepath}")
    private String configFilepath;

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    @Bean
    CommandLineRunner loadDefaults(Instruments instruments,
            DBService dbUtils) {
        return args -> {
            if (instruments.count() == 0)
                try {
                    DeviceConfig.loadDefaults(configFilepath, dbUtils.getInstrumentFindAll(),
                            dbUtils.getInstrumentSaver(),
                            dbUtils.getCaptionSaver(), dbUtils.getControlCodeSaver(), dbUtils.getPadSaver());
                    // SystemConfig.saveCurrentStateToFile(configFilepath, instruments);
                } catch (Exception e) {
                    e.printStackTrace();
                }

        };
    }

}
