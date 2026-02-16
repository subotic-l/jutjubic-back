package com.example.jutjubic.service;

import com.example.jutjubic.dto.CreateWatchPartyRequest;
import com.example.jutjubic.dto.WatchPartyDto;
import com.example.jutjubic.dto.WatchPartyMessage;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.model.WatchParty;
import com.example.jutjubic.repository.VideoPostRepository;
import com.example.jutjubic.repository.WatchPartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchPartyService {

    private final WatchPartyRepository watchPartyRepository;
    private final VideoPostRepository videoPostRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public WatchPartyDto createWatchParty(CreateWatchPartyRequest request, User owner) {
        WatchParty watchParty = new WatchParty();
        watchParty.setName(request.getName());
        watchParty.setOwner(owner);
        watchParty.getParticipants().add(owner);

        watchParty = watchPartyRepository.save(watchParty);
        log.info("Watch party created: {} by user {}", watchParty.getRoomCode(), owner.getActualUsername());

        return mapToDto(watchParty);
    }

    @Transactional
    public WatchPartyDto joinWatchParty(String roomCode, User user) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));

        if (!watchParty.isActive()) {
            throw new RuntimeException("Watch party is no longer active");
        }

        watchParty.getParticipants().add(user);
        watchPartyRepository.save(watchParty);

        log.info("User {} joined watch party {}", user.getActualUsername(), roomCode);

        WatchPartyMessage message = new WatchPartyMessage(
                "USER_JOINED",
                roomCode,
                null,
                null,
                user.getActualUsername(),
                user.getActualUsername() + " joined the party!"
        );
        messagingTemplate.convertAndSend("/topic/watchparty/" + roomCode, message);

        return mapToDto(watchParty);
    }

    @Transactional
    public void leaveWatchParty(String roomCode, User user) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));

        watchParty.getParticipants().remove(user);
        watchPartyRepository.save(watchParty);

        log.info("User {} left watch party {}", user.getActualUsername(), roomCode);

        WatchPartyMessage message = new WatchPartyMessage(
                "USER_LEFT",
                roomCode,
                null,
                null,
                user.getActualUsername(),
                user.getActualUsername() + " left the party"
        );
        messagingTemplate.convertAndSend("/topic/watchparty/" + roomCode, message);
    }

    @Transactional
    public void startVideo(String roomCode, Long videoId, User user) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));

        if (!watchParty.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Only the owner can start videos");
        }

        VideoPost video = videoPostRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        watchParty.setCurrentVideo(video);
        watchPartyRepository.save(watchParty);

        log.info("Video {} started in watch party {} by owner {}", videoId, roomCode, user.getActualUsername());

        WatchPartyMessage message = new WatchPartyMessage(
                "VIDEO_STARTED",
                roomCode,
                videoId,
                video.getTitle(),
                user.getActualUsername(),
                "Video started: " + video.getTitle()
        );
        messagingTemplate.convertAndSend("/topic/watchparty/" + roomCode, message);
    }

    @Transactional
    public void closeWatchParty(String roomCode, User user) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));

        if (!watchParty.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Only the owner can close the watch party");
        }

        watchParty.setActive(false);
        watchPartyRepository.save(watchParty);

        log.info("Watch party {} closed by owner {}", roomCode, user.getActualUsername());

        WatchPartyMessage message = new WatchPartyMessage(
                "PARTY_CLOSED",
                roomCode,
                null,
                null,
                user.getActualUsername(),
                "Watch party has been closed"
        );
        messagingTemplate.convertAndSend("/topic/watchparty/" + roomCode, message);
    }

    public WatchPartyDto getWatchParty(String roomCode) {
        WatchParty watchParty = watchPartyRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));
        return mapToDto(watchParty);
    }

    public List<WatchPartyDto> getActiveWatchParties() {
        return watchPartyRepository.findByActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private WatchPartyDto mapToDto(WatchParty watchParty) {
        return new WatchPartyDto(
                watchParty.getId(),
                watchParty.getRoomCode(),
                watchParty.getName(),
                watchParty.getOwner().getActualUsername(),
                watchParty.getParticipants().stream()
                        .map(User::getActualUsername)
                        .collect(Collectors.toSet()),
                watchParty.getCurrentVideo() != null ? watchParty.getCurrentVideo().getId() : null,
                watchParty.getCurrentVideo() != null ? watchParty.getCurrentVideo().getTitle() : null,
                watchParty.isActive(),
                watchParty.getCreatedAt()
        );
    }
}