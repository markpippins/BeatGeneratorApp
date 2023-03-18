package com.angrysurfer.midi.model.test;

import com.angrysurfer.midi.repo.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.angrysurfer.BeatGeneratorApplication;
import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.Ticker;

@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = BeatGeneratorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
  locations = "classpath:application.properties")
public class TickerTests {

    @Autowired
    MidiInstrumentRepository midiInstrumentRepository;

    static String RAZ = "raz";
    static String ZERO = "zero";

    static String DEVICE = "MRCC 880";

    @Before
    public void setUp() {
        if (!midiInstrumentRepository.findByName(RAZ).isPresent()) {
            MidiInstrument raz = new MidiInstrument();
            raz.setName(RAZ);
            raz.setDeviceName(DEVICE);
            raz.setChannel(9);
            midiInstrumentRepository.save(raz);
        }
    }

    @After
    public void tearDown() {
        // if (midiInstrumentRepository.findByName(RAZ).isPresent()) {
        //     MidiInstrument raz = midiInstrumentRepository.findByName(RAZ).orElseThrow() ;
        //     raz.setName(RAZ);
        //     raz.setDeviceName(DEVICE);
        //     raz.setChannel(9);
        //     midiInstrumentRepository.save(raz);
        // }
    }

    @Test
    public void whenRunCalled_thenTickerStarts() {
        Ticker ticker = new Ticker();
        // ticker.run();
        // assertTrue(ticker.isPlaying());
        // ticker.stop();
        // assertTrue(!ticker.isPlaying());        
    }

    // @Test
    // public void whenMidiDevicesRetrievedByName_thenCorrectDeviceReturned() {
    //     List<MidiDevice> devices = midiService.findMidiDevices(true, false);
    //     assertTrue(devices.size() > 0);
    //     devices.forEach(d -> {
    //         String name = d.getDeviceInfo().getName();
    //         MidiDevice device = midiService.findMidiOutDevice(name);
    //         assertEquals(d, device);
    //     });
    // }

}
