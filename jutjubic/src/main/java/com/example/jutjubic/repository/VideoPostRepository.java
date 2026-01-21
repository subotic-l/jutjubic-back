package com.example.jutjubic.repository;

import com.example.jutjubic.model.VideoPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VideoPostRepository extends JpaRepository<VideoPost, Long> {
    @Modifying
    @Query("UPDATE VideoPost v SET v.views = v.views + 1 WHERE v.id = :id")
    void incrementViews(@Param("id") Long id);

    List<VideoPost> findAllByOrderByCreatedAtDesc();

    @Query("SELECT v FROM VideoPost v WHERE " +
           "v.latitude IS NOT NULL AND v.longitude IS NOT NULL AND " +
           "v.latitude BETWEEN :minLat AND :maxLat AND " +
           "v.longitude BETWEEN :minLon AND :maxLon " +
           "ORDER BY v.createdAt DESC")
    List<VideoPost> findAllWithinBounds(
        @Param("minLat") Double minLatitude,
        @Param("maxLat") Double maxLatitude,
        @Param("minLon") Double minLongitude,
        @Param("maxLon") Double maxLongitude
    );
}
