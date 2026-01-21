package com.example.jutjubic.service;

import com.example.jutjubic.dto.LikeResponse;
import com.example.jutjubic.dto.TileCoordinate;
import com.example.jutjubic.dto.VideoPostRequest;
import com.example.jutjubic.dto.VideoPostResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.VideoPostRepository;
import com.example.jutjubic.util.TileCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.jutjubic.exception.UnauthorizedActionException;

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
    private static final String UPLOAD_DIR = "uploads";
    private static final String VIDEO_DIR = UPLOAD_DIR + "/videos";
    private static final String THUMBNAIL_DIR = UPLOAD_DIR + "/thumbnails";
    private static final int DEFAULT_TILE_ZOOM = 12;

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
        
        java.util.Set<Long> videoIds = new java.util.HashSet<>();
        java.util.List<VideoPostResponse> result = new java.util.ArrayList<>();
        
        for (TileCoordinate tile : tiles) {
            validateTile(tile);
            
            java.util.List<VideoPost> videosInTile = videoPostRepository.findByTileCoordinates(
                tile.getX(), tile.getY(), tile.getZoom()
            );
            
            for (VideoPost video : videosInTile) {
                if (!videoIds.contains(video.getId())) {
                    videoIds.add(video.getId());
                    result.add(mapToResponse(video));
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
        videoPostRepository.incrementViews(id);
        VideoPost videoPost = videoPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
        
        return mapToResponse(videoPost);
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
                likedByCurrentUser
        );
    }
}
