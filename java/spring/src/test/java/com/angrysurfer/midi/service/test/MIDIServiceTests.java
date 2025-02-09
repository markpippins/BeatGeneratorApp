// package com.angrysurfer.midi.service.test;

// import static org.junit.jupiter.api.Assertions.assertTrue;

// import java.util.List;

// import javax.sound.midi.MidiDevice;

// import org.aspectj.lang.annotation.Before;
// import org.junit.jupiter.api.Test;
// import org.junit.runner.RunWith;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.TestPropertySource;
// import org.springframework.test.context.junit4.SpringRunner;

// import com.angrysurfer.core.model.midi.Instrument;
// import com.angrysurfer.core.repo.ControlCodes;
// import com.angrysurfer.core.repo.Instruments;
// import com.angrysurfer.core.repo.Pads;
// import com.angrysurfer.core.repo.Songs;
// import com.angrysurfer.core.repo.Steps;
// import com.angrysurfer.spring.Application;
// import com.angrysurfer.spring.service.MIDIService;

// @RunWith(SpringRunner.class)
// @SpringBootTest(
//   webEnvironment = SpringBootTest.WebEnvironment.MOCK,
//   classes = Application.class)
// @AutoConfigureMockMvc
// @TestPropertySource(
//   locations = "classpath:application.properties")
// public class MIDIServiceTests {

  
//     static Logger logger = LoggerFactory.getLogger(MIDIServiceTests.class.getCanonicalName());

//     @Autowired
//     MIDIService midiService;

//     @Autowired
//     Instruments midiInstrumentRepository;

//     @Autowired
//     ControlCodes controlCodeRepository;

//     @Autowired
//     Pads padRepository;

//     @Autowired
//     Steps stepRepository;

//     @Autowired
//     Songs songRepository;

//     static String RAZ = "raz";
//     static String ZERO = "zero";

//     static String DEVICE = "MRCC 880";

//     @Before
//     public void setUp() {
//         if (!midiInstrumentRepository.findByName(RAZ).isPresent()) {
//             Instrument raz = new Instrument();
//             raz.setName(RAZ);
//             raz.setDeviceName(DEVICE);
//             // raz.setChannels([9]);
//             midiInstrumentRepository.save(raz);
//         }
//     }

//     @Test
//     public void whenMidiDevicesRetrieved_thenListIsPopulated() {
//             List<MidiDevice> devices = MIDIService.getMidiOutDevices();
//         assertTrue(devices.size() > 0);
//     }

        
//     // @Test
//     // public void whenInstrumentRequestedDeviceIsInitialized() {
//     //     midiService.getInstrumentByChannel(9).forEach(i -> {
//     //         try {
//     //             if (!i.getDevice().isOpen())
//     //                 i.getDevice().open();

//     //             i.noteOn(45, 120);
//     //             Thread.sleep(500);
//     //             i.noteOff(45, 120);
                
//     //         } catch (InvalidMidiDataException | MidiUnavailableException | InterruptedException e) {
//     //             logger.error(e.getMessage());
//     //         }
//     //     });
//     // }

//     // @Test
//     // public void whenMidiDevicesRetrievedByName_thenCorrectDeviceReturned() {
//     //     List<MidiDevice> devices = midiService.findMidiDevices(true, false);
//     //     assertTrue(devices.size() > 0);
//     //     devices.forEach(d -> {
//     //         String name = d.getDeviceInfo().getName();
//     //         MidiDevice device = midiService.findMidiOutDevice(name);
//     //         assertEquals(d, device);
//     //     });
//     // }

// }
