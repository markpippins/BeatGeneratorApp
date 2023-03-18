package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.Strike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Set;

public interface StrikeRepository extends JpaRepository<Strike, Long> {
    Set<Strike> findByTickerId(Long id);
//    @Query(value="select p.* from player p where p.ticker_id = :tickerId", nativeQuery = true)
//    Set<Player> findByTickerId(@Param("tickerId") Long tickerId);
}

