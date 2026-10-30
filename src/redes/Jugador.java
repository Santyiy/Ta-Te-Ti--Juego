package redes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Un hilo lee los mensajes de este socket durante toda la conexion. */
final class Jugador implements Runnable {
    private final Socket socket;
    private final Servidor servidor;
    private final PrintWriter salida;
    private String nombre = "Jugador";
    // volatile permite que el hilo del jugador vea la partida asignada por otro hilo.
    private volatile Partida partida;

    Jugador(Socket socket, Servidor servidor) throws IOException {
        this.socket = socket;
        this.servidor = servidor;
        // true activa el envio inmediato al usar println; la red usa UTF-8.
        salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
    }

    String nombre() { return nombre; }
    void asignar(Partida nuevaPartida) { partida = nuevaPartida; }
    Partida partida() { return partida; }

    // Evita intercalar mensajes enviados desde distintos hilos.
    synchronized void enviar(String mensaje) {
        salida.println(mensaje);
    }

    @Override
    public void run() {
        try (BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            // Tiene 30 segundos para enviar el nombre.
            socket.setSoTimeout(30000);
            enviar("NOMBRE: Envia tu nombre (1 a 20 letras, numeros, espacios, _ o -).");
            String recibido = entrada.readLine();
            if (recibido == null) return;
            recibido = recibido.trim();
            if (!recibido.matches("[\\p{L}\\p{N} _-]{1,20}")) {
                enviar("ERROR: Nombre invalido. Volve a conectarte.");
                return;
            }
            nombre = recibido;
            // Durante el juego no hay limite de tiempo para responder.
            socket.setSoTimeout(0);
            enviar("BIENVENIDO: " + nombre + ". Comandos: 1-9, /ayuda, /salir.");
            servidor.emparejar(this);
            String mensaje;
            // Cada linea recibida es un comando o una casilla; null indica cierre.
            while ((mensaje = entrada.readLine()) != null) {
                mensaje = mensaje.trim();
                if (mensaje.equalsIgnoreCase("/salir")) {
                    enviar("SALIENDO: Hasta luego.");
                    break;
                }
                if (mensaje.equalsIgnoreCase("/ayuda")) {
                    enviar("AYUDA: En tu turno, envia una casilla libre del 1 al 9. "
                        + "Forma una linea de tres fichas. /salir cierra la conexion.");
                } else {
                    Partida actual = partida;
                    if (actual == null) enviar("ESPERA: Todavia no hay un rival conectado.");
                    else actual.mover(this, mensaje);
                }
            }
        } catch (IOException e) {
            System.out.println("Conexion cerrada de " + nombre + ": " + e.getMessage());
        } finally {
            // Se ejecuta tanto al salir normalmente como si hay un error de conexion.
            cerrar();
            servidor.retirar(this);
        }
    }

    void cerrar() {
        try { socket.close(); } catch (IOException ignored) { /* Ya esta cerrado. */ }
    }
}
