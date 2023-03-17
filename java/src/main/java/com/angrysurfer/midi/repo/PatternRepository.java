package com.angrysurfer.midi.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.midi.model.Pattern;

public interface PatternRepository  extends JpaRepository<Pattern, Long> {
}
