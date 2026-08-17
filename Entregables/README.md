# Entregables — Taller de Patrones Estructurales: GuasappMessenger

**Universidad de La Sabana — Diseño y Arquitectura de Software**

- Estudiante: Ethian White
- Código: 352225
- Fecha de entrega: 16/08/2026

Esta carpeta contiene la entrega completa del taller: blindar el módulo de terceros `MessagingClient` con tres validaciones independientes y combinables, sin modificarlo, aplicando el patrón **Decorator** sobre la interfaz `MessageSender`.

## Contenido

| Archivo o carpeta | Qué es |
|---|---|
| [01-Informe-Taller.md](01-Informe-Taller.md) | Informe del taller: análisis del código inicial, por qué Decorator, decisiones tomadas y conclusiones |
| [02-Diagramas/](02-Diagramas/) | Diagramas de clases y de secuencia, en fuente PlantUML (`.puml`) y renderizados (`.png`) |
| [03-Evidencias/](03-Evidencias/) | Salidas reales de consola, resultado de las pruebas y verificación de que la clase de terceros quedó intacta |
| [04-Checklist-Criterios-Evaluacion.md](04-Checklist-Criterios-Evaluacion.md) | Cada criterio del enunciado con el archivo y la línea donde se cumple |
| [05-Instrucciones-Ejecucion.md](05-Instrucciones-Ejecucion.md) | Cómo compilar, probar, ejecutar, cambiar umbrales y agregar una cuarta validación |

El código no está en esta carpeta: vive en `src/main/java/` y `src/test/java/` en la raíz del repositorio, que es donde corresponde para que el proyecto Maven siga funcionando.

## Diagramas

| Diagrama | Contenido |
|---|---|
| `01-modelo-inicial` | Cómo estaba el proyecto: el launcher llamando directo al cliente de terceros, sin filtro alguno |
| `02-decorator-estructura` | Estructura del patrón: `MessageSender` como componente, `MessageSenderDecorator` como base y las tres validaciones concretas |
| `03-secuencia-mensaje-bloqueado` | Recorrido de un mensaje de 201 caracteres por la cadena, hasta el bloqueo |
| `04-extension-cuarta-validacion` | Cómo entra una cuarta validación sin modificar nada existente (abierto/cerrado) |

## Evidencias

| Archivo | Qué muestra |
|---|---|
| `salida-ejecucion-completa.txt` | Ejecución real del launcher: los cuatro escenarios del enunciado con sus tres líneas de bloqueo y sus entregas normales |
| `salida-pruebas-junit.txt` | `mvn test`: 16 pruebas, 0 fallos |
| `verificacion-messagingclient-intacto.txt` | `git diff`/`git status` sobre el paquete `util/`: cero cambios. Y `grep` de `if`/`switch` en el launcher: cero coincidencias |

## Resumen de lo implementado

Se resolvió el taller con el patrón **Decorator**. `MessageSenderDecorator` es una clase abstracta que implementa `MessageSender` y envuelve otro `MessageSender`; su método `sendMessage()` es `final` y contiene la única copia de la lógica común —delegar si el mensaje se acepta, registrar la línea de bloqueo si no—. Cada validación concreta aporta solo dos métodos: su predicado (`isAllowed`) y su mensaje (`blockedReason`).

Las tres validaciones (`DangerousContentMessageSender`, `MaxLengthMessageSender`, `RateLimitMessageSender`) son independientes entre sí y se apilan en el launcher sin un solo `if` ni `switch` sobre el tipo de validación. Agregar una cuarta es crear una clase con dos métodos y sumar un nivel a la cadena.

Como decisiones de diseño adicionales, `RateLimitMessageSender` recibe un `TimeSource` inyectable —lo que hace que la regla de frecuencia se pueda probar de forma determinista, sin esperas reales— y todos los decoradores aceptan un `BlockedMessageLogger` configurable. Se agregaron JUnit 5 y Surefire al `pom.xml`, con **16 pruebas en verde** que cubren los casos frontera (200 vs. 201 caracteres, `##` sin llaves, vencimiento y deslizamiento de la ventana de 1 segundo).

`MessagingClient` y `MessageSender` quedaron sin una sola modificación, verificado con `git`.
