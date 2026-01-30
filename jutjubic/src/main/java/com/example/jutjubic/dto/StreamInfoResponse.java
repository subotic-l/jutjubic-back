package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StreamInfoResponse {
    private boolean isScheduled;
    private boolean hasStarted;
    private boolean hasEnded;
    private LocalDateTime scheduledReleaseTime;
    private Long videoDurationSeconds;
    private Long currentOffsetSeconds;
    private LocalDateTime serverTime;
}
