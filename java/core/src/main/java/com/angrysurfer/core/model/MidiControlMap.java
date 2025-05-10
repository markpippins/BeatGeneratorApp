import java.util.Map;
import java.util.HashMap;


// WebMidi.js MIDI_CONTROL_CHANGE_MESSAGES

public class MidiControlMap {
    public static final Map<Integer, String> CONTROL_FUNCTIONS;

    public static String getName(int cc) {
        return MidiControlMap.CONTROL_FUNCTIONS.getOrDefault(cc, "Unknown Controller (" + cc + ")");
    }

    /***
        * Returns the name of the controller function for a given MIDI Control Change number.
        *
        * @param cc The MIDI Control Change number (0-127).
        * @return The name of the controller function, or "Unknown Controller" if not found.
        */

    static {
        CONTROL_FUNCTIONS = new HashMap<>();
        CONTROL_FUNCTIONS.put(0, "Bank Select (Coarse)");
        CONTROL_FUNCTIONS.put(1, "Modulation Wheel (Coarse)");
        CONTROL_FUNCTIONS.put(2, "Breath Controller (Coarse)");
        CONTROL_FUNCTIONS.put(3, "Undefined (3)");
        CONTROL_FUNCTIONS.put(4, "Foot Controller (Coarse)");
        CONTROL_FUNCTIONS.put(5, "Portamento Time (Coarse)");
        CONTROL_FUNCTIONS.put(6, "Data Entry (Coarse)");
        CONTROL_FUNCTIONS.put(7, "Main Volume (Coarse)");
        CONTROL_FUNCTIONS.put(8, "Balance (Coarse)");
        CONTROL_FUNCTIONS.put(9, "Undefined (9)");
        CONTROL_FUNCTIONS.put(10, "Pan (Coarse)");
        CONTROL_FUNCTIONS.put(11, "Expression (Coarse)");
        CONTROL_FUNCTIONS.put(12, "Effect Control 1 (Coarse)");
        CONTROL_FUNCTIONS.put(13, "Effect Control 2 (Coarse)");
        CONTROL_FUNCTIONS.put(14, "Undefined (14)");
        CONTROL_FUNCTIONS.put(15, "Undefined (15)");
        CONTROL_FUNCTIONS.put(16, "General Purpose Slider 1");
        CONTROL_FUNCTIONS.put(17, "General Purpose Slider 2");
        CONTROL_FUNCTIONS.put(18, "General Purpose Slider 3");
        CONTROL_FUNCTIONS.put(19, "General Purpose Slider 4");
        CONTROL_FUNCTIONS.put(20, "Undefined (20)");
        CONTROL_FUNCTIONS.put(21, "Undefined (21)");
        CONTROL_FUNCTIONS.put(22, "Undefined (22)");
        CONTROL_FUNCTIONS.put(23, "Undefined (23)");
        CONTROL_FUNCTIONS.put(24, "Undefined (24)");
        CONTROL_FUNCTIONS.put(25, "Undefined (25)");
        CONTROL_FUNCTIONS.put(26, "Undefined (26)");
        CONTROL_FUNCTIONS.put(27, "Undefined (27)");
        CONTROL_FUNCTIONS.put(28, "Undefined (28)");
        CONTROL_FUNCTIONS.put(29, "Undefined (29)");
        CONTROL_FUNCTIONS.put(30, "Undefined (30)");
        CONTROL_FUNCTIONS.put(31, "Undefined (31)");
        CONTROL_FUNCTIONS.put(32, "Bank Select (Fine)");
        CONTROL_FUNCTIONS.put(33, "Modulation Wheel (Fine)");
        CONTROL_FUNCTIONS.put(34, "Breath Controller (Fine)");
        CONTROL_FUNCTIONS.put(35, "Undefined (35)");
        CONTROL_FUNCTIONS.put(36, "Foot Controller (Fine)");
        CONTROL_FUNCTIONS.put(37, "Portamento Time (Fine)");
        CONTROL_FUNCTIONS.put(38, "Data Entry (Fine)");
        CONTROL_FUNCTIONS.put(39, "Main Volume (Fine)");
        CONTROL_FUNCTIONS.put(40, "Balance (Fine)");
        CONTROL_FUNCTIONS.put(41, "Undefined (41)");
        CONTROL_FUNCTIONS.put(42, "Pan (Fine)");
        CONTROL_FUNCTIONS.put(43, "Expression (Fine)");
        CONTROL_FUNCTIONS.put(44, "Effect Control 1 (Fine)");
        CONTROL_FUNCTIONS.put(45, "Effect Control 2 (Fine)");
        CONTROL_FUNCTIONS.put(46, "Undefined (46)");
        CONTROL_FUNCTIONS.put(47, "Undefined (47)");
        CONTROL_FUNCTIONS.put(48, "Undefined (48)");
        CONTROL_FUNCTIONS.put(49, "Undefined (49)");
        CONTROL_FUNCTIONS.put(50, "Undefined (50)");
        CONTROL_FUNCTIONS.put(51, "Undefined (51)");
        CONTROL_FUNCTIONS.put(52, "Undefined (52)");
        CONTROL_FUNCTIONS.put(53, "Undefined (53)");
        CONTROL_FUNCTIONS.put(54, "Undefined (54)");
        CONTROL_FUNCTIONS.put(55, "Undefined (55)");
        CONTROL_FUNCTIONS.put(56, "Undefined (56)");
        CONTROL_FUNCTIONS.put(57, "Undefined (57)");
        CONTROL_FUNCTIONS.put(58, "Undefined (58)");
        CONTROL_FUNCTIONS.put(59, "Undefined (59)");
        CONTROL_FUNCTIONS.put(60, "Undefined (60)");
        CONTROL_FUNCTIONS.put(61, "Undefined (61)");
        CONTROL_FUNCTIONS.put(62, "Undefined (62)");
        CONTROL_FUNCTIONS.put(63, "Undefined (63)");
        CONTROL_FUNCTIONS.put(64, "Hold Pedal (Sustain)");
        CONTROL_FUNCTIONS.put(65, "Portamento Switch");
        CONTROL_FUNCTIONS.put(66, "Sostenuto Pedal");
        CONTROL_FUNCTIONS.put(67, "Soft Pedal");
        CONTROL_FUNCTIONS.put(68, "Legato Pedal");
        CONTROL_FUNCTIONS.put(69, "Hold 2 Pedal");
        CONTROL_FUNCTIONS.put(70, "Sound Variation");
        CONTROL_FUNCTIONS.put(71, "Timbre / Resonance");
        CONTROL_FUNCTIONS.put(72, "Release Time");
        CONTROL_FUNCTIONS.put(73, "Attack Time");
        CONTROL_FUNCTIONS.put(74, "Brightness");
        CONTROL_FUNCTIONS.put(75, "Sound Control 6");
        CONTROL_FUNCTIONS.put(76, "Sound Control 7");
        CONTROL_FUNCTIONS.put(77, "Sound Control 8");
        CONTROL_FUNCTIONS.put(78, "Sound Control 9");
        CONTROL_FUNCTIONS.put(79, "Sound Control 10");
        CONTROL_FUNCTIONS.put(80, "General Purpose Button 1");
        CONTROL_FUNCTIONS.put(81, "General Purpose Button 2");
        CONTROL_FUNCTIONS.put(82, "General Purpose Button 3");
        CONTROL_FUNCTIONS.put(83, "General Purpose Button 4");
        CONTROL_FUNCTIONS.put(84, "Undefined (84)");
        CONTROL_FUNCTIONS.put(85, "Undefined (85)");
        CONTROL_FUNCTIONS.put(86, "Undefined (86)");
        CONTROL_FUNCTIONS.put(87, "Undefined (87)");
        CONTROL_FUNCTIONS.put(88, "Undefined (88)");
        CONTROL_FUNCTIONS.put(89, "Undefined (89)");
        CONTROL_FUNCTIONS.put(90, "Undefined (90)");
        CONTROL_FUNCTIONS.put(91, "Reverb Level");
        CONTROL_FUNCTIONS.put(92, "Tremolo Level");
        CONTROL_FUNCTIONS.put(93, "Chorus Level");
        CONTROL_FUNCTIONS.put(94, "Celeste Level (Detune)");
        CONTROL_FUNCTIONS.put(95, "Phaser Level");
        CONTROL_FUNCTIONS.put(96, "Data Button Increment");
        CONTROL_FUNCTIONS.put(97, "Data Button Decrement");
        CONTROL_FUNCTIONS.put(98, "Non-Registered Parameter (Coarse)");
        CONTROL_FUNCTIONS.put(99, "Non-Registered Parameter (Fine)");
        CONTROL_FUNCTIONS.put(100, "Registered Parameter (Coarse)");
        CONTROL_FUNCTIONS.put(101, "Registered Parameter (Fine)");
        CONTROL_FUNCTIONS.put(102, "Undefined (102)");
        CONTROL_FUNCTIONS.put(103, "Undefined (103)");
        CONTROL_FUNCTIONS.put(104, "Undefined (104)");
        CONTROL_FUNCTIONS.put(105, "Undefined (105)");
        CONTROL_FUNCTIONS.put(106, "Undefined (106)");
        CONTROL_FUNCTIONS.put(107, "Undefined (107)");
        CONTROL_FUNCTIONS.put(108, "Undefined (108)");
        CONTROL_FUNCTIONS.put(109, "Undefined (109)");
        CONTROL_FUNCTIONS.put(110, "Undefined (110)");
        CONTROL_FUNCTIONS.put(111, "Undefined (111)");
        CONTROL_FUNCTIONS.put(112, "Undefined (112)");
        CONTROL_FUNCTIONS.put(113, "Undefined (113)");
        CONTROL_FUNCTIONS.put(114, "Undefined (114)");
        CONTROL_FUNCTIONS.put(115, "Undefined (115)");
        CONTROL_FUNCTIONS.put(116, "Undefined (116)");
        CONTROL_FUNCTIONS.put(117, "Undefined (117)");
        CONTROL_FUNCTIONS.put(118, "Undefined (118)");
        CONTROL_FUNCTIONS.put(119, "Undefined (119)");
        CONTROL_FUNCTIONS.put(120, "All Sound Off");
        CONTROL_FUNCTIONS.put(121, "Reset All Controllers");
        CONTROL_FUNCTIONS.put(122, "Local Control");
        CONTROL_FUNCTIONS.put(123, "All Notes Off");
        CONTROL_FUNCTIONS.put(124, "Omni Mode Off");
        CONTROL_FUNCTIONS.put(125, "Omni Mode On");
        CONTROL_FUNCTIONS.put(126, "Mono Mode On");
        CONTROL_FUNCTIONS.put(127, "Poly Mode On");
    }
}
