package com.angrysurfer.core.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class SessionDeserializer extends StdDeserializer<Session> {
    
    public SessionDeserializer() {
        this(null);
    }
    
    public SessionDeserializer(Class<?> vc) {
        super(vc);
    }
    
    @Override
    public Session deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        
        // Create a new session that will properly initialize all transient fields
        Session session = new Session();
        
        // Read properties
        if (node.has("id")) {
            session.setId(node.get("id").asLong());
        }
        if (node.has("name")) {
            session.setName(node.get("name").asText());
        }
        
        // Add other properties here...
        
        return session;
    }
}