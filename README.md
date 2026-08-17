# GuasappMessenger

Simulador de una herramienta de mensajería. El módulo `MessagingClient` (provisto por un tercero, **no modificable**) tiene varias debilidades que deben "blindarse" desde afuera, usando patrones de diseño estructurales:

1. **Contenido peligroso**: cualquier mensaje que contenga el patrón `##{...}` (por ejemplo `##{./exec(rm /* -r)}`) le da acceso a terceros al equipo donde se ejecute. Debe bloquearse.
2. **Longitud excesiva**: un mensaje de más de 200 caracteres no debe procesarse (protección básica contra abuso/flood de payloads grandes).
3. **Frecuencia de envío**: no se deben permitir más de 3 mensajes en menos de 1 segundo; el 4º mensaje en adelante dentro de esa ventana debe bloquearse.

Cada mensaje bloqueado debe registrar por log exactamente una de estas líneas, según la regla que lo bloqueó:

* `"Mensaje bloqueado debido a contenido peligroso"`
* `"Mensaje bloqueado por exceder la longitud máxima permitida"`
* `"Mensaje bloqueado por exceso de frecuencia de envío"`

Un mensaje que no viole ninguna regla debe entregarse con normalidad (es decir, `MessagingClient` sí debe recibir la llamada).

Las tres validaciones son independientes entre sí: la solución debe permitir combinarlas (o activar solo algunas) sin duplicar lógica entre ellas, y sin que ninguna conozca los detalles de las otras.

Recuerde los comandos para la ejecución del programa

Para compilar

```bash
mvn compile
```

Para ejecutar la aplicación:

```bash
mvn exec:java  -Dexec.mainClass=edu.unisabana.dyas.patterns.GuasappProgramLauncher
```

## Criterios de evaluación

* Diseño.
	1. `MessagingClient` (clase de terceros) no debe modificarse.
	2. La solución debe apoyarse en la interfaz `MessageSender`, sin introducir `if`/`switch` sobre el tipo de validación en el código del launcher.
	3. Debe ser posible agregar una cuarta validación en el futuro sin modificar las tres existentes ni `MessagingClient` (principio abierto/cerrado).
* Funcionalidad.
	1. El mensaje `"##{./exec(rm /* -r)}"` debe bloquearse con su log correspondiente.
	2. Un mensaje de más de 200 caracteres debe bloquearse con su log correspondiente.
	3. A partir del 4º mensaje enviado en menos de 1 segundo, deben bloquearse con su log correspondiente.
	4. Los mensajes que no violan ninguna regla se entregan con normalidad (se ve el `"Sending message: ..."` de `MessagingClient`).
