package com.example.jutjubic.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class ImageCompressionService {

    private static final double COMPRESSION_QUALITY = 0.7; // 70% quality
    private static final String COMPRESSED_SUFFIX = "_compressed.jpg";

    /**
     * Kompresuje sliku i čuva kompresovanu verziju.
     *
     * @param originalPath Putanja do originalne slike
     * @return Putanja do kompresovane slike, ili null ako kompresija ne uspe
     */
    public String compressImage(String originalPath) {
        try {
            // Proveri da li je putanja null ili prazna
            if (originalPath == null || originalPath.isEmpty()) {
                log.warn("Original path is null or empty, skipping compression");
                return null;
            }

            Path originalFilePath = Paths.get(originalPath);

            // Proveri da li original fajl postoji
            if (!Files.exists(originalFilePath)) {
                log.warn("Original image does not exist, skipping: {}", originalPath);
                return null;
            }

            // Kreiraj putanju za kompresovanu sliku
            String compressedPath = originalPath.replaceFirst("(\\.[^.]+)$", COMPRESSED_SUFFIX);

            // Ako kompresovana verzija već postoji, preskoči
            if (Files.exists(Paths.get(compressedPath))) {
                log.info("Compressed image already exists: {}", compressedPath);
                return compressedPath;
            }

            // Kompresuj sliku
            Thumbnails.of(new File(originalPath))
                    .scale(1.0) // Zadrži originalnu rezoluciju
                    .outputQuality(COMPRESSION_QUALITY)
                    .outputFormat("jpg")
                    .toFile(new File(compressedPath));

            log.info("Successfully compressed image: {} -> {}", originalPath, compressedPath);

            // Logiraj veličine fajlova
            long originalSize = Files.size(originalFilePath);
            long compressedSize = Files.size(Paths.get(compressedPath));
            double savingsPercent = ((originalSize - compressedSize) * 100.0) / originalSize;

            log.info("Compression savings: {}% (Original: {} bytes, Compressed: {} bytes)",
                    String.format("%.2f", savingsPercent), originalSize, compressedSize);

            return compressedPath;

        } catch (IOException e) {
            log.error("Error compressing image {}: {}", originalPath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Provera da li je slika već kompresovana.
     */
    public boolean isAlreadyCompressed(String thumbnailPath) {
        if (thumbnailPath == null) {
            return false;
        }
        String compressedPath = thumbnailPath.replaceFirst("(\\.[^.]+)$", COMPRESSED_SUFFIX);
        return Files.exists(Paths.get(compressedPath));
    }
}