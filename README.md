# Ta-Te-Ti multijugador por consola

Proyecto en Java con arquitectura cliente-servidor, sockets TCP e hilos. Dos jugadores
se conectan desde sus consolas; el servidor administra el tablero, los turnos y el resultado.
Puede haber varias partidas independientes al mismo tiempo, con hasta 50 jugadores conectados.
No usa interfaz grafica, dependencias externas ni herramientas de metodologia agil.

## Requisitos y compilacion

- JDK 11 o posterior (incluye `java` y `javac` en el PATH).
- Una terminal para el servidor y una por cada cliente.
- Para jugar en computadoras distintas, deben poder comunicarse por red.

Desde la carpeta del proyecto, en PowerShell:

```powershell
.\compilar.ps1
```

Si PowerShell tiene deshabilitada la ejecucion de scripts, compilar directamente
con este comando (no hace falta modificar la politica del sistema):

```powershell
javac -encoding UTF-8 --release 11 -d out src/redes/*.java pruebas/redes/*.java
```

## Jugar en una sola computadora

En una terminal, iniciar el servidor:

```powershell
java -cp out redes.Servidor
```

En otras **dos terminales**, ejecutar una vez por terminal:

```powershell
java -cp out redes.Cliente
```

Cada jugador escribe su nombre y presiona Enter. El primero recibe la ficha X y espera
al segundo, que recibe O. X comienza. Los nombres pueden repetirse; las fichas identifican
a cada jugador dentro de la partida.

```text
 1 | 2 | 3
---+---+---
 4 | 5 | 6
---+---+---
 7 | 8 | 9
```

En tu turno, escribi el numero de una casilla libre y presiona Enter. El tablero se actualiza
en ambas consolas. Gana quien forme una fila, columna o diagonal de tres fichas.
Si se completan las nueve casillas sin ganador, hay empate. Un movimiento invalido no pierde el turno.

- `/ayuda`: muestra las instrucciones.
- `/salir`: abandona y cierra la conexion. Si ya habia partida, el rival gana por abandono.
- Para volver a jugar, ejecutar de nuevo el cliente. Cada conexion participa en una sola partida.
- Para detener el servidor, presionar Ctrl+C en su terminal.

## Jugar desde computadoras distintas

1. Elegir una computadora como servidor e iniciar `redes.Servidor` alli.
2. En esa computadora, ejecutar `ipconfig` y buscar la direccion IPv4 del adaptador de la red
   compartida, por ejemplo `192.168.1.20`.
3. Copiar el proyecto y compilarlo en las computadoras de los jugadores (o copiar `out`
   ya compilado y usar Java 11 o posterior).
4. Cada jugador se conecta a la IP del servidor:

```powershell
java -cp out redes.Cliente 192.168.1.20 5000
```

El servidor escucha en todas las interfaces. Si Windows solicita permiso de firewall para Java,
permitirlo en la red privada usada para la practica. Las redes de invitados pueden impedir
la comunicacion entre dispositivos. `localhost` solo sirve para conectarse a la misma computadora.
Para esta practica se recomienda una LAN: no hace falta publicar el servidor en Internet.

El puerto predeterminado es 5000. Para cambiarlo, usar el mismo numero en ambos programas:

```powershell
java -cp out redes.Servidor 6000
java -cp out redes.Cliente localhost 6000
```

## Arquitectura y concurrencia

| Archivo | Responsabilidad |
| --- | --- |
| `src/redes/Servidor.java` | Aceptar sockets, administrar cupos y emparejar jugadores en orden de llegada. |
| `src/redes/Jugador.java` | Leer el nombre y los comandos de una conexion en un hilo propio. |
| `src/redes/Partida.java` | Mantener una partida, validar turnos y comunicar jugadas y resultados. |
| `src/redes/Tablero.java` | Colocar fichas, dibujar el tablero y detectar ganador o empate. |
| `src/redes/Cliente.java` | Enviar el teclado y recibir mensajes del servidor simultaneamente. |
| `pruebas/redes/Pruebas.java` | Verificar reglas y partidas mediante conexiones TCP reales. |

El hilo principal del servidor espera conexiones con `ServerSocket.accept()`. Cada socket
aceptado se atiende mediante un `Runnable` ejecutado por un `ExecutorService`. Asi, un jugador
que espera un mensaje no bloquea la lectura de los demas.

La cola de espera se protege con `synchronized`. Cada `Partida` tambien sincroniza sus metodos:
dos hilos no pueden modificar su tablero al mismo tiempo. Todos los movimientos se validan
en el servidor; el cliente solo transmite texto. Los jugadores de distintas partidas usan
tableros y monitores distintos. Un `Semaphore` limita las conexiones a 50.

El cliente usa dos hilos: el principal lee del socket y el secundario lee del teclado.
Esto permite ver inmediatamente las jugadas del rival sin tener que presionar Enter.
El hilo de teclado es daemon para que no impida salir cuando el servidor termina la partida.

## Protocolo de comunicacion

Se usa texto UTF-8 sobre TCP, con un mensaje por linea (un tablero ocupa varias lineas).
`PrintWriter` con autoflush envia cada linea inmediatamente y `BufferedReader.readLine()`
la recibe completa: TCP por si solo no conserva fronteras entre mensajes.

1. El servidor solicita `NOMBRE:`. El cliente envia una linea con el nombre.
2. El servidor responde `BIENVENIDO:` y luego `ESPERA:` o `PARTIDA:`.
3. El cliente envia una casilla (`1` a `9`), `/ayuda` o `/salir`.
4. El servidor informa `JUGADA:`, el tablero y `TURNO:`, o explica el rechazo con `ERROR:`.
5. Al ganar, empatar o abandonar, el servidor envia `FIN:` y cierra ambos sockets.

El nombre debe llegar en 30 segundos. Si se cierra una conexion, el hilo correspondiente
retira al jugador de la espera o termina su partida por abandono. Una perdida abrupta de red
puede tardar en detectarse segun TCP y el sistema operativo: esta version no implementa
latidos de conexion ni limite de tiempo para una jugada. Tampoco guarda resultados entre ejecuciones.

## Pruebas

Despues de compilar:

```powershell
java -cp out redes.Pruebas
```

Las pruebas lanzan su propio servidor en un puerto local libre y lo detienen al terminar.
Verifican las ocho lineas ganadoras para ambas fichas, limites, casillas ocupadas, empate,
espera de rival, turnos incorrectos, entradas invalidas, victoria por red, partidas simultaneas,
salida voluntaria, cierre abrupto del socket y rechazo de nombres invalidos.

Como comprobacion manual adicional, jugar desde dos equipos de la misma red y cerrar
una de las consolas durante una partida. Esta prueba permite comprobar la configuracion
real de IP y firewall del aula.

## Relacion con la consigna

- **Cliente-servidor:** dos programas ejecutables con responsabilidades separadas.
- **Sockets:** `ServerSocket` y `Socket` para comunicar computadoras mediante TCP.
- **Hilos:** conexiones concurrentes en el servidor y lectura simultanea de red y consola en el cliente.
- **Estado centralizado:** el servidor decide si cada jugada es valida y quien gana.
- **Consola:** toda la interaccion se realiza por texto.
- **Trabajo individual:** proyecto pequeno, sin frameworks, con clases separadas para poder estudiar y explicar cada parte.
