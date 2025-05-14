package com.example.sdpbackend.repository;

import com.example.sdpbackend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;


@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find token by username and userType
     */
    Optional<PasswordResetToken> findByUsernameAndUserType(String username, String userType);

    /**
     * NEW METHOD - Find by username and verification code
     */
    Optional<PasswordResetToken> findByUsernameAndVerificationCode(String username, String verificationCode);

    /**
     * NEW METHOD - Find most recent token by username
     */
    Optional<PasswordResetToken> findFirstByUsernameOrderByIdDesc(String username);
}
