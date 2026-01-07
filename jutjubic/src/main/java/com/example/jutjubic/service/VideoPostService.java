package com.example.jutjubic.service;

import com.example.jutjubic.dto.VideoPostRequest;
import com.example.jutjubic.dto.VideoPostResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.VideoPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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
            // But we should clean up any partially uploaded files if necessary
            // In a real scenario, we might want more complex logic here.
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

    private VideoPostResponse mapToResponse(VideoPost videoPost) {
        return new VideoPostResponse(
                videoPost.getId(),
                videoPost.getTitle(),
                videoPost.getDescription(),
                videoPost.getTags(),
                videoPost.getVideoUrl(),
                videoPost.getThumbnailPath(),
                videoPost.getCreatedAt(),
                videoPost.getLocation(),
                videoPost.getUser().getUsername()
        );
    }
}
