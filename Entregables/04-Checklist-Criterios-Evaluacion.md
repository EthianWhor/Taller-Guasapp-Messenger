# Checklist de criterios de evaluación

Cada criterio del enunciado con el lugar exacto del código donde se cumple y la evidencia que lo respalda. Las rutas son relativas a la raíz del repositorio.

---

## Diseño

### Diseño 1 — `MessagingClient` (clase de terceros) no debe modificarse

**Cumplido.** El paquete `util/` completo quedó tal como venía; toda la solución vive en el paquete nuevo `decorator/`.

- `src/main/java/edu/unisabana/dyas/patterns/util/MessagingClient.java` — sin cambios respecto al commit base `2dea6ed`
- `src/main/java/edu/unisabana/dyas/patterns/util/MessageSender.java` — sin cambios

Evidencia: `03-Evidencias/verificacion-messagingclient-intacto.txt`
- `git diff --stat HEAD -- .../util/` → sin salida (cero cambios)
- `git status --porcelain -- .../util/` → sin salida (ningún archivo modificado)
- `git log --oneline -1 -- .../MessagingClient.java` → `2dea6ed Correciones a taller` (el commit de la plantilla base)

---

### Diseño 2 — La solución se apoya en `MessageSender`, sin `if`/`switch` sobre el tipo de validación en el launcher

**Cumplido.** El launcher solo compone objetos y luego trabaja contra el tipo de la interfaz:

- `GuasappProgramLauncher.java:19-22` — armado de la cadena:
  ```java
  MessageSender secureSender =
          new DangerousContentMessageSender(
                  new MaxLengthMessageSender(
                          new RateLimitMessageSender(originalClient)));
  ```
- `GuasappProgramLauncher.java:25-45` — todos los envíos son `secureSender.sendMessage(...)`, sin conocer qué validaciones hay detrás
- `MessageSenderDecorator.java:15` — `public abstract class MessageSenderDecorator implements MessageSender`
- `MessageSenderDecorator.java:37` — el único `if` del diseño es el genérico "aceptado / bloqueado", no una ramificación por tipo de validación

Evidencia: `03-Evidencias/verificacion-messagingclient-intacto.txt`
- `grep -nE '\b(if|switch)\b' GuasappProgramLauncher.java` → **0 coincidencias**

---

### Diseño 3 — Debe poder agregarse una cuarta validación sin modificar las tres existentes ni `MessagingClient` (abierto/cerrado)

**Cumplido.** La estructura del Decorator lo garantiza:

- `MessageSenderDecorator.java:36-42` — `sendMessage()` es `final` y concentra la lógica común (delegar o registrar)
- `MessageSenderDecorator.java:45` y `:48` — los dos únicos métodos que una validación nueva debe implementar: `isAllowed(String)` y `blockedReason()`
- Las tres validaciones existentes no se referencian entre sí: cada una solo extiende la base
  - `DangerousContentMessageSender.java:25-27`
  - `MaxLengthMessageSender.java:32-34`
  - `RateLimitMessageSender.java:47-55`

Una cuarta regla es una clase nueva con dos métodos, más un nivel en el armado de la cadena. Código de ejemplo y diagrama:
- `01-Informe-Taller.md` §4
- `02-Diagramas/04-extension-cuarta-validacion.png`

Prueba que respalda la combinabilidad: `SecureMessageSenderChainTest.permiteCombinarSoloAlgunasValidaciones` — se usa una sola validación de la cadena, sin tocar las demás.

---

## Funcionalidad

### Funcional 1 — El mensaje `"##{./exec(rm /* -r)}"` se bloquea con su log correspondiente

**Cumplido.**

- `DangerousContentMessageSender.java:14` — `Pattern.compile("##\\{[^}]*\\}")`
- `DangerousContentMessageSender.java:25-27` — `isAllowed()`
- `DangerousContentMessageSender.java:12` — texto exacto: `"Mensaje bloqueado debido a contenido peligroso"`

Evidencia:
- `03-Evidencias/salida-ejecucion-completa.txt`, línea 5 → `Mensaje bloqueado debido a contenido peligroso`
- Pruebas: `DangerousContentMessageSenderTest` (4 casos, incluidos `##` sin llaves y el patrón incrustado en medio del texto)

---

### Funcional 2 — Un mensaje de más de 200 caracteres se bloquea con su log correspondiente

**Cumplido.**

- `MaxLengthMessageSender.java:32-34` — `message.length() <= maxLength`
- `MaxLengthMessageSender.java:13` — `DEFAULT_MAX_LENGTH = 200`
- `MaxLengthMessageSender.java:11` — texto exacto: `"Mensaje bloqueado por exceder la longitud máxima permitida"`

Evidencia:
- `03-Evidencias/salida-ejecucion-completa.txt`, línea 6 → `Mensaje bloqueado por exceder la longitud máxima permitida`
- Pruebas de frontera: `MaxLengthMessageSenderTest` — 200 caracteres pasa, 201 se bloquea
- `02-Diagramas/03-secuencia-mensaje-bloqueado.png` — el recorrido del mensaje por la cadena

---

### Funcional 3 — A partir del 4º mensaje enviado en menos de 1 segundo, se bloquean con su log correspondiente

**Cumplido.**

- `RateLimitMessageSender.java:47-55` — ventana deslizante sobre `Deque<Long>` de marcas de tiempo
- `RateLimitMessageSender.java:18-19` — `DEFAULT_MAX_MESSAGES = 3`, `DEFAULT_WINDOW_MILLIS = 1000`
- `RateLimitMessageSender.java:16` — texto exacto: `"Mensaje bloqueado por exceso de frecuencia de envío"`

Evidencia:
- `03-Evidencias/salida-ejecucion-completa.txt`, líneas 7-11 → los mensajes de ráfaga #1, #2 y #3 se entregan; #4 y #5 se bloquean
- Pruebas deterministas con reloj inyectado: `RateLimitMessageSenderTest` (4 casos, incluido el vencimiento de la ventana y su carácter deslizante)

Nota: el launcher espera 1100 ms antes de la ráfaga (`GuasappProgramLauncher.java:39`) para que el mensaje válido inicial no consuma cupo de esa ventana. Justificación en `01-Informe-Taller.md` §3.4.

---

### Funcional 4 — Los mensajes que no violan ninguna regla se entregan con normalidad

**Cumplido.**

- `MessageSenderDecorator.java:38` — `wrapped.sendMessage(message)` cuando la validación acepta

Evidencia:
- `03-Evidencias/salida-ejecucion-completa.txt`, líneas 4 y 7-9 → cuatro `Sending message: ...` producidos por `MessagingClient`
- Pruebas: `SecureMessageSenderChainTest.entregaUnMensajeValido`

---

## Resumen

| Criterio | Estado |
|---|---|
| Diseño 1 — `MessagingClient` intacto | ✔ |
| Diseño 2 — Se apoya en `MessageSender`, sin `if`/`switch` en el launcher | ✔ |
| Diseño 3 — Abierto/cerrado para una cuarta validación | ✔ |
| Funcional 1 — Contenido peligroso bloqueado | ✔ |
| Funcional 2 — Longitud excesiva bloqueada | ✔ |
| Funcional 3 — Exceso de frecuencia bloqueado | ✔ |
| Funcional 4 — Mensajes válidos entregados | ✔ |
| Pruebas automáticas | 16/16 en verde |
