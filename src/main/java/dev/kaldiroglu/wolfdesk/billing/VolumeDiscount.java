package dev.kaldiroglu.wolfdesk.billing;

/**
 * The automatic volume discounts for monthly tickets: 10% for opening more than 500 tickets,
 * 20% for more than 750, and 30% for more than 1,000 (Preface, p. xviii).
 */
public final class VolumeDiscount {

    private VolumeDiscount() {
    }

    public static int percentFor(int ticketsOpened) {
        if (ticketsOpened > 1_000) return 30;
        if (ticketsOpened > 750) return 20;
        if (ticketsOpened > 500) return 10;
        return 0;
    }
}
