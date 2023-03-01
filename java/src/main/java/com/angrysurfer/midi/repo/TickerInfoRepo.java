package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.config.TickerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TickerInfoRepo  extends JpaRepository<TickerInfo, Long> {
}
