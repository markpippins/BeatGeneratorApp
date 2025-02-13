package com.angrysurfer.spring.repo;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.angrysurfer.core.api.IRule;

public interface Rules extends JpaRepository<IRule, Long> {
    String FIND_BY_PLAYER_ID = "select * from rule r where r.player_id = :playerId";

    @Query(value = FIND_BY_PLAYER_ID, nativeQuery = true)
    public Set<IRule> findByPlayerId(@Param("playerId") Long playerId);
}
