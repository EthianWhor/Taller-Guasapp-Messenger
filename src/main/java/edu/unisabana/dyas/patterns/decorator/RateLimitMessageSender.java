package edu.unisabana.dyas.patterns.decorator;

import edu.unisabana.dyas.patterns.util.MessageSender;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bloquea los mensajes que exceden la frecuencia permitida: por defecto, mas de
 * 3 mensajes dentro de una ventana deslizante de 1 segundo.
 *
 * Solo se contabilizan los mensajes que llegan hasta este decorador; los que
 * fueron rechazados por una validacion anterior de la cadena no consumen cupo.
 */
public class RateLimitMessageSender extends MessageSenderDecorator {

    public static final String BLOCKED_REASON = "Mensaje bloqueado por exceso de frecuencia de envío";

    public static final int DEFAULT_MAX_MESSAGES = 3;
    public static final long DEFAULT_WINDOW_MILLIS = 1000L;

    private final int maxMessages;
    private final long windowMillis;
    private final TimeSource timeSource;
    private final Deque<Long> sentTimestamps = new ArrayDeque<>();

    public RateLimitMessageSender(MessageSender wrapped) {
        this(wrapped, DEFAULT_MAX_MESSAGES, DEFAULT_WINDOW_MILLIS, new SystemTimeSource());
    }

    public RateLimitMessageSender(MessageSender wrapped, int maxMessages, long windowMillis,
                                  TimeSource timeSource) {
        super(wrapped);
        this.maxMessages = maxMessages;
        this.windowMillis = windowMillis;
        this.timeSource = timeSource;
    }

    public RateLimitMessageSender(MessageSender wrapped, int maxMessages, long windowMillis,
                                  TimeSource timeSource, BlockedMessageLogger logger) {
        super(wrapped, logger);
        this.maxMessages = maxMessages;
        this.windowMillis = windowMillis;
        this.timeSource = timeSource;
    }

    @Override
    protected boolean isAllowed(String message) {
        long now = timeSource.currentTimeMillis();
        discardOutsideWindow(now);
        if (sentTimestamps.size() >= maxMessages) {
            return false;
        }
        sentTimestamps.addLast(now);
        return true;
    }

    /** Descarta los envios que ya quedaron fuera de la ventana deslizante. */
    private void discardOutsideWindow(long now) {
        while (!sentTimestamps.isEmpty() && now - sentTimestamps.peekFirst() >= windowMillis) {
            sentTimestamps.removeFirst();
        }
    }

    @Override
    protected String blockedReason() {
        return BLOCKED_REASON;
    }
}
