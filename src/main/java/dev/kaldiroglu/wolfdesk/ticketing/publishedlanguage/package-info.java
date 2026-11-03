/**
 * Ticketing's published language (chapter 9, p. 140): the public messages other bounded
 * contexts may consume.
 *
 * <p>They are not the ticket's domain events. They carry only plain values, so a consumer
 * depends on this package and on nothing else in ticketing, and the ticket's model can
 * change without breaking them. Private events, such as a message being read, stay inside.
 */
package dev.kaldiroglu.wolfdesk.ticketing.publishedlanguage;
