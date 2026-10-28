package dev.kaldiroglu.wolfdesk.ticketing;

import java.time.Instant;
import java.util.Objects;

/** One of an agent's working periods, from the shift schedule (Preface, p. xix). */
public record Shift(Instant start, Instant end) {

    public Shift {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) throw new IllegalArgumentException("A shift ends after it starts");
    }
}
