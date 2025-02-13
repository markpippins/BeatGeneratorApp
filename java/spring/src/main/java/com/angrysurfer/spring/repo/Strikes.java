package com.angrysurfer.spring.repo;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.angrysurfer.core.api.IPlayer;

public interface Strikes extends JpaRepository<IPlayer, Long> {

   // @Query(value="select s.id from strike s where s.ticker_id  = :tickerId", nativeQuery = true)
   // List<Long> getIdsForTicker(@Param("tickerId") Long ticker_id);
    
   @Query(value="select p.* from strike p where p.ticker_id = :tickerId", nativeQuery = true)
   Set<IPlayer> findByTickerId(@Param("tickerId") Long tickerId);
}

