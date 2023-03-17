package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.Step;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StepRepository  extends JpaRepository<Step, Long> {

    List<Step> findBySongId(@Param("songId") Long songId);

    String FIND_BY_SONG_PAGE_AND_POSITION = "SELECT * FROM step_data sd WHERE sd.songId = :songId AND sd.pageId = :pageId AND sd.position = :position";
    @Query(value=FIND_BY_SONG_PAGE_AND_POSITION, nativeQuery = true)
    Optional<Step> findBySongIdPageIdAndPosition(@Param("songId") Long songId, @Param("pageId") Long pageId, @Param("position") int position);
}