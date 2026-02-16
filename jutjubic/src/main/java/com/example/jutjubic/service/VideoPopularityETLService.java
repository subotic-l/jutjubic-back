package com.example.jutjubic.service;

import com.example.jutjubic.model.DailyVideoView;
import com.example.jutjubic.model.PopularityReport;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.DailyVideoViewRepository;
import com.example.jutjubic.repository.PopularityReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoPopularityETLService {

    private final DailyVideoViewRepository dailyVideoViewRepository;
    private final PopularityReportRepository popularityReportRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runETLPipeline() {
        log.info("Starting ETL pipeline for video popularity calculation");
        
        try {
            // EXTRACT
            List<DailyVideoView> recentViews = extractRecentViews();
            log.info("Extracted {} daily view records from the last 7 days", recentViews.size());
            
            // TRANSFORM
            Map<VideoPost, Double> popularityScores = transformToPopularityScores(recentViews);
            log.info("Calculated popularity scores for {} videos", popularityScores.size());
            
            // LOAD
            loadTopVideos(popularityScores);
            log.info("ETL pipeline completed successfully");
            
        } catch (Exception e) {
            log.error("Error during ETL pipeline execution", e);
            throw e;
        }
    }

    private List<DailyVideoView> extractRecentViews() {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        return dailyVideoViewRepository.findViewsSince(sevenDaysAgo);
    }

    private Map<VideoPost, Double> transformToPopularityScores(List<DailyVideoView> views) {
        LocalDate today = LocalDate.now();
        Map<VideoPost, Double> scores = new HashMap<>();
        
        for (DailyVideoView view : views) {
            long daysAgo = today.toEpochDay() - view.getViewDate().toEpochDay();
            
            if (daysAgo > 7) {
                continue;
            }
            
            double weight = 7 - daysAgo + 1;
            
            double weightedScore = view.getViewCount() * weight;
            
            scores.merge(view.getVideoPost(), weightedScore, Double::sum);
            
            log.debug("Video {}: {} views {} days ago, weight={}, weighted_score={}", 
                    view.getVideoPost().getId(), view.getViewCount(), daysAgo, weight, weightedScore);
        }
        
        return scores;
    }

    private void loadTopVideos(Map<VideoPost, Double> popularityScores) {
        if (popularityScores.isEmpty()) {
            log.warn("No videos with views in the last 7 days, skipping report generation");
            return;
        }
        
        List<Map.Entry<VideoPost, Double>> sortedVideos = popularityScores.entrySet()
                .stream()
                .sorted(Map.Entry.<VideoPost, Double>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());
        
        if (sortedVideos.size() < 3) {
            log.warn("Less than 3 videos with views in the last 7 days (found {}), skipping report generation", 
                    sortedVideos.size());
            return;
        }
        
        PopularityReport report = new PopularityReport();
        report.setReportDate(LocalDateTime.now());
        
        report.setFirstVideo(sortedVideos.get(0).getKey());
        report.setFirstVideoScore(sortedVideos.get(0).getValue());
        
        report.setSecondVideo(sortedVideos.get(1).getKey());
        report.setSecondVideoScore(sortedVideos.get(1).getValue());
        
        report.setThirdVideo(sortedVideos.get(2).getKey());
        report.setThirdVideoScore(sortedVideos.get(2).getValue());
        
        popularityReportRepository.save(report);
        
        log.info("Saved popularity report: 1st={} (score={}), 2nd={} (score={}), 3rd={} (score={})",
                report.getFirstVideo().getTitle(), report.getFirstVideoScore(),
                report.getSecondVideo().getTitle(), report.getSecondVideoScore(),
                report.getThirdVideo().getTitle(), report.getThirdVideoScore());
    }

    @Transactional
    public void runETLPipelineManually() {
        log.info("Manually triggered ETL pipeline");
        runETLPipeline();
    }
}
