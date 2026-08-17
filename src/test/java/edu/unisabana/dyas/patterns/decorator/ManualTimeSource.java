package edu.unisabana.dyas.patterns.decorator;

/**
 * Reloj controlado a mano: permite probar la validacion de frecuencia sin
 * esperas reales y con resultados siempre reproducibles.
 */
class ManualTimeSource implements TimeSource {

    private long currentMillis;

    ManualTimeSource(long initialMillis) {
        this.currentMillis = initialMillis;
    }

    @Override
    public long currentTimeMillis() {
        return currentMillis;
    }

    void advance(long millis) {
        currentMillis += millis;
    }
}
