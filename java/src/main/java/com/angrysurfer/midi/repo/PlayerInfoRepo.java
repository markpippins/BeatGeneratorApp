// package com.angrysurfer.midi.repo;

// import com.angrysurfer.midi.model.PlayerInfo;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;

// import java.util.List;

// public interface PlayerInfoRepository extends JpaRepository<PlayerInfo, Long> {
//     @Query(value="select p.* from player_info p where p.ticker_id = :tickerId", nativeQuery = true)
//     List<PlayerInfo> findByTickerId(@Param("tickerId") Long tickerId);
// }

