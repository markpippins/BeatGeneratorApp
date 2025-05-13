package com.angrysurfer.core.util;

import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class MelodicSequenceDataDeserializer extends StdDeserializer<MelodicSequenceData> {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceDataDeserializer.class);
    
    public MelodicSequenceDataDeserializer() {
        this(null);
    }
    
    public MelodicSequenceDataDeserializer(Class<?> vc) {
        super(vc);
    }
    
    @Override
    public MelodicSequenceData deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = mapper.readTree(jp);
        
        // Create a new MelodicSequenceData instance
        MelodicSequenceData data = new MelodicSequenceData();
        
        // Handle all standard fields using normal deserialization
        // But handle special case for rootNote
        if (node.has("rootNote")) {
            JsonNode rootNoteNode = node.get("rootNote");
            if (rootNoteNode.isTextual()) {
                // It's a note name like "C" - convert to MIDI note number
                String noteName = rootNoteNode.asText();
                int midiNote = NoteNameConverter.noteNameToMidi(noteName);
                data.setRootNote(midiNote);
                logger.debug("Converted note name {} to MIDI note {}", noteName, midiNote);
            } else if (rootNoteNode.isNumber()) {
                // It's already a number, use it directly
                data.setRootNote(rootNoteNode.asInt());
            } else {
                // Default to middle C
                data.setRootNote(60);
                logger.warn("Could not parse rootNote, defaulting to 60 (middle C)");
            }
        }
        
        // Handle all other fields
        if (node.has("id")) data.setId(node.get("id").asLong());
        if (node.has("sequencerId")) data.setSequencerId(node.get("sequencerId").asInt());
        if (node.has("patternLength")) data.setPatternLength(node.get("patternLength").asInt());
        if (node.has("direction")) data.setDirection(mapper.treeToValue(node.get("direction"), data.getDirection().getClass()));
        if (node.has("timingDivision")) data.setTimingDivision(mapper.treeToValue(node.get("timingDivision"), data.getTimingDivision().getClass()));
        if (node.has("looping")) data.setLooping(node.get("looping").asBoolean());
        if (node.has("octaveShift")) data.setOctaveShift(node.get("octaveShift").asInt());
        if (node.has("quantizeEnabled")) data.setQuantizeEnabled(node.get("quantizeEnabled").asBoolean());
        if (node.has("scale")) data.setScale(node.get("scale").asText());
        
        // Handle arrays
        if (node.has("activeSteps")) {
            JsonNode stepsNode = node.get("activeSteps");
            boolean[] activeSteps = new boolean[stepsNode.size()];
            for (int i = 0; i < stepsNode.size(); i++) {
                activeSteps[i] = stepsNode.get(i).asBoolean();
            }
            data.setActiveSteps(activeSteps);
        }
        
        if (node.has("noteValues")) {
            JsonNode notesNode = node.get("noteValues");
            int[] noteValues = new int[notesNode.size()];
            for (int i = 0; i < notesNode.size(); i++) {
                noteValues[i] = notesNode.get(i).asInt();
            }
            data.setNoteValues(noteValues);
        }
        
        if (node.has("velocityValues")) {
            JsonNode velNode = node.get("velocityValues");
            int[] velocityValues = new int[velNode.size()];
            for (int i = 0; i < velNode.size(); i++) {
                velocityValues[i] = velNode.get(i).asInt();
            }
            data.setVelocityValues(velocityValues);
        }
        
        if (node.has("gateValues")) {
            JsonNode gateNode = node.get("gateValues");
            int[] gateValues = new int[gateNode.size()];
            for (int i = 0; i < gateNode.size(); i++) {
                gateValues[i] = gateNode.get(i).asInt();
            }
            data.setGateValues(gateValues);
        }
        
        // Handle tilt values
        if (node.has("tiltValues")) {
            JsonNode tiltNode = node.get("tiltValues");
            int[] tiltValues = new int[tiltNode.size()];
            for (int i = 0; i < tiltNode.size(); i++) {
                tiltValues[i] = tiltNode.get(i).asInt();
            }
            data.setTiltValues(tiltValues);
            logger.debug("Loaded {} tilt values from JSON", tiltValues.length);
        } else {
            // If tilt values don't exist in the JSON, initialize with defaults
            logger.debug("No tilt values found in JSON, initializing defaults");
            
            // Create default tilt values based on pattern length
            if (data.getPatternLength() > 0) {
                int[] defaultTiltValues = new int[data.getPatternLength()];
                // Default is 0 (no tilt)
                for (int i = 0; i < defaultTiltValues.length; i++) {
                    defaultTiltValues[i] = 0;
                }
                data.setTiltValues(defaultTiltValues);
            }
        }

        // Handle mute values
        if (node.has("muteValues")) {
            JsonNode muteNode = node.get("muteValues");
            int[] muteValues = new int[muteNode.size()];
            for (int i = 0; i < muteNode.size(); i++) {
                muteValues[i] = muteNode.get(i).asInt();
            }
            data.setMuteValues(muteValues);
            logger.debug("Loaded {} mute values from JSON", muteValues.length);
        } else {
            // If mute values don't exist in the JSON, initialize with defaults
            logger.debug("No mute values found in JSON, initializing defaults");
            
            // Create default mute values based on pattern length
            if (data.getPatternLength() > 0) {
                int[] defaultMuteValues = new int[data.getPatternLength()];
                // Default is 0 (unmuted)
                Arrays.fill(defaultMuteValues, 0);
                data.setMuteValues(defaultMuteValues);
            }
        }
        
        return data;
    }
}