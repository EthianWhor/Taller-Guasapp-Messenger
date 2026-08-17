package edu.unisabana.dyas.patterns.decorator;

import edu.unisabana.dyas.patterns.util.MessageSender;
import java.util.regex.Pattern;

/**
 * Bloquea los mensajes que contienen el patron peligroso {@code ##{...}}, que
 * daria acceso a terceros al equipo donde se ejecute.
 */
public class DangerousContentMessageSender extends MessageSenderDecorator {

    public static final String BLOCKED_REASON = "Mensaje bloqueado debido a contenido peligroso";

    private static final Pattern DANGEROUS_PATTERN = Pattern.compile("##\\{[^}]*\\}");

    public DangerousContentMessageSender(MessageSender wrapped) {
        super(wrapped);
    }

    public DangerousContentMessageSender(MessageSender wrapped, BlockedMessageLogger logger) {
        super(wrapped, logger);
    }

    @Override
    protected boolean isAllowed(String message) {
        return message == null || !DANGEROUS_PATTERN.matcher(message).find();
    }

    @Override
    protected String blockedReason() {
        return BLOCKED_REASON;
    }
}
