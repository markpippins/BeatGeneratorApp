package com.angrysurfer.sequencer.repo;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.angrysurfer.sequencer.model.Step;

public interface StepRepo extends JpaRepository<Step, Long> {

    Set<Step> findByPatternId(@Param("patternId") Long patternId);

    // String FIND_BY_SONG_PAGE_AND_POSITION = "SELECT * FROM step sd WHERE
    // sd.songId = :songId AND sd.page = :page AND sd.position = :position";
    // @Query(value=FIND_BY_SONG_PAGE_AND_POSITION, nativeQuery = true)
    // Optional<Pattern> findBySongIdpageAndPosition(@Param("songId") Long songId,
    // @Param("page") Long page, @Param("position") int position);
}
