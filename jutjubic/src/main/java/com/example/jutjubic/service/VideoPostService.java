package com.example.jutjubic.service;

import com.example.jutjubic.dto.LikeResponse;
import com.example.jutjubic.dto.TileCoordinate;
import com.example.jutjubic.dto.VideoPostRequest;
import com.example.jutjubic.dto.VideoPostResponse;
import com.example.jutjubic.dto.StreamInfoResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.VideoPostRepository;
import com.example.jutjubic.util.TileCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.jutjubic.exception.UnauthorizedActionException;
import com.example.jutjubic.util.TileRange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoPostService {

    private final VideoPostRepository videoPostRepository;
    private final DailyVideoViewService dailyVideoViewService;
    private static final String UPLOAD_DIR = "uploads";
    private static final String VIDEO_DIR = UPLOAD_DIR + "/videos";
    private static final String THUMBNAIL_DIR = UPLOAD_DIR + "/thumbnails";

    private static final int BASE_TILE_ZOOM = 12;

    private static final int HIGH_ZOOM_THRESHOLD = 11;   // veliki zoom (grad/ulica)
    private static final int MEDIUM_ZOOM_THRESHOLD = 7;  // srednji zoom (država/region)

    private static final int MEDIUM_LEVEL_MAX_VIDEOS_PER_TILE = 10;
    private static final int LOW_LEVEL_MAX_VIDEOS_PER_TILE = 3;

    private static final int DEFAULT_TILE_ZOOM = BASE_TILE_ZOOM;

    @Transactional(rollbackFor = Exception.class)
    public VideoPostResponse createVideoPost(VideoPostRequest request, User user) throws IOException {
        if (request.getVideo() == null || request.getVideo().isEmpty()) {
            throw new RuntimeException("Video file is required");
        }
        if (request.getThumbnail() == null || request.getThumbnail().isEmpty()) {
            throw new RuntimeException("Thumbnail file is required");
        }

        createDirectories();

        String videoFileName = UUID.randomUUID() + "_" + request.getVideo().getOriginalFilename();
        Path videoPath = Paths.get(VIDEO_DIR, videoFileName);

        String thumbnailFileName = UUID.randomUUID() + "_" + request.getThumbnail().getOriginalFilename();
        Path thumbnailPath = Paths.get(THUMBNAIL_DIR, thumbnailFileName);

        VideoPost videoPost = new VideoPost();
        videoPost.setTitle(request.getTitle());
        videoPost.setDescription(request.getDescription());
        videoPost.setTags(request.getTags());
        videoPost.setLongitude(request.getLongitude());
        videoPost.setLatitude(request.getLatitude());
        videoPost.setCreatedAt(LocalDateTime.now());
        videoPost.setUser(user);
        videoPost.setVideoUrl(videoPath.toString());
        videoPost.setThumbnailPath(thumbnailPath.toString());
        videoPost.setScheduledReleaseTime(request.getScheduledReleaseTime());
        videoPost.setVideoDurationSeconds(request.getVideoDurationSeconds());
        
        if (request.getLatitude() != null && request.getLongitude() != null) {
            TileCoordinate tile = TileCalculator.getTileForLocation(
                request.getLatitude(), 
                request.getLongitude(), 
                DEFAULT_TILE_ZOOM
            );
            videoPost.setTileX(tile.getX());
            videoPost.setTileY(tile.getY());
            videoPost.setTileZoom(tile.getZoom());
        }

        videoPost = videoPostRepository.save(videoPost);

        try {
            uploadFile(request.getVideo(), videoPath);
            uploadFile(request.getThumbnail(), thumbnailPath);
        } catch (IOException e) {
            throw new IOException("Failed to upload files, rolling back...", e);
        }

        return mapToResponse(videoPost);
    }

    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(VIDEO_DIR));
        Files.createDirectories(Paths.get(THUMBNAIL_DIR));
    }

    private void uploadFile(MultipartFile file, Path path) throws IOException {
        Files.copy(file.getInputStream(), path);
    }

    @Cacheable(value = "thumbnails", key = "#thumbnailPath")
    public byte[] getThumbnail(String thumbnailPath) throws IOException {
        return Files.readAllBytes(Paths.get(thumbnailPath));
    }

    public byte[] getVideo(String videoPath) throws IOException {
        Path path = Paths.get(videoPath);
        if (!Files.exists(path)) {
            throw new IOException("Video file not found: " + videoPath);
        }
        return Files.readAllBytes(path);
    }

    public java.util.List<VideoPostResponse> getAllVideos() {
        return videoPostRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public java.util.List<VideoPostResponse> getVideosForTiles(java.util.List<TileCoordinate> tiles) {
        if (tiles == null || tiles.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        int mapZoom = tiles.get(0).getZoom();

        boolean highZoom = mapZoom >= HIGH_ZOOM_THRESHOLD;
        boolean mediumZoom = mapZoom >= MEDIUM_ZOOM_THRESHOLD && mapZoom < HIGH_ZOOM_THRESHOLD;

        java.util.Set<Long> videoIds = new java.util.HashSet<>();
        java.util.List<VideoPostResponse> result = new java.util.ArrayList<>();

        for (TileCoordinate tile : tiles) {
            validateTile(tile);

            TileRange range = toBaseTileRange(tile);

            if (highZoom) {
                java.util.List<VideoPost> videosInRange =
                        videoPostRepository.findAllByTileZoomAndTileXBetweenAndTileYBetween(
                                BASE_TILE_ZOOM,
                                range.getXStart(), range.getXEnd(),
                                range.getYStart(), range.getYEnd()
                        );

                for (VideoPost video : videosInRange) {
                    if (videoIds.add(video.getId())) {
                        result.add(mapToResponse(video));
                    }
                }

            } else {
                int limit = mediumZoom ? MEDIUM_LEVEL_MAX_VIDEOS_PER_TILE : LOW_LEVEL_MAX_VIDEOS_PER_TILE;

                PageRequest pageRequest = PageRequest.of(
                        0,
                        limit,
                        Sort.by(Sort.Direction.DESC, "views") // ili "createdAt" ako želiš po datumu
                );

                Page<VideoPost> page =
                        videoPostRepository.findByTileZoomAndTileXBetweenAndTileYBetween(
                                BASE_TILE_ZOOM,
                                range.getXStart(), range.getXEnd(),
                                range.getYStart(), range.getYEnd(),
                                pageRequest
                        );

                for (VideoPost video : page.getContent()) {
                    if (videoIds.add(video.getId())) {
                        result.add(mapToResponse(video));
                    }
                }
            }
        }

        return result;
    }

    private void validateTile(TileCoordinate tile) {
        int max = 1 << tile.getZoom();
        
        if (tile.getZoom() < 0 || tile.getZoom() > 20) {
            throw new IllegalArgumentException("Invalid zoom");
        }
        if (tile.getX() < 0 || tile.getY() < 0 || tile.getX() >= max || tile.getY() >= max) {
            throw new IllegalArgumentException("Invalid tile coordinates");
        }
    }

    @Transactional
    public VideoPostResponse getVideoById(Long id) {
        VideoPost videoPost = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
        
        videoPostRepository.incrementViews(id);
        dailyVideoViewService.recordView(id); // Beleži dnevni pregled
        return mapToResponse(videoPost);
    }

    public StreamInfoResponse getStreamInfo(Long id) {
        VideoPost videoPost = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
        
        LocalDateTime now = LocalDateTime.now();
        StreamInfoResponse response = new StreamInfoResponse();
        response.setServerTime(now);
        
        if (videoPost.getScheduledReleaseTime() == null) {
            response.setScheduled(false);
            response.setHasStarted(true);
            response.setHasEnded(false);
            response.setCurrentOffsetSeconds(0L);
            response.setVideoDurationSeconds(videoPost.getVideoDurationSeconds());
            return response;
        }
        
        response.setScheduled(true);
        response.setScheduledReleaseTime(videoPost.getScheduledReleaseTime());
        response.setVideoDurationSeconds(videoPost.getVideoDurationSeconds());
        
        if (now.isBefore(videoPost.getScheduledReleaseTime())) {
            response.setHasStarted(false);
            response.setHasEnded(false);
            response.setCurrentOffsetSeconds(0L);
            return response;
        }
        
        long offsetSeconds = java.time.Duration.between(
            videoPost.getScheduledReleaseTime(), 
            now
        ).getSeconds();
        
        response.setHasStarted(true);
        response.setCurrentOffsetSeconds(offsetSeconds);
        
        if (videoPost.getVideoDurationSeconds() != null && 
            offsetSeconds >= videoPost.getVideoDurationSeconds()) {
            response.setHasEnded(true);
            response.setCurrentOffsetSeconds(videoPost.getVideoDurationSeconds());
        } else {
            response.setHasEnded(false);
        }
        
        return response;
    }

    @Transactional
    public LikeResponse toggleLike(Long videoId, User user) {

        if (user == null || "anonymousUser".equals(user.getUsername())) {
            throw new UnauthorizedActionException("You must be logged in to like video.");
        }

        VideoPost videoPost = videoPostRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        boolean liked;
        if (user.getLikedVideos().contains(videoPost)) {
            user.getLikedVideos().remove(videoPost);
            videoPost.setLikes(videoPost.getLikes() - 1);
            liked = false;
        } else {
            user.getLikedVideos().add(videoPost);
            videoPost.setLikes(videoPost.getLikes() + 1);
            liked = true;
        }
        videoPostRepository.save(videoPost);
        return new LikeResponse(liked, videoPost.getLikes());
    }

    private VideoPostResponse mapToResponse(VideoPost videoPost) {
        String email = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            email = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        
        final String finalEmail = email;
        boolean liked = false;
        if (finalEmail != null && !finalEmail.equals("anonymousUser")) {
            liked = videoPost.getLikedByUsers().stream()
                    .anyMatch(u -> u.getEmail().equals(finalEmail));
        }
        return mapToResponseLiked(videoPost, liked);
    }

    private VideoPostResponse mapToResponseLiked(VideoPost videoPost, boolean likedByCurrentUser) {
        return new VideoPostResponse(
                videoPost.getId(),
                videoPost.getTitle(),
                videoPost.getDescription(),
                videoPost.getTags(),
                videoPost.getVideoUrl(),
                videoPost.getThumbnailPath(),
                videoPost.getCreatedAt(),
                videoPost.getViews(),
                videoPost.getLikes(),
                videoPost.getLongitude(),
                videoPost.getLatitude(),
                videoPost.getUser().getActualUsername(),
                likedByCurrentUser,
                videoPost.getScheduledReleaseTime(),
                videoPost.getVideoDurationSeconds()
        );
    }

    private TileRange toBaseTileRange(TileCoordinate tile) {
        int effectiveZoom = Math.min(tile.getZoom(), BASE_TILE_ZOOM);
        int zoomDiff = BASE_TILE_ZOOM - effectiveZoom;
        int factor = 1 << zoomDiff; // 2^(zoomDiff)

        int baseXStart = tile.getX() * factor;
        int baseXEnd   = (tile.getX() + 1) * factor - 1;

        int baseYStart = tile.getY() * factor;
        int baseYEnd   = (tile.getY() + 1) * factor - 1;

        return new TileRange(baseXStart, baseXEnd, baseYStart, baseYEnd);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void recalculateTileData() {
        int pageSize = 100;
        int pageNumber = 0;
        Page<VideoPost> page;

        do {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            page = videoPostRepository.findAll(pageable);

            processVideoBatch(page.getContent());

            pageNumber++;
        } while (page.hasNext());
    }

    @Transactional
    protected void processVideoBatch(java.util.List<VideoPost> videos) {
        for (VideoPost video : videos) {
            if (video.getLatitude() != null && video.getLongitude() != null) {
                TileCoordinate tile = TileCalculator.getTileForLocation(
                        video.getLatitude(),
                        video.getLongitude(),
                        DEFAULT_TILE_ZOOM
                );
                video.setTileX(tile.getX());
                video.setTileY(tile.getY());
                video.setTileZoom(tile.getZoom());
            }
        }

        videoPostRepository.saveAll(videos);
    }
}
