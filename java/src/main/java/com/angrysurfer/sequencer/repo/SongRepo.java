package com.angrysurfer.sequencer.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.angrysurfer.sequencer.model.Song;

public interface SongRepo  extends JpaRepository<Song, Long> {

    String NEXT_SONG = "select * from song s where s.id = (select min(so.id) from song so WHERE so.id > :currentSongId)";
    @Query(value=NEXT_SONG, nativeQuery = true)
    Song getNextSong(@Param("currentSongId") Long currentSongId);
    
    String PREV_SONG = "select * from song s where s.id = (select max(so.id) from song so WHERE so.id < :currentSongId)";
    @Query(value=PREV_SONG, nativeQuery = true)
    Song getPreviousSong(@Param("currentSongId") Long currentSongId);

    String NEXT_SONG_ID = "select min(s.id) from song s where s.id > :currentSongId)";
    @Query(value=NEXT_SONG, nativeQuery = true)
    Long getNextSongId(@Param("currentSongId") Long currentSongId);

    String MIN_SONG = "select min(s.id) from song s";
    @Query(value=MIN_SONG, nativeQuery = true)
    Long getMinimumSongId();

    String MAX_SONG = "select max(s.id) from song s";
    @Query(value=MAX_SONG, nativeQuery = true)
    Long getMaximumSongId();
}
