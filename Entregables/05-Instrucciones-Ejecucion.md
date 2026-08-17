# Instrucciones de ejecución

## Requisitos

- JDK 8 o superior (verificado con Temurin 17)
- Apache Maven 3.6+ (verificado con 3.9.9)

Todos los comandos se ejecutan desde la **raíz del repositorio** (donde está `pom.xml`).

---

## Compilar

```bash
mvn compile
```

## Ejecutar el programa

```bash
mvn exec:java -Dexec.mainClass=edu.unisabana.dyas.patterns.GuasappProgramLauncher
```

Como `exec-maven-plugin` quedó configurado en el `pom.xml` con la clase principal, también basta con:

```bash
mvn exec:java
```

> **PowerShell:** hay que entrecomillar el parámetro, o PowerShell lo parte en el punto:
> `mvn exec:java "-Dexec.mainClass=edu.unisabana.dyas.patterns.GuasappProgramLauncher"`

### Salida esperada

```
Sending message: Hola, ¿cómo estás?
Mensaje bloqueado debido a contenido peligroso
Mensaje bloqueado por exceder la longitud máxima permitida
Sending message: Mensaje de ráfaga #1
Sending message: Mensaje de ráfaga #2
Sending message: Mensaje de ráfaga #3
Mensaje bloqueado por exceso de frecuencia de envío
Mensaje bloqueado por exceso de frecuencia de envío
```

El programa tarda algo más de un segundo: espera a propósito para que la ráfaga se demuestre con la ventana de frecuencia vacía (ver `01-Informe-Taller.md` §3.4).

> Si los acentos salen mal en la consola de Windows, ejecutar antes `chcp 65001`, o pasar `MAVEN_OPTS="-Dfile.encoding=UTF-8"`.

## Ejecutar las pruebas

```bash
mvn test
```

Deben pasar **16 pruebas** repartidas en cuatro clases. No dependen del reloj real, así que no son intermitentes.

---

## Cambiar la configuración

### Activar solo algunas validaciones

Se quitan o agregan niveles en el armado de la cadena, en `GuasappProgramLauncher.java:19-22`. Por ejemplo, solo contenido peligroso y longitud:

```java
MessageSender secureSender =
        new DangerousContentMessageSender(
                new MaxLengthMessageSender(originalClient));
```

El resto del launcher no cambia: sigue llamando a `secureSender.sendMessage(...)`.

### Cambiar los umbrales

Todos los límites son parámetros de constructor; no hay que tocar el cuerpo de las clases:

```java
// Longitud máxima de 500 caracteres en vez de 200
new MaxLengthMessageSender(originalClient, 500)

// 10 mensajes cada 5 segundos en vez de 3 por segundo
new RateLimitMessageSender(originalClient, 10, 5000L, new SystemTimeSource())
```

### Cambiar el orden de las validaciones

Basta con reordenar el anidamiento. La regla más externa es la que se evalúa primero y, si bloquea, la que registra el log. Con contenido y longitud por fuera, un mensaje rechazado por ellas no consume cupo del control de frecuencia.

### Redirigir el log de bloqueos

Todos los decoradores aceptan un `BlockedMessageLogger` en el constructor. Por defecto escriben en la salida estándar; se puede sustituir por cualquier destino:

```java
BlockedMessageLogger auditoria = reason -> miSistemaDeAuditoria.registrar(reason);
new DangerousContentMessageSender(originalClient, auditoria);
```

---

## Agregar una cuarta validación

1. Crear una clase que extienda `MessageSenderDecorator` en `src/main/java/edu/unisabana/dyas/patterns/decorator/`.
2. Implementar sus dos métodos: `isAllowed(String)` y `blockedReason()`.
3. Añadir un nivel más al armado de la cadena en el launcher.

No hay que modificar ninguna de las tres validaciones existentes, ni la clase base, ni `MessagingClient`. Código de ejemplo completo en `01-Informe-Taller.md` §4.

---

## Regenerar los diagramas

Los `.png` de `02-Diagramas/` se generan desde los `.puml` con PlantUML:

```bash
java -jar plantuml.jar -charset UTF-8 -tpng Entregables/02-Diagramas/*.puml
```
