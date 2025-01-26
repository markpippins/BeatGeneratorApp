package com.angrysurfer.sequencer.repo;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.sequencer.model.Pattern;

public interface PatternRepo  extends JpaRepository<Pattern, Long> {

    Set<Pattern> findBySongId(Long id);
}
