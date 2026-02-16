package com.example.uploadeventconsumer.service;

import com.example.proto.UploadEventProto;
import com.example.uploadeventconsumer.dto.UploadEventJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Izolovani benchmark servis koji meri performanse serijalizacije i deserijalizacije
 * JSON vs Protobuf formata BEZ I/O overhead-a (RabbitMQ, mreža, disk).
 * 
 * Koristi iteracioni sistem: za svaku od 50 poruka izvršava se 100 serijalizacija
 * i 100 deserijalizacija kako bi se dobile stabilne i precizne metrike.
 */
@Service
public class IsolatedBenchmarkService {

    private static final Logger logger = LoggerFactory.getLogger(IsolatedBenchmarkService.class);
    
    private final ObjectMapper objectMapper;
    
    // Konstante za testiranje
    private static final int NUM_MESSAGES = 50;
    private static final int ITERATIONS_PER_MESSAGE = 100;
    private static final int WARMUP_ITERATIONS = 20;
    
    public IsolatedBenchmarkService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Pokreće izolovano benchmark testiranje.
     * Vraća DetailedBenchmarkResult sa metrikama za serijalizaciju i deserijalizaciju.
     */
    public DetailedBenchmarkResult runIsolatedBenchmark() {
        logger.info("Starting isolated benchmark test with {} messages and {} iterations per message", 
            NUM_MESSAGES, ITERATIONS_PER_MESSAGE);
        
        // Generiši 50 različitih poruka
        List<UploadEventJson> messages = generateDiverseMessages();
        
        // JIT Warmup - izvrši nekoliko iteracija pre merenja kako bi se JVM optimizovao
        performWarmup(messages);
        
        // Reci za garbage collector da oslobodi memoriju pre merenja
        System.gc();
        
        try {
            Thread.sleep(100); // Kratka pauza nakon GC
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Izvršavanje merenja
        DetailedBenchmarkResult result = performBenchmark(messages);
        
        logger.info("Benchmark completed. JSON avg serialize: {} ns, deserialize: {} ns | Protobuf avg serialize: {} ns, deserialize: {} ns",
            result.jsonSerializeNs, result.jsonDeserializeNs, result.protobufSerializeNs, result.protobufDeserializeNs);
        
        return result;
    }
    
    /**
     * JIT Warmup - pokreće nekoliko iteracija pre merenja kako bi HotSpot JIT
     * kompajler optimizovao kod i eliminisao uticaj cold start-a.
     */
    private void performWarmup(List<UploadEventJson> messages) {
        logger.info("Performing JIT warmup with {} iterations...", WARMUP_ITERATIONS);
        
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            UploadEventJson msg = messages.get(i % messages.size());
            
            try {
                // JSON warmup
                byte[] jsonBytes = objectMapper.writeValueAsBytes(msg);
                objectMapper.readValue(jsonBytes, UploadEventJson.class);
                
                // Protobuf warmup
                UploadEventProto.UploadEvent proto = convertToProtobuf(msg);
                byte[] protoBytes = proto.toByteArray();
                UploadEventProto.UploadEvent.parseFrom(protoBytes);
            } catch (Exception e) {
                logger.warn("Warmup iteration {} failed: {}", i, e.getMessage());
            }
        }
        
        logger.info("JIT warmup completed");
    }
    
    /**
     * Izvršava benchmark testiranje na svim porukama.
     * Za svaku poruku izvršava ITERATIONS_PER_MESSAGE serijalizacija i deserijalizacija.
     */
    private DetailedBenchmarkResult performBenchmark(List<UploadEventJson> messages) {
        // Akumulatori za vreme (long da bi se izbegao overflow)
        long totalJsonSerializeNs = 0;
        long totalJsonDeserializeNs = 0;
        long totalProtobufSerializeNs = 0;
        long totalProtobufDeserializeNs = 0;
        
        // Akumulatori za veličinu poruka
        long totalJsonSize = 0;
        long totalProtobufSize = 0;
        
        int totalIterations = messages.size() * ITERATIONS_PER_MESSAGE;
        
        for (int msgIndex = 0; msgIndex < messages.size(); msgIndex++) {
            UploadEventJson message = messages.get(msgIndex);
            UploadEventProto.UploadEvent protoMessage = convertToProtobuf(message);
            
            // Za svaku poruku izvršiti ITERATIONS_PER_MESSAGE serijalizacija/deserijalizacija
            for (int iter = 0; iter < ITERATIONS_PER_MESSAGE; iter++) {
                try {
                    // === JSON SERIJALIZACIJA ===
                    long startTime = System.nanoTime();
                    byte[] jsonBytes = objectMapper.writeValueAsBytes(message);
                    long endTime = System.nanoTime();
                    totalJsonSerializeNs += (endTime - startTime);
                    
                    if (iter == 0) { // Veličinu beležimo samo jednom po poruci
                        totalJsonSize += jsonBytes.length;
                    }
                    
                    // === JSON DESERIJALIZACIJA ===
                    startTime = System.nanoTime();
                    objectMapper.readValue(jsonBytes, UploadEventJson.class);
                    endTime = System.nanoTime();
                    totalJsonDeserializeNs += (endTime - startTime);
                    
                    // === PROTOBUF SERIJALIZACIJA ===
                    startTime = System.nanoTime();
                    byte[] protoBytes = protoMessage.toByteArray();
                    endTime = System.nanoTime();
                    totalProtobufSerializeNs += (endTime - startTime);
                    
                    if (iter == 0) { // Veličinu beležimo samo jednom po poruci
                        totalProtobufSize += protoBytes.length;
                    }
                    
                    // === PROTOBUF DESERIJALIZACIJA ===
                    startTime = System.nanoTime();
                    UploadEventProto.UploadEvent.parseFrom(protoBytes);
                    endTime = System.nanoTime();
                    totalProtobufDeserializeNs += (endTime - startTime);
                    
                } catch (Exception e) {
                    logger.error("Error during benchmark iteration {} for message {}", iter, msgIndex, e);
                }
            }
            
            if ((msgIndex + 1) % 10 == 0) {
                logger.debug("Processed {}/{} messages", msgIndex + 1, messages.size());
            }
        }
        
        // Izračunaj proseke
        long avgJsonSerializeNs = totalJsonSerializeNs / totalIterations;
        long avgJsonDeserializeNs = totalJsonDeserializeNs / totalIterations;
        long avgProtobufSerializeNs = totalProtobufSerializeNs / totalIterations;
        long avgProtobufDeserializeNs = totalProtobufDeserializeNs / totalIterations;
        
        double avgJsonSizeBytes = (double) totalJsonSize / messages.size();
        double avgProtobufSizeBytes = (double) totalProtobufSize / messages.size();
        
        return new DetailedBenchmarkResult(
            avgJsonSerializeNs,
            avgJsonDeserializeNs,
            avgJsonSizeBytes,
            avgProtobufSerializeNs,
            avgProtobufDeserializeNs,
            avgProtobufSizeBytes
        );
    }
    
