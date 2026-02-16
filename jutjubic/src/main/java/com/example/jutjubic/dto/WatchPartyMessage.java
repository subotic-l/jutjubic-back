package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WatchPartyMessage {
    private String type;        // "VIDEO_STARTED", "USER_JOINED", "USER_LEFT", "PARTY_CLOSED"
    private String roomCode;
    private Long videoId;
    private String videoTitle;
    private String username;
    private String message;
}