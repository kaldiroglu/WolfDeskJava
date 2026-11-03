/**
 * Billing: charging tenants for the tickets they open (Preface, p. xviii).
 *
 * <p>Implemented with the transaction script pattern (chapter 5), in a layered architecture
 * (chapter 8), one package per layer. Each layer depends only on the layer beneath it:
 * <ul>
 *   <li>{@code presentation}: how billing receives requests. Its anticorruption layer
 *       subscribes to ticketing's published language (chapter 9).</li>
 *   <li>{@code businesslogic}: the transaction scripts. They are the service layer as well:
 *       "when the business logic is implemented as a transaction script, it essentially is a
 *       service layer" (p. 124), so billing has no separate one.</li>
 *   <li>{@code dataaccess}: the database the scripts work on, the product owners it looks
 *       up, the records they store, and the in-memory implementations.</li>
 * </ul>
 */
package dev.kaldiroglu.wolfdesk.billing;
