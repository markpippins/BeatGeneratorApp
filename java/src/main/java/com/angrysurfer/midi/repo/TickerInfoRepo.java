package com.angrysurfer.midi.repo;

import com.angrysurfer.midi.model.TickerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TickerInfoRepo extends JpaRepository<TickerInfo, Long> {
    String NEXT_TICKER = "select * from ticker_info ti where ti.id = (select min(tii.id) from ticker_info tii WHERE tii.id > :currentTickerId)";
    @Query(value=NEXT_TICKER, nativeQuery = true)
    TickerInfo getNextTicker(@Param("currentTickerId") Long currentTickerId);
    
    String PREV_TICKER = "select * from ticker_info ti where ti.id = (select max(tii.id) from ticker_info tii WHERE tii.id < :currentTickerId)";
    @Query(value=PREV_TICKER, nativeQuery = true)
    TickerInfo getPreviousTicker(@Param("currentTickerId") Long currentTickerId);

    String MIN_TICKER = "select min(ti.id) from ticker_info";
    @Query(value=MIN_TICKER, nativeQuery = true)
    Long getMinimumTickerId();

}
