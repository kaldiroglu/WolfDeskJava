package dev.kaldiroglu.wolfdesk.ticketing.domain.commands;

import dev.kaldiroglu.wolfdesk.ticketing.domain.MessageId;
import dev.kaldiroglu.wolfdesk.ticketing.domain.UserId;

import java.time.Instant;

/** Command: the receiver of a message has read it. */
public record AcknowledgeMessage(MessageId messageId, UserId by, Instant at) {
}
