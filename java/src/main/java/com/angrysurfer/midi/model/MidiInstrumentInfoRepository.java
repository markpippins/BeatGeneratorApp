package com.angrysurfer.midi.model.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MidiInstrumentInfoRepository extends JpaRepository<MidiInstrumentInfo, Long> {

}