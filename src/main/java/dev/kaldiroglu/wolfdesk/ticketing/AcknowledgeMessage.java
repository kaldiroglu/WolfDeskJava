package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;

/** Command: the receiver of a message has read it. */
public record AcknowledgeMessage(MessageId messageId, UserId by, Instant at) {
}
