package com.example.jutjubic.service;

import com.example.jutjubic.dto.LikeResponse;
import com.example.jutjubic.dto.VideoPostRequest;
import com.example.jutjubic.dto.VideoPostResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.VideoPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
        videoPost.setLocation(request.getLocation());
        videoPost.setCreatedAt(LocalDateTime.now());
        videoPost.setUser(user);
        videoPost.setVideoUrl(videoPath.toString());
        videoPost.setThumbnailPath(thumbnailPath.toString());

        // Save to DB first within transaction
        videoPost = videoPostRepository.save(videoPost);

        // Upload files
        try {
            uploadFile(request.getVideo(), videoPath);
            uploadFile(request.getThumbnail(), thumbnailPath);
        } catch (IOException e) {
            // Rolling back DB is automatic because of @Transactional
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
        return videoPostRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
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
                videoPost.getLocation(),
                videoPost.getUser().getActualUsername(),
                likedByCurrentUser
        );
    }
}
