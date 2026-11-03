/**
 * Ticketing: the life of a support ticket, from opening to closing.
 *
 * <p>Ports and adapters (chapter 8), one package per role:
 * <ul>
 *   <li>{@code domain}: the ticket aggregate and its value objects, with its commands in
 *       {@code domain.commands} and its domain events in {@code domain.events}.</li>
 *   <li>{@code application}: the application service and the ports it needs.</li>
 *   <li>{@code inmemory}: the adapters. Nothing outside it imports it.</li>
 *   <li>{@code publishedlanguage}: the public messages for other bounded contexts.</li>
 *   <li>{@code eventsourced} and {@code readmodels}: chapter 7's event-sourced ticket and
 *       the CQRS read model.</li>
 * </ul>
 */
package dev.kaldiroglu.wolfdesk.ticketing;
