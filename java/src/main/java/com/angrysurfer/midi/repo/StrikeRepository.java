package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.Strike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrikeRepository extends JpaRepository<Strike, Long> {
}

