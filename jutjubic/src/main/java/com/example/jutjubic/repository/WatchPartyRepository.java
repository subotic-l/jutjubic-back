package com.example.jutjubic.repository;

import com.example.jutjubic.model.WatchParty;
import com.example.jutjubic.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchPartyRepository extends JpaRepository<WatchParty, Long> {

    Optional<WatchParty> findByRoomCode(String roomCode);

    List<WatchParty> findByActiveTrue();

    List<WatchParty> findByOwner(User owner);

    boolean existsByRoomCode(String roomCode);
}