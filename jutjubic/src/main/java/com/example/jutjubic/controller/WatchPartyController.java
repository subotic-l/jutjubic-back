package com.example.jutjubic.controller;

import com.example.jutjubic.dto.CreateWatchPartyRequest;
import com.example.jutjubic.dto.WatchPartyDto;
import com.example.jutjubic.model.User;
import com.example.jutjubic.service.UserService;
import com.example.jutjubic.service.WatchPartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watch-party")
@RequiredArgsConstructor
public class WatchPartyController {

    private final WatchPartyService watchPartyService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<WatchPartyDto> createWatchParty(@RequestBody CreateWatchPartyRequest request) {
        User user = getCurrentUser();
        WatchPartyDto watchParty = watchPartyService.createWatchParty(request, user);
        return ResponseEntity.ok(watchParty);
    }

    @PostMapping("/{roomCode}/join")
    public ResponseEntity<WatchPartyDto> joinWatchParty(@PathVariable String roomCode) {
        User user = getCurrentUser();
        WatchPartyDto watchParty = watchPartyService.joinWatchParty(roomCode, user);
        return ResponseEntity.ok(watchParty);
    }

    @PostMapping("/{roomCode}/leave")
    public ResponseEntity<Void> leaveWatchParty(@PathVariable String roomCode) {
        User user = getCurrentUser();
        watchPartyService.leaveWatchParty(roomCode, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomCode}/start-video/{videoId}")
    public ResponseEntity<Void> startVideo(@PathVariable String roomCode, @PathVariable Long videoId) {
        User user = getCurrentUser();
        watchPartyService.startVideo(roomCode, videoId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomCode}/close")
    public ResponseEntity<Void> closeWatchParty(@PathVariable String roomCode) {
        User user = getCurrentUser();
        watchPartyService.closeWatchParty(roomCode, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<WatchPartyDto> getWatchParty(@PathVariable String roomCode) {
        WatchPartyDto watchParty = watchPartyService.getWatchParty(roomCode);
        return ResponseEntity.ok(watchParty);
    }

    @GetMapping
    public ResponseEntity<List<WatchPartyDto>> getActiveWatchParties() {
        List<WatchPartyDto> watchParties = watchPartyService.getActiveWatchParties();
        return ResponseEntity.ok(watchParties);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email);
    }
}