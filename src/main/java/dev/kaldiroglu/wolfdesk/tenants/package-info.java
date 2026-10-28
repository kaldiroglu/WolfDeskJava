/**
 * Tenants: what each tenant configures in the administration interface, the ticket
 * categories and the products it supports (Preface, p. xix).
 *
 * <p>Implemented with the active record pattern (chapter 5): {@link Tenant} wraps rows in
 * the database and knows how to save itself; the operations are transaction scripts that
 * manipulate it.
 */
package dev.kaldiroglu.wolfdesk.tenants;
