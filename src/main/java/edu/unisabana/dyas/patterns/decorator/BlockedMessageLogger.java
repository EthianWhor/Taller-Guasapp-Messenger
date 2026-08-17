package edu.unisabana.dyas.patterns.decorator;

/**
 * Destino donde se registran los mensajes bloqueados.
 *
 * Se extrae como interfaz para que los decoradores no dependan de la consola:
 * en produccion se usa {@link #console()} y en las pruebas se inyecta un
 * registro en memoria.
 */
public interface BlockedMessageLogger {

    void log(String reason);

    /** Registro por defecto: escribe la linea tal cual en la salida estandar. */
    static BlockedMessageLogger console() {
        return System.out::println;
    }
}
