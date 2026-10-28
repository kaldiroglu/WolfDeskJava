package dev.kaldiroglu.wolfdesk.billing;

import java.math.BigDecimal;
import java.time.YearMonth;

/** What a tenant owes for one charging period. */
public record Invoice(TenantId tenant, YearMonth period, int ticketsOpened,
                      BigDecimal pricePerTicket, int discountPercent, BigDecimal amount) {
}
