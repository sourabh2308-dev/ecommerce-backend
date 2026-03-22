package com.sourabh.user_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponse {

    private Long id;

    private String senderUuid;

    private String senderRole;

    private String content;

    private LocalDateTime createdAt;
}
