package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.Rule;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RuleRepo  extends JpaRepository<Rule, Long> {
    String FIND_BY_PLAYER_ID = "select * from rule r where r.player_id = :playerId";
    @Query(value=FIND_BY_PLAYER_ID, nativeQuery = true)
    public Set<Rule> findByPlayerId(@Param("playerId") Long playerId);
}
