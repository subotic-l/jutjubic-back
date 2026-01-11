package com.example.jutjubic.repository;

import com.example.jutjubic.model.VideoComment;
import com.example.jutjubic.model.VideoPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoCommentRepository extends JpaRepository<VideoComment, Long> {

    Page<VideoComment> findByVideoPostOrderByCreatedAtDesc(VideoPost videoPost, Pageable pageable);
}
