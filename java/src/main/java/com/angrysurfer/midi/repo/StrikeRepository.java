package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.Strike;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StrikeRepository extends JpaRepository<Strike, Long> {

   // @Query(value="select s.id from strike s where s.ticker_id  = :tickerId", nativeQuery = true)
   // List<Long> getIdsForTicker(@Param("tickerId") Long ticker_id);
    
   // @Query(value="select p.* from strike p where p.ticker_id = :tickerId", nativeQuery = true)
   // Set<Strike> findByTickerId(@Param("tickerId") Long tickerId);
}

