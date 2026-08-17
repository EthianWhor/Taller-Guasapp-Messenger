package edu.unisabana.dyas.patterns.decorator;

import java.util.ArrayList;
import java.util.List;

/**
 * Registro en memoria: permite verificar las lineas de bloqueo sin capturar la
 * salida estandar.
 */
class RecordingLogger implements BlockedMessageLogger {

    private final List<String> lines = new ArrayList<>();

    @Override
    public void log(String reason) {
        lines.add(reason);
    }

    List<String> getLines() {
        return lines;
    }
}
