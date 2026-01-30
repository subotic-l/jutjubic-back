package com.example.jutjubic.repository;

import com.example.jutjubic.model.VideoPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface VideoPostRepository extends JpaRepository<VideoPost, Long> {
    @Modifying
    @Query("UPDATE VideoPost v SET v.views = v.views + 1 WHERE v.id = :id")
    void incrementViews(@Param("id") Long id);

    List<VideoPost> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT v FROM VideoPost v WHERE " +
           "v.tileX = :x AND v.tileY = :y AND v.tileZoom = :zoom " +
           "ORDER BY v.createdAt DESC")
    List<VideoPost> findByTileCoordinates(
        @Param("x") Integer tileX,
        @Param("y") Integer tileY,
        @Param("zoom") Integer zoom
    );

    List<VideoPost> findAllByTileZoomAndTileXBetweenAndTileYBetween(
            Integer tileZoom,
            Integer tileXStart,
            Integer tileXEnd,
            Integer tileYStart,
            Integer tileYEnd
    );

    // NOVO: paginirano (ograničen broj videa po sekciji)
    Page<VideoPost> findByTileZoomAndTileXBetweenAndTileYBetween(
            Integer tileZoom,
            Integer tileXStart,
            Integer tileXEnd,
            Integer tileYStart,
            Integer tileYEnd,
            Pageable pageable
    );
}
