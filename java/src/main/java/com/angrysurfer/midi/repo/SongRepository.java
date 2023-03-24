package com.angrysurfer.midi.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angrysurfer.midi.model.Song;

public interface SongRepository  extends JpaRepository<Song, Long> {

    // List<StepData> findBySongId(@Param("songId") Long songId);

    // String FIND_BY_SONG_PAGE_AND_POSITION = "SELECT * FROM step_data sd WHERE sd.songId = :songId AND sd.page = :page AND sd.position = :position";
    // @Query(value=FIND_BY_SONG_PAGE_AND_POSITION, nativeQuery = true)
    // Optional<StepData> findBySongIdpageAndPosition(@Param("songId") Long songId, @Param("page") Long page, @Param("position") int position);
}
