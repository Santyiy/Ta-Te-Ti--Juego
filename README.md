# Ta-Te-Ti por consola

Juego en Java para dos jugadores, con servidor, clientes, sockets TCP e hilos.
El servidor controla las jugadas y puede manejar varias partidas al mismo tiempo.

## Compilar

Se necesita JDK 11 o superior. Desde la carpeta del proyecto:

```powershell
javac -encoding UTF-8 --release 11 -d out src/redes/*.java pruebas/redes/*.java
```

## Jugar

Primero iniciar el servidor en una terminal:

```powershell
java -cp out redes.Servidor
```

En otras dos terminales, abrir un cliente en cada una:

```powershell
java -cp out redes.Cliente
```

Cada jugador ingresa su nombre. El primero usa X y el segundo O.
En tu turno, ingresa una casilla libre del 1 al 9, numeradas de izquierda a derecha
y de arriba hacia abajo. Gana quien forme una linea de tres fichas.
Usa `/ayuda` para ver las instrucciones o `/salir` para abandonar.
Para otra partida, ejecuta de nuevo el cliente.

Para jugar desde otra computadora de la misma red, reemplaza la IP del ejemplo
por la del servidor (se puede consultar con `ipconfig`):

```powershell
java -cp out redes.Cliente 192.168.1.20 5000
```

El puerto por defecto es 5000. Si no conecta, revisar la IP y el firewall.
Para cerrar el servidor, presionar Ctrl+C.

## Archivos

- `src/redes`: codigo del cliente, servidor y logica del juego.
- `pruebas/redes`: pruebas de las reglas y las conexiones.
- `out/redes`: archivos compilados; se generan al compilar.
- `compilar.ps1`: alternativa para compilar desde PowerShell, si permite scripts.