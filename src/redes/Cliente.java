package redes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** La consola envia en un hilo; el hilo principal recibe sin esperar al teclado. */
public final class Cliente {
    public static void main(String[] args) {
        if (args.length > 2) {
            System.err.println("Uso: java -cp out redes.Cliente [host] [puerto]");
            return;
        }
        String host = args.length > 0 ? args[0] : "localhost";
        try {
            int puerto = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
            if (puerto < 1 || puerto > 65535) throw new IllegalArgumentException();
            try (Socket socket = new Socket()) {
                // Intenta conectarse al servidor durante un maximo de 5 segundos.
                socket.connect(new InetSocketAddress(host, puerto), 5000);
                System.out.println("Conectado a " + host + ":" + puerto + ".");
                BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                // El teclado tiene su hilo para no frenar los mensajes del servidor.
                Thread teclado = new Thread(() -> enviarTeclado(socket, salida), "teclado");
                // Si termina la partida, el proceso puede finalizar aunque el teclado este esperando.
                teclado.setDaemon(true);
                teclado.start();
                String mensaje;
                // El hilo principal muestra lo que llega hasta que el servidor cierra.
                while ((mensaje = entrada.readLine()) != null) System.out.println(mensaje);
                System.out.println("Conexion finalizada. Ejecuta el cliente otra vez para volver a jugar.");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("El puerto debe ser un numero entre 1 y 65535.");
        } catch (IOException e) {
            System.err.println("No se pudo mantener la conexion: " + e.getMessage());
            System.err.println("Verifica que el servidor este activo y que el host y el puerto sean correctos.");
        }
    }

    private static void enviarTeclado(Socket socket, PrintWriter salida) {
        BufferedReader consola = new BufferedReader(new InputStreamReader(System.in));
        try {
            String linea;
            while ((linea = consola.readLine()) != null) {
                salida.println(linea);
                if (linea.trim().equalsIgnoreCase("/salir")) {
                    // Deja de enviar, pero permite recibir la despedida del servidor.
                    socket.shutdownOutput();
                    return;
                }
            }
            socket.shutdownOutput();
        } catch (IOException e) {
            // El servidor puede cerrar el socket mientras se espera una entrada.
        }
    }
}
