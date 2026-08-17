package edu.unisabana.dyas.patterns.decorator;

/**
 * Implementacion de produccion de {@link TimeSource}: el reloj real de la maquina.
 */
public class SystemTimeSource implements TimeSource {

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
