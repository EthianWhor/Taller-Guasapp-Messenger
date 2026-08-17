# Informe del taller — GuasappMessenger

**Universidad de La Sabana — Diseño y Arquitectura de Software**
Estudiante: Ethian White · Código: 352225 · Fecha: 16/08/2026

---

## 1. Análisis del código inicial

El proyecto entregado tenía tres clases y ninguna protección:

| Archivo | Rol |
|---|---|
| `util/MessageSender.java` | Interfaz con un único método `sendMessage(String)` |
| `util/MessagingClient.java` | Cliente de terceros, `implements MessageSender`. Imprime `"Sending message: ..."`. **No modificable** |
| `GuasappProgramLauncher.java` | Programa principal. Llamaba directo a `originalClient.sendMessage(...)` |

El launcher traía un `TODO` explícito: *"envolver originalClient con las validaciones necesarias […] sin modificar MessagingClient"*. Es decir, cualquier mensaje llegaba al cliente sin filtro: contenido peligroso `##{...}`, mensajes gigantes y ráfagas.

Ver diagrama `02-Diagramas/01-modelo-inicial.png`.

### Las tres debilidades a blindar

1. **Contenido peligroso** — el patrón `##{...}` (p. ej. `##{./exec(rm /* -r)}`) da acceso a terceros al equipo.
2. **Longitud excesiva** — mensajes de más de 200 caracteres.
3. **Frecuencia de envío** — más de 3 mensajes en menos de 1 segundo.

Cada bloqueo debe registrar **exactamente una** línea, la de su regla.

---

## 2. Diseño propuesto: patrón Decorator

### Por qué Decorator

El enunciado no pide una protección, pide **tres validaciones independientes y combinables**, con la exigencia de poder agregar una cuarta sin tocar las existentes. Eso descarta las alternativas obvias:

| Alternativa | Por qué se descartó |
|---|---|
| Validar dentro de `MessagingClient` | Prohibido: es clase de terceros. |
| Una cadena de `if` en el launcher | Rompe el criterio de diseño 2 (nada de `if`/`switch` por tipo de validación) y obliga a editar el launcher por cada regla nueva. |
| Un único **Proxy** con las tres reglas adentro | Funciona, pero las tres reglas quedarían acopladas en una sola clase: no se pueden activar por separado y agregar la cuarta significa modificar esa clase. Viola abierto/cerrado. |
| **Decorator** ✔ | Cada regla es una clase que envuelve a un `MessageSender` y devuelve un `MessageSender`. Se apilan en el orden que se quiera, se activan solo las que se necesiten, y la cuarta regla es una clase nueva que no obliga a tocar nada. |

Un decorador aquí es a la vez un *protection proxy* por regla: la diferencia con el Proxy clásico es que el tipo envuelto es la interfaz, no la clase concreta, y por eso se pueden encadenar.

Ver diagrama `02-Diagramas/02-decorator-estructura.png`.

### La base abstracta: cómo se evita duplicar lógica

El enunciado exige combinar las validaciones *"sin duplicar lógica entre ellas"*. Si cada validación implementara `sendMessage` por su cuenta, las tres repetirían el mismo esqueleto: comprobar, delegar o loguear.

`MessageSenderDecorator` resuelve esto con un **método plantilla**: `sendMessage` es `final` y contiene la única copia de esa lógica.

```java
@Override
public final void sendMessage(String message) {
    if (isAllowed(message)) {
        wrapped.sendMessage(message);   // delega al siguiente eslabón
    } else {
        logger.log(blockedReason());    // línea exacta del enunciado
    }
}

protected abstract boolean isAllowed(String message);
protected abstract String blockedReason();
```

Cada validación concreta aporta únicamente **dos métodos**: su predicado y su mensaje. Ninguna sabe que las otras existen, ninguna sabe si está al principio o al final de la cadena, y ninguna decide cómo se registra el bloqueo.

### Las tres validaciones

| Clase | Criterio | Línea de bloqueo |
|---|---|---|
| `DangerousContentMessageSender` | La expresión regular `##\{[^}]*\}` no aparece en el mensaje | `Mensaje bloqueado debido a contenido peligroso` |
| `MaxLengthMessageSender` | `length() <= 200` (límite configurable por constructor) | `Mensaje bloqueado por exceder la longitud máxima permitida` |
| `RateLimitMessageSender` | Ventana deslizante: menos de 3 envíos en los últimos 1000 ms | `Mensaje bloqueado por exceso de frecuencia de envío` |

### El armado en el launcher

```java
MessageSender secureSender =
        new DangerousContentMessageSender(
                new MaxLengthMessageSender(
                        new RateLimitMessageSender(originalClient)));
```

El launcher no contiene un solo `if` ni `switch` sobre el tipo de validación: solo compone objetos. A partir de ahí trabaja siempre contra el tipo `MessageSender`, sin enterarse de cuántas validaciones hay ni de cuáles son.

