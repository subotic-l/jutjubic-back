package com.example.jutjubic.service;

import com.example.jutjubic.dto.PopularVideosResponse;
import com.example.jutjubic.dto.PopularVideosResponse.PopularVideoDto;
import com.example.jutjubic.model.PopularityReport;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.PopularityReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularVideosService {

    private final PopularityReportRepository popularityReportRepository;

    @Transactional(readOnly = true)
    public Optional<PopularVideosResponse> getPopularVideos() {
        Optional<PopularityReport> latestReport = popularityReportRepository.findLatestReport();
        
        if (latestReport.isEmpty()) {
            log.info("No popularity report found");
            return Optional.empty();
        }
        
        PopularityReport report = latestReport.get();
        PopularVideosResponse response = new PopularVideosResponse();
        response.setReportDate(report.getReportDate());
        
        List<PopularVideoDto> videos = new ArrayList<>();
        videos.add(mapToDto(report.getFirstVideo(), report.getFirstVideoScore()));
        videos.add(mapToDto(report.getSecondVideo(), report.getSecondVideoScore()));
        videos.add(mapToDto(report.getThirdVideo(), report.getThirdVideoScore()));
        
        response.setPopularVideos(videos);
        
        log.info("Retrieved popular videos from report dated {}", report.getReportDate());
        return Optional.of(response);
    }

    private PopularVideoDto mapToDto(VideoPost video, Double score) {
        PopularVideoDto dto = new PopularVideoDto();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setThumbnailPath(video.getThumbnailPath());
        dto.setPopularityScore(score);
        dto.setTotalViews(video.getViews());
        dto.setLikes(video.getLikes());
        dto.setUploaderUsername(video.getUser().getActualUsername());
        return dto;
    }
}
