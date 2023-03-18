package com.angrysurfer.midi.service.test;

import com.angrysurfer.midi.repo.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.sound.midi.MidiDevice;

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
import com.angrysurfer.midi.service.MIDIService;

@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = BeatGeneratorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(
  locations = "classpath:application.properties")
public class MIDIServiceTests {

    @Autowired
    MIDIService midiService;

    @Autowired
    MidiInstrumentRepository midiInstrumentRepository;

    @Autowired
    ControlCodeRepository controlCodeRepository;

    @Autowired
    PadRepository padRepository;

    @Autowired
    StepRepository stepRepository;

    @Autowired
    SongRepository songRepository;

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

    @Test
    public void whenMidiDevicesRetrieved_thenListIsPopulated() {
            List<MidiDevice> devices = MIDIService.getMidiDevices();
        assertTrue(devices.size() > 0);
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
