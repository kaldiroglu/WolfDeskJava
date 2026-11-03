package dev.kaldiroglu.wolfdesk.ticketing.domain;

/** Why a ticket was escalated: the response deadline passed, or the customer asked. */
public enum EscalationReason {
    MISSED_SLA, CUSTOMER_REQUEST
}
