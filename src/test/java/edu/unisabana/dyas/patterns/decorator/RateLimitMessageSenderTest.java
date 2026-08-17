package edu.unisabana.dyas.patterns.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimitMessageSenderTest {

    private final RecordingMessageSender client = new RecordingMessageSender();
    private final RecordingLogger logger = new RecordingLogger();
    private final ManualTimeSource clock = new ManualTimeSource(0L);
    private final RateLimitMessageSender sender = new RateLimitMessageSender(
            client,
            RateLimitMessageSender.DEFAULT_MAX_MESSAGES,
            RateLimitMessageSender.DEFAULT_WINDOW_MILLIS,
            clock,
            logger);

    @Test
    @DisplayName("Los tres primeros mensajes del segundo se entregan")
    void dejaPasarLosTresPrimeros() {
        sender.sendMessage("uno");
        sender.sendMessage("dos");
        sender.sendMessage("tres");

        assertEquals(3, client.count());
        assertTrue(logger.getLines().isEmpty());
    }

    @Test
    @DisplayName("Del cuarto mensaje en adelante, dentro del mismo segundo, se bloquea")
    void bloqueaDesdeElCuartoMensaje() {
        for (int i = 1; i <= 5; i++) {
            sender.sendMessage("mensaje " + i);
            clock.advance(10);
        }

        assertEquals(3, client.count());
        assertEquals(2, logger.getLines().size());
        assertEquals("Mensaje bloqueado por exceso de frecuencia de envío", logger.getLines().get(0));
        assertEquals("Mensaje bloqueado por exceso de frecuencia de envío", logger.getLines().get(1));
    }

    @Test
    @DisplayName("Pasada la ventana de 1 segundo, se vuelve a permitir el envío")
    void permiteDeNuevoTrasVencerLaVentana() {
        sender.sendMessage("uno");
        sender.sendMessage("dos");
        sender.sendMessage("tres");
        sender.sendMessage("bloqueado");

        assertEquals(3, client.count());
        assertEquals(1, logger.getLines().size());

        clock.advance(1001);
        sender.sendMessage("nuevo segundo");

        assertEquals(4, client.count());
        assertEquals(1, logger.getLines().size());
    }

    @Test
    @DisplayName("La ventana es deslizante: solo cuentan los envíos del último segundo")
    void laVentanaEsDeslizante() {
        sender.sendMessage("uno");
        clock.advance(600);
        sender.sendMessage("dos");
        clock.advance(500); // el primero ya salió de la ventana
        sender.sendMessage("tres");
        sender.sendMessage("cuatro");

        assertEquals(4, client.count());
        assertTrue(logger.getLines().isEmpty());
    }
}
