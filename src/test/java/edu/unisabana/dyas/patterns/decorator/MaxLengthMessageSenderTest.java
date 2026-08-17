package edu.unisabana.dyas.patterns.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MaxLengthMessageSenderTest {

    private final RecordingMessageSender client = new RecordingMessageSender();
    private final RecordingLogger logger = new RecordingLogger();
    private final MaxLengthMessageSender sender =
            new MaxLengthMessageSender(client, MaxLengthMessageSender.DEFAULT_MAX_LENGTH, logger);

    private static String repeat(char c, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(c);
        }
        return builder.toString();
    }

    @Test
    @DisplayName("Un mensaje de exactamente 200 caracteres se entrega")
    void dejaPasarElLimiteExacto() {
        sender.sendMessage(repeat('a', 200));

        assertEquals(1, client.count());
        assertTrue(logger.getLines().isEmpty());
    }

    @Test
    @DisplayName("Un mensaje de 201 caracteres se bloquea con su línea de log")
    void bloqueaUnCaracterPorEncimaDelLimite() {
        sender.sendMessage(repeat('a', 201));

        assertEquals(0, client.count());
        assertEquals(
                Collections.singletonList("Mensaje bloqueado por exceder la longitud máxima permitida"),
                logger.getLines());
    }

    @Test
    @DisplayName("El límite es configurable sin tocar la clase")
    void permiteConfigurarOtroLimite() {
        MaxLengthMessageSender corto = new MaxLengthMessageSender(client, 5, logger);

        corto.sendMessage("12345");
        corto.sendMessage("123456");

        assertEquals(1, client.count());
        assertEquals(1, logger.getLines().size());
    }
}
