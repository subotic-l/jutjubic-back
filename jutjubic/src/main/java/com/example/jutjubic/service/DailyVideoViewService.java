package com.example.jutjubic.service;

import com.example.jutjubic.model.DailyVideoView;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.DailyVideoViewRepository;
import com.example.jutjubic.repository.VideoPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyVideoViewService {

    private final DailyVideoViewRepository dailyVideoViewRepository;
    private final VideoPostRepository videoPostRepository;

    @Transactional
    public void recordView(Long videoId) {
        LocalDate today = LocalDate.now();
        
        int updated = dailyVideoViewRepository.incrementViewCount(videoId, today);
        
        if (updated == 0) {
            VideoPost videoPost = videoPostRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));
            
            DailyVideoView dailyView = new DailyVideoView();
            dailyView.setVideoPost(videoPost);
            dailyView.setViewDate(today);
            dailyView.setViewCount(1L);
            
            dailyVideoViewRepository.save(dailyView);
        }
        
        log.debug("Recorded view for video {} on date {}", videoId, today);
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldViews() {
        LocalDate cutoffDate = LocalDate.now().minusDays(8);
        var oldViews = dailyVideoViewRepository.findViewsOlderThan(cutoffDate);
        
        if (!oldViews.isEmpty()) {
            dailyVideoViewRepository.deleteAll(oldViews);
            log.info("Cleaned up {} old daily view records", oldViews.size());
        }
    }
}