---

## 3. Decisiones tomadas

### 3.1 Orden de la cadena

Contenido y longitud quedan **por fuera** del control de frecuencia. Así, un mensaje rechazado por peligroso o por largo no consume cupo de la ventana de 1 segundo, que es lo razonable: no tiene sentido que un ataque de payloads gigantes agote la cuota de los mensajes legítimos. La prueba `losBloqueadosNoConsumenCupoDeFrecuencia` deja fijo este comportamiento.

Como efecto secundario, cuando dos reglas se violan a la vez gana la más externa y se registra **una sola** línea, tal como pide el enunciado.

### 3.2 `TimeSource` inyectable

`RateLimitMessageSender` no llama a `System.currentTimeMillis()` directamente: recibe un `TimeSource`. En producción se usa `SystemTimeSource`; en las pruebas, un `ManualTimeSource` que avanza a mano.

Sin esto, probar la regla de frecuencia exigiría `Thread.sleep` dentro de los tests: lento y, peor, intermitente en máquinas cargadas. Con el reloj inyectado las cuatro pruebas de frecuencia son deterministas y corren en milisegundos.

### 3.3 `BlockedMessageLogger`

El destino del log también se inyecta. Por defecto es `System.out::println`, para que la línea del enunciado aparezca limpia en la consola —`java.util.logging` la habría mezclado con marca de tiempo, nivel y salida estándar de error—. En las pruebas se sustituye por un registro en memoria, lo que permite afirmar sobre el texto exacto y sobre el número de líneas sin capturar la consola.

### 3.4 La espera en el launcher

El programa de demostración envía primero `"Hola, ¿cómo estás?"`, que es válido y por tanto **consume el primer cupo** de la ventana. Sin más, la ráfaga posterior se habría bloqueado desde su tercer mensaje y no desde el cuarto, aunque la regla estuviera bien implementada.

Para que la evidencia muestre el escenario tal como lo describe el criterio funcional 3, el launcher espera 1100 ms antes del bucle de ráfaga, con el comentario que lo explica ([`GuasappProgramLauncher.java:39`](../src/main/java/edu/unisabana/dyas/patterns/GuasappProgramLauncher.java#L39)). Es una decisión del programa de demostración; no altera la regla, que sigue siendo global y por ventana deslizante.

### 3.5 Pruebas automáticas

Se agregaron JUnit 5 y Surefire al `pom.xml`, que no tenía dependencias ni plugins. Se fijó también la versión de `exec-maven-plugin` para que el comando del README no dependa de resolver una versión en tiempo de ejecución. Se mantuvo el compilador en Java 8, como venía.

**16 pruebas, todas en verde** (`03-Evidencias/salida-pruebas-junit.txt`), incluidos los casos frontera: 200 caracteres pasa y 201 se bloquea; `##` sin llaves no es contenido peligroso; la ventana deslizante descarta correctamente los envíos viejos.

---

## 4. Extensión: agregar una cuarta validación

Supóngase una regla de palabras prohibidas. El trabajo completo es:

```java
public class BlockedWordsMessageSender extends MessageSenderDecorator {

    public static final String BLOCKED_REASON = "Mensaje bloqueado por contener palabras prohibidas";

    private final Set<String> blockedWords;

    public BlockedWordsMessageSender(MessageSender wrapped, Set<String> blockedWords) {
        super(wrapped);
        this.blockedWords = blockedWords;
    }

    @Override
    protected boolean isAllowed(String message) {
        return blockedWords.stream().noneMatch(message.toLowerCase()::contains);
    }

    @Override
    protected String blockedReason() {
        return BLOCKED_REASON;
    }
}
```

…y añadir un nivel al armado de la cadena. **No se modifica** ninguna de las tres validaciones existentes, ni `MessageSenderDecorator`, ni `MessagingClient`. Eso es exactamente el principio abierto/cerrado que pide el criterio de diseño 3.

Ver diagrama `02-Diagramas/04-extension-cuarta-validacion.png`.

---

## 5. Conclusiones

- **Decorator fue la elección correcta** porque el requisito real no era "proteger", sino "componer protecciones independientes". El patrón hace que combinar, reordenar o desactivar reglas sea cuestión de cómo se construyen los objetos, no de editar código.
- **Poner la lógica común en la base abstracta** fue lo que permitió cumplir "sin duplicar lógica": las tres validaciones concretas suman menos de 15 líneas útiles cada una.
- **Programar contra `MessageSender`** dejó al launcher indiferente al número de validaciones. Cambiar la cadena no cambia una sola línea de los envíos.
- **Inyectar el reloj y el log** no fue adorno: sin el primero la regla de frecuencia no se podía probar de forma confiable, y sin el segundo habría que capturar la consola para verificar los textos exactos que el enunciado exige.
- La clase de terceros quedó **intacta**, verificado por `git` (`03-Evidencias/verificacion-messagingclient-intacto.txt`).
