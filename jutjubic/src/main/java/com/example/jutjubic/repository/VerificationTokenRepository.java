package com.example.jutjubic.repository;

import com.example.jutjubic.model.User;
import com.example.jutjubic.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    
    Optional<VerificationToken> findByToken(String token);
    
    Optional<VerificationToken> findByUser(User user);
    
    @Modifying
    void deleteByUser(User user);
}
