/**
 * Billing's business logic layer: the transaction scripts and the volume discount they
 * apply. Each public operation is one procedure that either succeeds or fails as a whole,
 * and each can be repeated safely. It depends only on
 * {@link dev.kaldiroglu.wolfdesk.billing.dataaccess}.
 */
package dev.kaldiroglu.wolfdesk.billing.businesslogic;
