package edu.unisabana.dyas.patterns.decorator;

/**
 * Fuente de tiempo del sistema.
 *
 * Se inyecta en {@link RateLimitMessageSender} para que la validacion de
 * frecuencia pueda probarse de forma determinista, sin depender de esperas
 * reales durante las pruebas.
 */
public interface TimeSource {

    long currentTimeMillis();
}