    /**
     * Generiše 50 različitih UploadEvent poruka sa različitim podacima.
     */
    private List<UploadEventJson> generateDiverseMessages() {
        List<UploadEventJson> messages = new ArrayList<>();
        Random random = new Random(42); // Fixed seed za reproducibilnost
        
        String[] authors = {"user1", "john_doe", "video_creator", "content_master", "streamer_pro"};
        String[] baseDescriptions = {
            "Short description",
            "This is a medium length description with some additional details about the video content",
            "A very detailed and comprehensive description that includes multiple sentences. It describes the video in great detail, including what viewers can expect to see, learn, and experience. This type of description is common for educational or tutorial videos.",
            "Minimal desc",
            "Epic gaming montage featuring the best moments from last week's streams and competitions"
        };
        
        for (int i = 1; i <= NUM_MESSAGES; i++) {
            Long videoId = (long) i;
            String title = "Video " + i;
            String username = authors[random.nextInt(authors.length)] + "_" + i;
            
            // Variranje dužine description-a (5 do 150 karaktera)
            String baseDesc = baseDescriptions[random.nextInt(baseDescriptions.length)];
            String description = baseDesc.substring(0, Math.min(baseDesc.length(), 5 + random.nextInt(145)));
            
            String videoUrl = "https://example.com/videos/video_" + i + ".mp4";
            LocalDateTime uploadedAt = LocalDateTime.now().minusDays(random.nextInt(30));
            
            // Opciono: dodaj geografske koordinate (50% šanse)
            Double latitude = random.nextBoolean() ? 40.0 + random.nextDouble() * 10 : null;
            Double longitude = random.nextBoolean() ? -74.0 + random.nextDouble() * 10 : null;
            
            UploadEventJson event = new UploadEventJson(
                videoId, 
                title + " - " + description, // Kombinuj za varijabilnost
                username, 
                videoUrl, 
                uploadedAt, 
                latitude, 
                longitude
            );
            
            messages.add(event);
        }
        
        logger.info("Generated {} diverse messages", messages.size());
        return messages;
    }
    
    /**
     * Konvertuje UploadEventJson u Protobuf format.
     */
    private UploadEventProto.UploadEvent convertToProtobuf(UploadEventJson event) {
        UploadEventProto.UploadEvent.Builder builder = UploadEventProto.UploadEvent.newBuilder()
            .setVideoId(event.getVideoId())
            .setTitle(event.getTitle())
            .setUsername(event.getUsername())
            .setVideoUrl(event.getVideoUrl())
            .setUploadedAt(event.getUploadedAt().toEpochSecond(java.time.ZoneOffset.UTC));
        
        if (event.getLatitude() != null) {
            builder.setLatitude(event.getLatitude());
        }
        if (event.getLongitude() != null) {
            builder.setLongitude(event.getLongitude());
        }
        
        return builder.build();
    }
    
    /**
     * Rezultat detaljnog benchmark testa sa metrikama za serijalizaciju i deserijalizaciju.
     */
    public static class DetailedBenchmarkResult {
        public final long jsonSerializeNs;
        public final long jsonDeserializeNs;
        public final double jsonSizeBytes;
        
        public final long protobufSerializeNs;
        public final long protobufDeserializeNs;
        public final double protobufSizeBytes;
        
        public DetailedBenchmarkResult(
            long jsonSerializeNs, long jsonDeserializeNs, double jsonSizeBytes,
            long protobufSerializeNs, long protobufDeserializeNs, double protobufSizeBytes
        ) {
            this.jsonSerializeNs = jsonSerializeNs;
            this.jsonDeserializeNs = jsonDeserializeNs;
            this.jsonSizeBytes = jsonSizeBytes;
            this.protobufSerializeNs = protobufSerializeNs;
            this.protobufDeserializeNs = protobufDeserializeNs;
            this.protobufSizeBytes = protobufSizeBytes;
        }
    }
}
