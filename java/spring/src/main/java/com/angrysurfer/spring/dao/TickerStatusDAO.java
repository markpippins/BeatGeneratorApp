// package com.angrysurfer.spring.dao;

// import java.util.List;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.stereotype.Repository;

// @Repository
// public class TickerStatusDAO {

//     @Autowired
//     private RedisTemplate template;

//     public static final String HASH_KEY = "TickerStatus";

//     public TickerStatus save(TickerStatus tickerStatus) {
//         template.opsForHash().put(HASH_KEY, tickerStatus.getId(), tickerStatus);
//         return tickerStatus;
//     }

//     public List<TickerStatus> findAll() {
//         return template.opsForHash().values(HASH_KEY);
//     }

//     public TickerStatus findById(long id) {
//         return (TickerStatus) template.opsForHash().get(HASH_KEY, id);
//     }

//     public String delete(long id) {
//         template.opsForHash().delete(HASH_KEY, id);
//         return "TickerStatus removed";
//     }
// }
