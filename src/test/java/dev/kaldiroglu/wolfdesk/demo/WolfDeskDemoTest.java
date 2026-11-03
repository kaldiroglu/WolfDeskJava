package dev.kaldiroglu.wolfdesk.demo;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WolfDeskDemoTest {

    @Test
    void theDemoRunsTheStoryAndChargesTheTenantForOneTicket() {
        var buffer = new ByteArrayOutputStream();
        var invoice = new WolfDeskDemo(new PrintStream(buffer, true, StandardCharsets.UTF_8)).run();
        var output = buffer.toString(StandardCharsets.UTF_8);

        assertEquals(1, invoice.ticketsOpened());
        assertEquals(0, invoice.discountPercent());
        assertEquals(new BigDecimal("2.00"), invoice.amount());
        assertTrue(output.contains("An agent is assigned; the response deadline is 12:00"), output);
        assertTrue(output.contains("11:00 The customer asks for an escalation: refused, the response deadline has not passed yet"), output);
        assertTrue(output.contains("12:00 The customer asks again: escalated"), output);
        assertTrue(output.contains("Tickets counted before the relay runs: 0"), output);
        assertTrue(output.contains("The outbox relay publishes TicketOpenedMessage, TicketEscalatedMessage, TicketClosedMessage"), output);
        assertTrue(output.contains("Tickets counted after the relay runs: 1"), output);
    }
}
