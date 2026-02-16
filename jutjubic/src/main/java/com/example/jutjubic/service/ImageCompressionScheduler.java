package com.example.jutjubic.service;

import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.VideoPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageCompressionScheduler {

    private final VideoPostRepository videoPostRepository;
    private final ImageCompressionService imageCompressionService;

    /**
     * Scheduled task koji se pokreće svaki dan u 03:00.
     * Kompresuje sve thumbnail slike starije od 30 dana.
     */
    @Scheduled(cron = "0 0 3 * * *") // Svaki dan u 03:00
    @Transactional
    public void compressOldThumbnails() {
        log.info("Starting scheduled thumbnail compression task...");

        // Datum pre 30 dana
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Pronađi sve video postove starije od 30 dana bez kompresovanih thumbnails
        List<VideoPost> videosToCompress = videoPostRepository.findAll().stream()
                .filter(video -> video.getCreatedAt().isBefore(thirtyDaysAgo))
                .filter(video -> video.getCompressedThumbnailPath() == null)
                .toList();

        log.info("Found {} thumbnails older than 30 days to compress", videosToCompress.size());

        int successCount = 0;
        int failCount = 0;

        for (VideoPost video : videosToCompress) {
            try {
                String originalThumbnail = video.getThumbnailPath();
                String compressedPath = imageCompressionService.compressImage(originalThumbnail);

                if (compressedPath != null) {
                    video.setCompressedThumbnailPath(compressedPath);
                    video.setThumbnailCompressedAt(LocalDateTime.now());
                    videoPostRepository.save(video);
                    successCount++;
                    log.info("Compressed thumbnail for video ID {}: {}", video.getId(), compressedPath);
                } else {
                    failCount++;
                    log.warn("Failed to compress thumbnail for video ID {}", video.getId());
                }

            } catch (Exception e) {
                failCount++;
                log.error("Error compressing thumbnail for video ID {}: {}", video.getId(), e.getMessage(), e);
            }
        }

        log.info("Thumbnail compression task completed. Success: {}, Failed: {}", successCount, failCount);
    }

    /**
     * Manual trigger za testiranje (opcionalno).
     * Možeš pozvati preko REST endpoint-a.
     */
    @Transactional
    public void compressAllOldThumbnailsManually() {
        log.info("Manually triggered thumbnail compression");
        compressOldThumbnails();
    }
}