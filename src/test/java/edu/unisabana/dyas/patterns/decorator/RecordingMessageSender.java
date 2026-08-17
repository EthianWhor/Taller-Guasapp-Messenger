package edu.unisabana.dyas.patterns.decorator;

import edu.unisabana.dyas.patterns.util.MessageSender;
import java.util.ArrayList;
import java.util.List;

/**
 * Doble de prueba que ocupa el lugar de MessagingClient y guarda los mensajes
 * que efectivamente le llegaron.
 */
class RecordingMessageSender implements MessageSender {

    private final List<String> received = new ArrayList<>();

    @Override
    public void sendMessage(String message) {
        received.add(message);
    }

    List<String> getReceived() {
        return received;
    }

    int count() {
        return received.size();
    }
}
