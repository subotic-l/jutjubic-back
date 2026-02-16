package com.example.jutjubic.repository;

import com.example.jutjubic.model.DailyVideoView;
import com.example.jutjubic.model.VideoPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyVideoViewRepository extends JpaRepository<DailyVideoView, Long> {

    Optional<DailyVideoView> findByVideoPostAndViewDate(VideoPost videoPost, LocalDate viewDate);

    @Modifying
    @Query("UPDATE DailyVideoView d SET d.viewCount = d.viewCount + 1 " +
           "WHERE d.videoPost.id = :videoId AND d.viewDate = :viewDate")
    int incrementViewCount(@Param("videoId") Long videoId, @Param("viewDate") LocalDate viewDate);

    @Query("SELECT d FROM DailyVideoView d WHERE d.viewDate >= :startDate ORDER BY d.viewDate DESC")
    List<DailyVideoView> findViewsSince(@Param("startDate") LocalDate startDate);

    @Query("SELECT d FROM DailyVideoView d WHERE d.viewDate < :cutoffDate")
    List<DailyVideoView> findViewsOlderThan(@Param("cutoffDate") LocalDate cutoffDate);
}
