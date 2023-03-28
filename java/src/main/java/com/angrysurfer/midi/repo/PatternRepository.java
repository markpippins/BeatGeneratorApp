package com.angrysurfer.midi.repo;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.midi.model.Pattern;

public interface PatternRepository  extends JpaRepository<Pattern, Long> {

    Set<Pattern> findBySongId(Long id);
}
