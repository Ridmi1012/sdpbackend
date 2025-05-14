package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;


@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    /**
     * NEW METHOD - Find token by token string
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * NEW METHOD - Find token by username and userType
     */
    Optional<PasswordResetToken> findByUsernameAndUserType(String username, String userType);
}
