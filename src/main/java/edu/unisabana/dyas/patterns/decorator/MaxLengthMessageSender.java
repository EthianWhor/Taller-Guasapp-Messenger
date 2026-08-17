package edu.unisabana.dyas.patterns.decorator;

import edu.unisabana.dyas.patterns.util.MessageSender;

/**
 * Bloquea los mensajes que superan la longitud maxima permitida (200 caracteres
 * por defecto), como proteccion basica frente a payloads abusivamente grandes.
 */
public class MaxLengthMessageSender extends MessageSenderDecorator {

    public static final String BLOCKED_REASON = "Mensaje bloqueado por exceder la longitud máxima permitida";

    public static final int DEFAULT_MAX_LENGTH = 200;

    private final int maxLength;

    public MaxLengthMessageSender(MessageSender wrapped) {
        this(wrapped, DEFAULT_MAX_LENGTH);
    }

    public MaxLengthMessageSender(MessageSender wrapped, int maxLength) {
        super(wrapped);
        this.maxLength = maxLength;
    }

    public MaxLengthMessageSender(MessageSender wrapped, int maxLength, BlockedMessageLogger logger) {
        super(wrapped, logger);
        this.maxLength = maxLength;
    }

    @Override
    protected boolean isAllowed(String message) {
        return message == null || message.length() <= maxLength;
    }

    @Override
    protected String blockedReason() {
        return BLOCKED_REASON;
    }
}
