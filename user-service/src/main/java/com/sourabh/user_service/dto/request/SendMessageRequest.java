package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for adding a support ticket message.
 */
@Getter
@Setter
public class SendMessageRequest {

    @NotBlank(message = "Message content is required")
    @Size(max = 2000, message = "Message must be under 2000 characters")
    private String content;
}
