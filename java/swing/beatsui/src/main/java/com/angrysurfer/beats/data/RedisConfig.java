package com.angrysurfer.beats.data;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConfig {
    private static JedisPool jedisPool;

    public static JedisPool getJedisPool() {
        if (jedisPool == null) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(4);
            poolConfig.setMaxIdle(2);
            jedisPool = new JedisPool(poolConfig, "localhost", 6379);
        }
        return jedisPool;
    }
}
