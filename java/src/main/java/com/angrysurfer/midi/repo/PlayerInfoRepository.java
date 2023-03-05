package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.config.PlayerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerInfoRepository extends JpaRepository<PlayerInfo, Long> {
    @Query("select p from PlayerInfo p where p.tickerId = ?1")
    List<PlayerInfo> findByTickerId(Long id);
}

