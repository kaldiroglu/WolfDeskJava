/**
 * Tenants' service layer: transaction scripts that orchestrate the active records. Like
 * {@code UserService.Create} in chapter 8 (p. 123), each one starts, commits or rolls back
 * the database transaction itself; apart from that, it works through
 * {@link dev.kaldiroglu.wolfdesk.tenants.businesslogic}.
 */
package dev.kaldiroglu.wolfdesk.tenants.service;
