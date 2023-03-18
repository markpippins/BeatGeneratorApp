package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TickerRepo extends JpaRepository<Ticker, Long> {
    
    String NEXT_TICKER = "select * from ticker ti where ti.id = (select min(tii.id) from ticker tii WHERE tii.id > :currentTickerId)";
    @Query(value=NEXT_TICKER, nativeQuery = true)
    Ticker getNextTicker(@Param("currentTickerId") Long currentTickerId);
    
    String PREV_TICKER = "select * from ticker ti where ti.id = (select max(tii.id) from ticker tii WHERE tii.id < :currentTickerId)";
    @Query(value=PREV_TICKER, nativeQuery = true)
    Ticker getPreviousTicker(@Param("currentTickerId") Long currentTickerId);

    String NEXT_TICKER_ID = "select min(ti.id) from ticker ti where ti.id > :currentTickerId)";
    @Query(value=NEXT_TICKER, nativeQuery = true)
    Long getNextTickerId(@Param("currentTickerId") Long currentTickerId);

    String MIN_TICKER = "select min(ti.id) from ticker ti";
    @Query(value=MIN_TICKER, nativeQuery = true)
    Long getMinimumTickerId();

    String MAX_TICKER = "select max(ti.id) from ticker ti";
    @Query(value=MAX_TICKER, nativeQuery = true)
    Long getMaximumTickerId();
}
