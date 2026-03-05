package com.sourabh.user_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response payload representing a single message within a support-ticket
 * conversation thread.
 *
 * <p>Messages can be sent by either the user or an admin; the
 * {@link #senderRole} field distinguishes the two.</p>
 */
@Getter
@Builder
public class MessageResponse {

    /** Internal database identifier of the message. */
    private Long id;

    /** UUID of the user or admin who sent this message. */
    private String senderUuid;

    /** Role of the sender (e.g. BUYER, ADMIN). */
    private String senderRole;

    /** Text body of the message. */
    private String content;

    /** Timestamp when the message was created. */
    private LocalDateTime createdAt;
}
