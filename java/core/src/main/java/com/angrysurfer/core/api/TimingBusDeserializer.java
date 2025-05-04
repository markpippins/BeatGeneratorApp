package com.angrysurfer.core.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Custom deserializer for TimingBus that always returns the singleton instance
 */
public class TimingBusDeserializer extends StdDeserializer<TimingBus> {
    
    public TimingBusDeserializer() {
        this(null);
    }
    
    public TimingBusDeserializer(Class<?> vc) {
        super(vc);
    }
    
    @Override
    public TimingBus deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Skip the content for this object by advancing the parser to the end of the object
        p.skipChildren();
        
        // Return the singleton instance
        return TimingBus.getInstance();
    }
}