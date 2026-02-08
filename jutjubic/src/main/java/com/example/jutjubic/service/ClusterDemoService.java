package com.example.jutjubic.service;

import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.VideoPostRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Servis za demonstraciju rada u klasteru sa resilience mehanizmima.
 * 
 * KLASTER FUNKCIONALNOST:
 * ========================
 * 1. Stateless dizajn - nema session state, sve u bazi
 * 2. Circuit breaker - štiti od kaskadnih padova
 * 3. Retry - pokušava ponovo kod prolaznih grešaka
 * 4. Timeout - sprečava dugotrajne operacije
 * 5. Graceful degradation - nastavlja bez MQ ako nije dostupan
 * 
 * PAD REPLIKE:
 * ============
 * - Load balancer detektuje pad kroz health check endpoint (/actuator/health)
 * - Zahtevi se rutiraju na preostale zdrave replike
 * - Kad replika oživi, automatski se vraća u pool
 * 
 * DETEKCIJA PADA:
 * ===============
 * - Health check interval: 10s (konfigurisano u nginx)
 * - Ako replika ne odgovori na 2 uzastopna health check-a, označava se kao DOWN
 * - Nginx automatski izbacuje DOWN replike iz load balancing pool-a
 */
@Service
public class ClusterDemoService {

    private static final Logger log = LoggerFactory.getLogger(ClusterDemoService.class);
    private static final String VIDEO_EVENTS_QUEUE = "video.events";

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Autowired(required = false) // Optional - može da radi bez RabbitMQ
    private RabbitTemplate rabbitTemplate;

    /**
     * Dohvata video sa circuit breaker zaštitom.
     * 
     * @CircuitBreaker: Ako baza ima previše grešaka, circuit se otvara i koristi fallback
     * @Retry: Pokušava 3 puta sa exponential backoff
     * @Transactional(readOnly): Optimizacija za read operacije
     */
    @CircuitBreaker(name = "database", fallbackMethod = "getVideoFallback")
    @Retry(name = "database")
    @Transactional(readOnly = true)
    public VideoPost getVideo(Long id) {
        log.info("Fetching video {} from database", id);
        
        Optional<VideoPost> video = videoPostRepository.findById(id);
        
        if (video.isEmpty()) {
            log.warn("Video {} not found", id);
            throw new RuntimeException("Video not found: " + id);
        }
        
        log.info("Successfully fetched video: {}", video.get().getTitle());
        return video.get();
    }

    /**
     * Fallback metoda kada baza nije dostupna.
     * Ovo je primer graceful degradation - vraća placeholder umesto da aplikacija padne.
     */
    private VideoPost getVideoFallback(Long id, Exception e) {
        log.error("Database unavailable, using fallback for video {}: {}", id, e.getMessage());
        
        // Vraćamo placeholder objekat kada baza nije dostupna
        VideoPost placeholder = new VideoPost();
        placeholder.setId(id);
        placeholder.setTitle("Service Temporarily Unavailable");
        placeholder.setDescription("Database connection error. Please try again later.");
        
        return placeholder;
    }

    /**
     * Lista svih videa sa circuit breaker zaštitom.
     */
    @CircuitBreaker(name = "database", fallbackMethod = "getAllVideosFallback")
    @Retry(name = "database")
    @Transactional(readOnly = true)
    public List<VideoPost> getAllVideos() {
        log.info("Fetching all videos from database");
        return videoPostRepository.findAll();
    }

    private List<VideoPost> getAllVideosFallback(Exception e) {
        log.error("Database unavailable, returning empty list: {}", e.getMessage());
        return List.of();
    }

    /**
     * Kreira video i opciono šalje event u message queue.
     * 
     * Demonstracija:
     * - DB operacija sa circuit breaker
     * - MQ operacija sa graceful degradation (ne pada ako MQ nije dostupan)
     */
    @CircuitBreaker(name = "database", fallbackMethod = "createVideoFallback")
    @Retry(name = "database")
    @Transactional
    public VideoPost createVideo(VideoPost video) {
        log.info("Creating video: {}", video.getTitle());
        
        // Čuva u bazu
        VideoPost savedVideo = videoPostRepository.save(video);
        log.info("Video saved to database with ID: {}", savedVideo.getId());
        
        // Pokušava da pošalje event u message queue (opciono - graceful degradation)
        sendVideoCreatedEvent(savedVideo);
        
        return savedVideo;
    }

    private VideoPost createVideoFallback(VideoPost video, Exception e) {
        log.error("Failed to create video: {}", e.getMessage());
        throw new RuntimeException("Service temporarily unavailable. Please try again later.", e);
    }

    /**
     * Šalje event u message queue sa graceful degradation.
     * 
     * VAŽNO: Ako RabbitMQ nije dostupan, aplikacija NE pada.
     * Circuit breaker štiti od kaskadnih grešaka.
     */
    @CircuitBreaker(name = "messageQueue", fallbackMethod = "sendEventFallback")
    @TimeLimiter(name = "messageQueue")
    public CompletableFuture<Void> sendVideoCreatedEvent(VideoPost video) {
        return CompletableFuture.runAsync(() -> {
            if (rabbitTemplate != null) {
                try {
                    Map<String, Object> event = new HashMap<>();
                    event.put("eventType", "VIDEO_CREATED");
                    event.put("videoId", video.getId());
                    event.put("title", video.getTitle());
                    event.put("timestamp", LocalDateTime.now().toString());
                    
                    rabbitTemplate.convertAndSend(VIDEO_EVENTS_QUEUE, event);
                    log.info("Event sent to message queue for video: {}", video.getId());
                } catch (Exception e) {
                    log.warn("Failed to send event to message queue: {}", e.getMessage());
                    throw e;
                }
            } else {
                log.warn("RabbitTemplate not available - message queue disabled");
            }
        });
    }

    /**
     * Fallback kada message queue nije dostupan.
     * Aplikacija nastavlja da radi - to je graceful degradation.
     */
    private CompletableFuture<Void> sendEventFallback(VideoPost video, Exception e) {
        log.warn("Message queue unavailable for video {}: {}. Application continues without messaging.", 
                 video.getId(), e.getMessage());
        return CompletableFuture.completedFuture(null);
    }
}
