package com.angrysurfer.midi.service.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.angrysurfer.BeatGeneratorApplication;
import com.angrysurfer.midi.service.TickerService;


@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = BeatGeneratorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
  locations = "classpath:application.properties")

public class TickerServiceTests {
    static Logger logger = org.slf4j.LoggerFactory.getLogger(PlayerServiceTests.class.getCanonicalName());

    @Autowired
    TickerService tickerService;
    @Test
    public void whenNewTickerCalled_thenGetTickerWithNewId() {
        Long id = tickerService.getTicker().getId();
        tickerService.newTicker();
        assertTrue(tickerService.getTicker().getId() > id); 
    }

    @Test
    public void whenLoadTickerCalled_thenGetRequestedTicker() {
        Long id = tickerService.getTicker().getId();
        tickerService.newTicker();
        assertTrue(tickerService.getTicker().getId() > id);
        
        tickerService.loadTicker(id);
        assertTrue(tickerService.getTicker().getId().equals(id));
    }
}
