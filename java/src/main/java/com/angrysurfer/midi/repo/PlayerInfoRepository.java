package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.config.PlayerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerInfoRepository extends JpaRepository<PlayerInfo, Long> {
}

