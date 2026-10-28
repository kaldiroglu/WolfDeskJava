/**
 * Billing: charging tenants for the tickets they open (Preface, p. xviii).
 *
 * <p>Implemented with the transaction script pattern (chapter 5). Each public operation is
 * one procedure that either succeeds or fails as a whole, and each can be repeated safely.
 */
package dev.kaldiroglu.wolfdesk.billing;
