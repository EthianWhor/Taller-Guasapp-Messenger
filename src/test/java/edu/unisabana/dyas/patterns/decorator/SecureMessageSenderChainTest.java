package edu.unisabana.dyas.patterns.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.unisabana.dyas.patterns.util.MessageSender;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica la cadena completa tal como la arma el launcher, incluida la
 * posibilidad de combinar solo algunas validaciones.
 */
class SecureMessageSenderChainTest {

    private final RecordingMessageSender client = new RecordingMessageSender();
    private final RecordingLogger logger = new RecordingLogger();
    private final ManualTimeSource clock = new ManualTimeSource(0L);

    private final MessageSender chain = new DangerousContentMessageSender(
            new MaxLengthMessageSender(
                    new RateLimitMessageSender(
                            client,
                            RateLimitMessageSender.DEFAULT_MAX_MESSAGES,
                            RateLimitMessageSender.DEFAULT_WINDOW_MILLIS,
                            clock,
                            logger),
                    MaxLengthMessageSender.DEFAULT_MAX_LENGTH,
                    logger),
            logger);

    private static String repeat(char c, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(c);
        }
        return builder.toString();
    }

    @Test
    @DisplayName("Un mensaje válido atraviesa toda la cadena y llega al cliente")
    void entregaUnMensajeValido() {
        chain.sendMessage("Hola, ¿cómo estás?");

        assertEquals(Collections.singletonList("Hola, ¿cómo estás?"), client.getReceived());
        assertTrue(logger.getLines().isEmpty());
    }

    @Test
    @DisplayName("Un mensaje bloqueado registra exactamente una línea, la de su regla")
    void registraUnaSolaLineaPorMensajeBloqueado() {
        chain.sendMessage("##{./exec(rm /* -r)}");

        assertEquals(0, client.count());
        assertEquals(
                Collections.singletonList("Mensaje bloqueado debido a contenido peligroso"),
                logger.getLines());
    }

    @Test
    @DisplayName("Un mensaje peligroso y largo se reporta solo por contenido peligroso")
    void laReglaMasExternaGanaSinAcumularLogs() {
        chain.sendMessage("##{payload}" + repeat('a', 300));

        assertEquals(0, client.count());
        assertEquals(
                Collections.singletonList("Mensaje bloqueado debido a contenido peligroso"),
                logger.getLines());
    }

    @Test
    @DisplayName("Los mensajes rechazados antes no consumen cupo del control de frecuencia")
    void losBloqueadosNoConsumenCupoDeFrecuencia() {
        chain.sendMessage("##{payload}");
        chain.sendMessage(repeat('a', 201));
        chain.sendMessage("uno");
        chain.sendMessage("dos");
        chain.sendMessage("tres");

        assertEquals(3, client.count());
        assertEquals(2, logger.getLines().size());
    }

    @Test
    @DisplayName("Se pueden activar solo algunas validaciones sin tocar las demás")
    void permiteCombinarSoloAlgunasValidaciones() {
        MessageSender soloLongitud = new MaxLengthMessageSender(client, 10, logger);

        soloLongitud.sendMessage("##{payload}corto");
        soloLongitud.sendMessage("ok");

        assertEquals(Collections.singletonList("ok"), client.getReceived());
        assertEquals(
                Collections.singletonList("Mensaje bloqueado por exceder la longitud máxima permitida"),
                logger.getLines());
    }
}
