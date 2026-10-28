package dev.kaldiroglu.wolfdesk.ticketing;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ValueObjectTest {

    @Test
    void twoIdsWithTheSameValueAreEqual() {
        var value = UUID.randomUUID();
        assertEquals(new TicketId(value), new TicketId(value));
        assertNotEquals(new TicketId(value), TicketId.random());
    }

    @Test
    void anIdCannotBeEmpty() {
        assertThrows(NullPointerException.class, () -> new UserId(null));
    }

    @Test
    void aTimeframeEndsAfterItStarts() {
        var t = Instant.parse("2026-10-28T20:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> new ResponseTimeframe(t, t));
    }

    @Test
    void theRemainingTimeIsAShareOfTheWholeTimeframe() {
        var timeframe = new ResponseTimeframe(Instant.parse("2026-10-28T20:00:00Z"),
                Instant.parse("2026-10-28T22:00:00Z"));

        assertEquals(1.0, timeframe.remainingTimePercentage(Instant.parse("2026-10-28T20:00:00Z")));
        assertEquals(0.5, timeframe.remainingTimePercentage(Instant.parse("2026-10-28T21:00:00Z")));
        assertEquals(-0.5, timeframe.remainingTimePercentage(Instant.parse("2026-10-28T23:00:00Z")));
    }
}
