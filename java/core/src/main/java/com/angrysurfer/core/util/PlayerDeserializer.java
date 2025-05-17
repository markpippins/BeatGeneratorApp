package com.angrysurfer.core.util;

import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Strike;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PlayerDeserializer extends StdDeserializer<Player> {
    private static final Logger logger = LoggerFactory.getLogger(PlayerDeserializer.class);
    
    public PlayerDeserializer() {
        this(null);
    }
    
    public PlayerDeserializer(Class<?> vc) {
        super(vc);
    }
    
    @Override
    public Player deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = mapper.readTree(jp);
        
        // Get the player class name
        String playerClassName = null;
        if (node.has("playerClassName")) {
            playerClassName = node.get("playerClassName").asText();
        } else if (node.has("drumPlayer") && node.get("drumPlayer").asBoolean()) {
            playerClassName = "com.angrysurfer.core.model.Strike";
        } else if (node.has("melodicPlayer") && node.get("melodicPlayer").asBoolean()) {
            playerClassName = "com.angrysurfer.core.model.Note";
        } else {
            // Default to Strike if we can't determine
            logger.warn("Could not determine player type from JSON, defaulting to Strike");
            playerClassName = "com.angrysurfer.core.model.Strike";
        }
        
        try {
            // Instantiate the correct class
            if (playerClassName.contains("Strike")) {
                return mapper.treeToValue(node, Strike.class);
            } else if (playerClassName.contains("Note")) {
                return mapper.treeToValue(node, Note.class);
            } else {
                logger.error("Unknown player class: {}", playerClassName);
                throw new IOException("Unknown player class: " + playerClassName);
            }
        } catch (Exception e) {
            logger.error("Error deserializing player: {}", e.getMessage(), e);
            throw new IOException("Error deserializing player", e);
        }
    }
}