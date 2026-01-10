package com.example.jutjubic;

import com.example.jutjubic.model.User;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.repository.VideoPostRepository;
import com.example.jutjubic.service.VideoPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class VideoPostConcurrencyTest {

    @Autowired
    private VideoPostService videoPostService;

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    public void testConcurrentViewIncrements() throws InterruptedException {
        // Setup
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        final Long[] videoIdContainer = new Long[1];

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                User user = new User();
                user.setEmail("test_" + System.currentTimeMillis() + "@example.com");
                user.setUsername("testuser_" + System.currentTimeMillis());
                user.setPassword("password");
                user.setFirstName("Test");
                user.setLastName("User");
                user.setAddress("Test Address");
                user.setEnabled(true);
                user = userRepository.save(user);

                VideoPost videoPost = new VideoPost();
                videoPost.setTitle("Test Video");
                videoPost.setVideoUrl("http://example.com/video.mp4");
                videoPost.setThumbnailPath("http://example.com/thumb.jpg");
                videoPost.setCreatedAt(LocalDateTime.now());
                videoPost.setUser(user);
                videoPost.setViews(0L);
                videoPost = videoPostRepository.save(videoPost);
                videoIdContainer[0] = videoPost.getId();
            }
        });

        Long videoId = videoIdContainer[0];
        int numberOfThreads = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.await(); // Wait for the signal to start
                    videoPostService.getVideoById(videoId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads at once
        latch.countDown();
        
        // Wait for all threads to finish
        doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Verify
        VideoPost updatedVideo = videoPostRepository.findById(videoId).orElseThrow();
        assertEquals(Long.valueOf(numberOfThreads), updatedVideo.getViews(), "Views should be exactly " + numberOfThreads);
    }
}
