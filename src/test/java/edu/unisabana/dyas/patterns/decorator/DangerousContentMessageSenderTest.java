package edu.unisabana.dyas.patterns.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DangerousContentMessageSenderTest {

    private final RecordingMessageSender client = new RecordingMessageSender();
    private final RecordingLogger logger = new RecordingLogger();
    private final DangerousContentMessageSender sender =
            new DangerousContentMessageSender(client, logger);

    @Test
    @DisplayName("El mensaje del enunciado con ##{...} se bloquea y no llega al cliente")
    void bloqueaElMensajePeligrosoDelEnunciado() {
        sender.sendMessage("##{./exec(rm /* -r)}");

        assertEquals(0, client.count());
        assertEquals(
                Collections.singletonList("Mensaje bloqueado debido a contenido peligroso"),
                logger.getLines());
    }

    @Test
    @DisplayName("El patrón peligroso se detecta aunque esté en medio del texto")
    void bloqueaElPatronPeligrosoIncrustado() {
        sender.sendMessage("Hola ##{payload} mira esto");

        assertEquals(0, client.count());
        assertEquals(1, logger.getLines().size());
    }

    @Test
    @DisplayName("Un mensaje normal se entrega sin registrar bloqueos")
    void dejaPasarUnMensajeNormal() {
        sender.sendMessage("Hola, ¿cómo estás?");

        assertEquals(Collections.singletonList("Hola, ¿cómo estás?"), client.getReceived());
        assertTrue(logger.getLines().isEmpty());
    }

    @Test
    @DisplayName("Los caracteres ## sin llaves no constituyen contenido peligroso")
    void dejaPasarNumeralesSinLlaves() {
        sender.sendMessage("Precio ## rebajado hoy");

        assertEquals(1, client.count());
        assertTrue(logger.getLines().isEmpty());
    }
}
