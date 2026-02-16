package com.example.jutjubic.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FFmpegService {

    private static final long TIMEOUT_MINUTES = 30;

    public boolean transcodeVideo(String inputPath, String outputPath, String resolution, String bitrate, String codec) {
        try {
            Path input = Paths.get(inputPath);
            if (!Files.exists(input)) {
                log.error("Input file does not exist: {}", inputPath);
                return false;
            }

            Path outputDir = Paths.get(outputPath).getParent();
            if (outputDir != null && !Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            String[] command = {
                    "ffmpeg",
                    "-i", inputPath,
                    "-vf", "scale=" + resolution,
                    "-c:v", codec,
                    "-b:v", bitrate,
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-y",
                    outputPath
            };

            log.info("Starting FFmpeg transcoding: {} -> {}", inputPath, outputPath);
            log.info("FFmpeg command: {}", String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                }
            }

            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);

            if (!finished) {
                log.error("FFmpeg process timed out after {} minutes", TIMEOUT_MINUTES);
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("Transcoding completed successfully: {}", outputPath);
                return true;
            } else {
                log.error("FFmpeg exited with code: {}", exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("Error during transcoding: {}", e.getMessage(), e);
            return false;
        }
    }
}