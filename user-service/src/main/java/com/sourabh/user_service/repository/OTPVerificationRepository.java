package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.OTPType;
import com.sourabh.user_service.entity.OTPVerification;
import com.sourabh.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OTPVerificationRepository extends JpaRepository<OTPVerification, Long> {

    Optional<OTPVerification> findTopByUserAndTypeOrderByCreatedAtDesc(User user, OTPType type);
}
