package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WatchPartyDto {
    private Long id;
    private String roomCode;
    private String name;
    private String ownerUsername;
    private Set<String> participantUsernames;
    private Long currentVideoId;
    private String currentVideoTitle;
    private boolean active;
    private LocalDateTime createdAt;
}