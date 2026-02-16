package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranscodingMessage {

    private Long videoId;
    private String originalVideoPath;
    private String outputVideoPath;
    private String resolution;
    private String bitrate;
    private String codec;

    public String toMessageString() {
        return String.join("|",
                String.valueOf(videoId),
                originalVideoPath,
                outputVideoPath,
                resolution,
                bitrate,
                codec
        );
    }

    public static TranscodingMessage fromMessageString(String message) {
        String[] parts = message.split("\\|");
        return new TranscodingMessage(
                Long.parseLong(parts[0]),
                parts[1],
                parts[2],
                parts[3],
                parts[4],
                parts[5]
        );
    }
}