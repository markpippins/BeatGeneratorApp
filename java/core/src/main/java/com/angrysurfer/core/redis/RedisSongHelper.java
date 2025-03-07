package com.angrysurfer.core.redis;

import java.util.Set;
import java.util.logging.Logger;

import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Song;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class RedisSongHelper {
    private static final Logger logger = Logger.getLogger(RedisSongHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final RedisPatternHelper patternHelper;

    public RedisSongHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
        this.patternHelper = new RedisPatternHelper(jedisPool, objectMapper);
    }

    public Song findSongById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("song:" + id);
            if (json != null) {
                Song song = objectMapper.readValue(json, Song.class);
                Set<Pattern> patterns = patternHelper.findPatternsForSong(id);
                song.setPatterns(patterns);
                patterns.forEach(p -> p.setSong(song));
                return song;
            }
            return null;
        } catch (Exception e) {
            logger.severe("Error finding song: " + e.getMessage());
            return null;
        }
    }

    public Song saveSong(Song song) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (song.getId() == null) {
                song.setId(jedis.incr("seq:song"));
            }

            // Save all patterns first
            if (song.getPatterns() != null) {
                song.getPatterns().forEach(pattern -> patternHelper.savePattern(pattern));
            }

            // Temporarily remove circular references
            Set<Pattern> patterns = song.getPatterns();
            song.setPatterns(null);

            // Save the song
            String json = objectMapper.writeValueAsString(song);
            jedis.set("song:" + song.getId(), json);

            // Restore references
            song.setPatterns(patterns);
            return song;
        } catch (Exception e) {
            logger.severe("Error saving song: " + e.getMessage());
            throw new RuntimeException("Failed to save song", e);
        }
    }

    public Long getMinimumSongId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("song:*");
            return keys.isEmpty() ? null : keys.stream()
                .map(key -> Long.parseLong(key.split(":")[1]))
                .min(Long::compareTo)
                .orElse(null);
        } catch (Exception e) {
            logger.severe("Error getting minimum song ID: " + e.getMessage());
            return null;
        }
    }

    public Long getMaximumSongId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("song:*");
            return keys.isEmpty() ? null : keys.stream()
                .map(key -> Long.parseLong(key.split(":")[1]))
                .max(Long::compareTo)
                .orElse(null);
        }
    }

    public Long getNextSongId(Long currentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("song:*");
            return keys.stream()
                .map(key -> Long.parseLong(key.split(":")[1]))
                .filter(id -> id > currentId)
                .min(Long::compareTo)
                .orElse(null);
        }
    }

    public Long getPreviousSongId(Long currentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("song:*");
            return keys.stream()
                .map(key -> Long.parseLong(key.split(":")[1]))
                .filter(id -> id < currentId)
                .max(Long::compareTo)
                .orElse(null);
        }
    }

    public void deleteSong(Long songId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Song song = findSongById(songId);
            if (song != null) {
                // Delete all patterns
                if (song.getPatterns() != null) {
                    song.getPatterns().forEach(pattern -> 
                        patternHelper.deletePattern(pattern.getId()));
                }
                // Delete the song
                jedis.del("song:" + songId);
            }
        } catch (Exception e) {
            logger.severe("Error deleting song: " + e.getMessage());
            throw new RuntimeException("Failed to delete song", e);
        }
    }
}