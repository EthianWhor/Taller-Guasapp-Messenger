package edu.unisabana.dyas.patterns.decorator;

import edu.unisabana.dyas.patterns.util.MessageSender;

/**
 * Base del patron Decorator sobre {@link MessageSender}.
 *
 * Concentra en un solo lugar la logica comun a todas las validaciones: si el
 * mensaje se acepta, se delega al siguiente eslabon de la cadena; si no, se
 * registra la linea de bloqueo y la llamada no continua.
 *
 * Cada validacion concreta solo aporta su predicado y su mensaje de bloqueo,
 * de modo que ninguna necesita conocer los detalles de las otras.
 */
public abstract class MessageSenderDecorator implements MessageSender {

    private final MessageSender wrapped;
    private final BlockedMessageLogger logger;

    protected MessageSenderDecorator(MessageSender wrapped) {
        this(wrapped, BlockedMessageLogger.console());
    }

    protected MessageSenderDecorator(MessageSender wrapped, BlockedMessageLogger logger) {
        if (wrapped == null) {
            throw new IllegalArgumentException("El MessageSender envuelto no puede ser nulo");
        }
        if (logger == null) {
            throw new IllegalArgumentException("El logger no puede ser nulo");
        }
        this.wrapped = wrapped;
        this.logger = logger;
    }

    @Override
    public final void sendMessage(String message) {
        if (isAllowed(message)) {
            wrapped.sendMessage(message);
        } else {
            logger.log(blockedReason());
        }
    }

    /** @return true si esta validacion deja pasar el mensaje. */
    protected abstract boolean isAllowed(String message);

    /** @return la linea exacta que debe registrarse cuando esta validacion bloquea. */
    protected abstract String blockedReason();
}
