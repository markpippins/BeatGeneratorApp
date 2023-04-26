package com.angrysurfer.midi.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.midi.model.Caption;

public interface CaptionRepo  extends JpaRepository<Caption, Long> {
}
