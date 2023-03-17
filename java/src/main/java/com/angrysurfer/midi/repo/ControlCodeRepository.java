package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.ControlCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlCodeRepository  extends JpaRepository<ControlCode, Long> {
}
