package edu.unisabana.dyas.patterns;

// GuasappProgramLauncher.java
import edu.unisabana.dyas.patterns.decorator.DangerousContentMessageSender;
import edu.unisabana.dyas.patterns.decorator.MaxLengthMessageSender;
import edu.unisabana.dyas.patterns.decorator.RateLimitMessageSender;
import edu.unisabana.dyas.patterns.util.MessageSender;
import edu.unisabana.dyas.patterns.util.MessagingClient;

public class GuasappProgramLauncher {
    public static void main(String[] args) throws InterruptedException {

        // Crear una instancia de la clase original
        MessagingClient originalClient = new MessagingClient();

        // Se envuelve el cliente original con las validaciones, sin modificarlo.
        // Contenido y longitud quedan por fuera para que un mensaje rechazado por
        // ellas no consuma cupo del control de frecuencia.
        MessageSender secureSender =
                new DangerousContentMessageSender(
                        new MaxLengthMessageSender(
                                new RateLimitMessageSender(originalClient)));

        // Mensaje normal: debe entregarse.
        secureSender.sendMessage("Hola, ¿cómo estás?");

        // Contenido peligroso: debe bloquearse.
        secureSender.sendMessage("##{./exec(rm /* -r)}");

        // Longitud excesiva (más de 200 caracteres): debe bloquearse.
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 201; i++) {
            longMessage.append('a');
        }
        secureSender.sendMessage(longMessage.toString());

        // El mensaje normal anterior ya consumió un cupo de la ventana de 1 segundo.
        // Se espera a que la ventana se vacíe para que la ráfaga se demuestre aislada.
        Thread.sleep(1100);

        // Ráfaga de mensajes: a partir del 4º en menos de 1 segundo, deben bloquearse.
        for (int i = 1; i <= 5; i++) {
            secureSender.sendMessage("Mensaje de ráfaga #" + i);
        }
    }
}
