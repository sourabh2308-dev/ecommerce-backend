package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for sending a message within a support ticket
 * conversation thread.
 *
 * <p>Messages are appended to an existing ticket identified by its UUID
 * (passed as a path variable in the controller).</p>
 */
@Getter
@Setter
public class SendMessageRequest {

    /** Text content of the message (max 2000 characters). */
    @NotBlank(message = "Message content is required")
    @Size(max = 2000, message = "Message must be under 2000 characters")
    private String content;
}
