package com.example.jutjubic.controller;

import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.service.ClusterDemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller za demonstraciju rada aplikacije u klasteru.
 * 
 * TESTIRANJE KLASTER FUNKCIONALNOSTI:
 * ====================================
 * 
 * 1. Normalan rad sa svim replikama:
 *    GET http://localhost/api/cluster/videos
 *    - Load balancer distribuira zahteve između replika (round-robin)
 *    - Svaki zahtev može biti opslužen od različite replike
 * 
 * 2. Pad jedne replike:
 *    - Ugasite jednu repliku: docker stop jutjubic-app-1
 *    - Zahtevi se automatski rutiraju na preostalu repliku
 *    - GET http://localhost/api/cluster/videos nastavlja da radi
 * 
 * 3. Ponovno podizanje replika:
 *    - Podignite repliku: docker start jutjubic-app-1
 *    - Health check detektuje da je replika živa
 *    - Load balancer automatski vraća repliku u pool (za ~20-30s)
 * 
 * 4. MQ nije dostupan:
 *    - Ugasite RabbitMQ: docker stop rabbitmq
 *    - POST http://localhost/api/cluster/videos i dalje radi
 *    - Video se čuva u bazi, ali event se ne šalje (graceful degradation)
 *    - Health check pokazuje MQ kao DOWN, ali aplikacija radi
 * 
 * 5. DB privremeno nedostupna:
 *    - Zaustavite Postgres privremeno
 *    - Circuit breaker aktivira fallback
 *    - Vraća placeholder podatke umesto greške
 */
@RestController
@RequestMapping("/api/cluster")
public class ClusterDemoController {

    private static final Logger log = LoggerFactory.getLogger(ClusterDemoController.class);

    @Autowired
    private ClusterDemoService clusterDemoService;

    @Value("${INSTANCE_ID:unknown}")
    private String instanceId;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Info endpoint - pokazuje koja replika obrađuje zahtev.
     * Korisno za demonstraciju load balancing-a.
     * 
     * Test: curl http://localhost/api/cluster/info
     * Pozovite nekoliko puta - videćete različite instance_id i port
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getInstanceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("instance_id", instanceId);
        info.put("port", serverPort);
        info.put("status", "healthy");
        info.put("message", "This request was handled by instance " + instanceId);
        
        log.info("Instance info requested - served by instance: {}", instanceId);
        return ResponseEntity.ok(info);
    }

    /**
     * Dohvata sve video postove.
     * 
     * Circuit breaker štiti od pada baze.
     * Retry mehanizam pokušava nekoliko puta kod prolaznih grešaka.
     * 
     * Test: 
     * curl http://localhost/api/cluster/videos
     * 
     * Kada DB padne:
     * - Vraća prazan list (fallback)
     * - Aplikacija ne pada
     */
    @GetMapping("/videos")
    public ResponseEntity<Map<String, Object>> getAllVideos() {
        log.info("GET /api/cluster/videos - Instance: {}", instanceId);
        
        try {
            List<VideoPost> videos = clusterDemoService.getAllVideos();
            
            Map<String, Object> response = new HashMap<>();
            response.put("instance_id", instanceId);
            response.put("videos", videos);
            response.put("count", videos.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching videos: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Service temporarily unavailable");
            errorResponse.put("instance_id", instanceId);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Dohvata pojedinačan video.
     * 
     * Test:
     * curl http://localhost/api/cluster/videos/1
     */
    @GetMapping("/videos/{id}")
    public ResponseEntity<Map<String, Object>> getVideo(@PathVariable Long id) {
        log.info("GET /api/cluster/videos/{} - Instance: {}", id, instanceId);
        
        try {
            VideoPost video = clusterDemoService.getVideo(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("instance_id", instanceId);
            response.put("video", video);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching video {}: {}", id, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Video not found or service unavailable");
            errorResponse.put("instance_id", instanceId);
            errorResponse.put("video_id", id);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Kreira novi video.
     * 
     * Demonstrira:
     * - Pisanje u bazu sa circuit breaker zaštitom
     * - Opciono slanje event-a u message queue
     * - Graceful degradation ako MQ nije dostupan
     * 
     * Test:
     * curl -X POST http://localhost/api/cluster/videos \
     *   -H "Content-Type: application/json" \
     *   -d '{"title":"Test Video","description":"Created for cluster testing"}'
     * 
     * Pozovite nekoliko puta - različite replike će obraditi zahteve
     * Svi videi se čuvaju u istu bazu i vidljivi su svim replikama
     */
    @PostMapping("/videos")
    public ResponseEntity<Map<String, Object>> createVideo(@RequestBody VideoPost video) {
        log.info("POST /api/cluster/videos - Instance: {} - Video: {}", instanceId, video.getTitle());
        
        try {
            VideoPost createdVideo = clusterDemoService.createVideo(video);
            
            Map<String, Object> response = new HashMap<>();
            response.put("instance_id", instanceId);
            response.put("video", createdVideo);
            response.put("message", "Video created successfully");
            response.put("note", "Event sent to message queue (if available)");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating video: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create video");
            errorResponse.put("instance_id", instanceId);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Test endpoint za simulaciju greške (za testiranje resilience).
     * 
     * Test:
     * curl http://localhost/api/cluster/test-error
     */
    @GetMapping("/test-error")
    public ResponseEntity<Map<String, String>> testError() {
        log.warn("Test error endpoint called on instance: {}", instanceId);
        throw new RuntimeException("Simulated error for testing resilience patterns");
    }
}
