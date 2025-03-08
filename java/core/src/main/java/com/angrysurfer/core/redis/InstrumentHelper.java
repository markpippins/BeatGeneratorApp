package com.angrysurfer.core.redis;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.angrysurfer.core.model.midi.Instrument;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class InstrumentHelper {
    private static final Logger logger = Logger.getLogger(InstrumentHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public InstrumentHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    public List<Instrument> findAllInstruments() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.keys("instrument:*").stream()
                .map(key -> findInstrumentById(Long.parseLong(key.split(":")[1])))
                .filter(i -> i != null)
                .collect(Collectors.toList());
        }
    }

    public Instrument findInstrumentById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("instrument:" + id);
            return json != null ? objectMapper.readValue(json, Instrument.class) : null;
        } catch (Exception e) {
            logger.severe("Error finding instrument: " + e.getMessage());
            return null;
        }
    }

    public void saveInstrument(Instrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (instrument.getId() == null) {
                instrument.setId(jedis.incr("seq:instrument"));
            }
            String json = objectMapper.writeValueAsString(instrument);
            jedis.set("instrument:" + instrument.getId(), json);
        } catch (Exception e) {
            logger.severe("Error saving instrument: " + e.getMessage());
            throw new RuntimeException("Failed to save instrument", e);
        }
    }

    public void deleteInstrument(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("instrument:" + id);
        } catch (Exception e) {
            logger.severe("Error deleting instrument: " + e.getMessage());
            throw new RuntimeException("Failed to delete instrument", e);
        }
    }
}