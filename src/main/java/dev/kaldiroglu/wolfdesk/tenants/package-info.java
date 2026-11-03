/**
 * Tenants: what each tenant configures in the administration interface, the ticket
 * categories and the products it supports (Preface, p. xix).
 *
 * <p>Implemented with the active record pattern (chapter 5), in a layered architecture with
 * a service layer (chapter 8), one package per layer:
 * <ul>
 *   <li>{@code service}: the transaction scripts that orchestrate the active records and the
 *       transaction: "the service layer is required if the business logic pattern requires
 *       external orchestration, as in the case of the active record pattern" (p. 124).</li>
 *   <li>{@code businesslogic}: the active records, {@code Tenant} and {@code Product}.</li>
 *   <li>{@code dataaccess}: the database the active records read and write.</li>
 * </ul>
 * Tenants has no presentation layer: no caller outside the tests and the demo.
 */
package dev.kaldiroglu.wolfdesk.tenants;
