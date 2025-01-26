package com.angrysurfer.sequencer.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.sequencer.model.ui.Caption;

public interface CaptionRepo  extends JpaRepository<Caption, Long> {
}
