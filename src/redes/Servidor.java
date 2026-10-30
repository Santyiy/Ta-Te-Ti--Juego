package redes;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/** Acepta conexiones TCP y empareja jugadores por orden de llegada. */
public final class Servidor {
    private final Queue<Jugador> espera = new ArrayDeque<>();
    private final Set<Jugador> conectados = ConcurrentHashMap.newKeySet();
    // Cada conexion ocupa un cupo hasta que el jugador se retira.
    private final Semaphore cupos = new Semaphore(50);
    private final ExecutorService hilos = Executors.newCachedThreadPool();

    // Solo un hilo a la vez puede sacar o agregar jugadores a la cola.
    synchronized void emparejar(Jugador jugador) {
        Jugador rival = espera.poll();
        if (rival == null) {
            espera.add(jugador);
            jugador.enviar("ESPERA: Buscando otro jugador...");
        } else {
            Partida partida = new Partida(rival, jugador);
            // La partida debe terminar de iniciarse antes de recibir jugadas.
            synchronized (partida) {
                rival.asignar(partida);
                jugador.asignar(partida);
                partida.iniciar();
            }
        }
    }

    // Limpia la espera o avisa del abandono y devuelve el cupo de la conexion.
    void retirar(Jugador jugador) {
        synchronized (this) { espera.remove(jugador); }
        Partida partida = jugador.partida();
        if (partida != null) partida.desconectar(jugador);
        if (conectados.remove(jugador)) cupos.release();
    }

    private void ejecutar(int puerto) throws IOException {
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            // Al cerrar con Ctrl+C, tambien se cierran los sockets de los clientes.
            Thread cierre = new Thread(() -> {
                try { servidor.close(); } catch (IOException ignored) { }
                for (Jugador jugador : conectados) jugador.cerrar();
                hilos.shutdownNow();
            }, "cierre-servidor");
            Runtime.getRuntime().addShutdownHook(cierre);
            System.out.println("Servidor de Ta-Te-Ti escuchando en el puerto " + puerto + ".");
            System.out.println("Presiona Ctrl+C para detenerlo. Limite: 50 jugadores conectados.");
            while (!servidor.isClosed()) {
                // accept() espera hasta que se conecte un cliente.
                Socket socket = servidor.accept();
                if (!cupos.tryAcquire()) {
                    try (Socket rechazado = socket) {
                        new PrintWriter(rechazado.getOutputStream(), true, StandardCharsets.UTF_8)
                            .println("ERROR: Servidor lleno. Intenta mas tarde.");
                    }
                    continue;
                }
                try {
                    Jugador jugador = new Jugador(socket, this);
                    conectados.add(jugador);
                    // Atiende al jugador en otro hilo y vuelve a aceptar conexiones.
                    hilos.execute(jugador);
                } catch (IOException e) {
                    cupos.release();
                    socket.close();
                    System.err.println("No se pudo preparar una conexion: " + e.getMessage());
                }
            }
        } finally {
            for (Jugador jugador : conectados) jugador.cerrar();
            hilos.shutdownNow();
        }
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Uso: java -cp out redes.Servidor [puerto]");
            return;
        }
        try {
            int puerto = args.length == 0 ? 5000 : Integer.parseInt(args[0]);
            if (puerto < 1 || puerto > 65535) throw new IllegalArgumentException();
            new Servidor().ejecutar(puerto);
        } catch (IllegalArgumentException e) {
            System.err.println("El puerto debe ser un numero entre 1 y 65535.");
        } catch (SocketException e) {
            System.err.println("Servidor detenido o puerto no disponible: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
        }
    }
}
