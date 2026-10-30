package redes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/** Pruebas sin librerias externas: tablero y servidor real con clientes TCP. */
public final class Pruebas {
    private static int comprobaciones;

    public static void main(String[] args) throws Exception {
        probarTablero();
        probarRed();
        System.out.println("OK: " + comprobaciones + " comprobaciones completadas.");
    }

    private static void verificar(boolean condicion, String descripcion) {
        comprobaciones++;
        if (!condicion) throw new AssertionError(descripcion);
    }

    private static void probarTablero() {
        int[][] lineas = {
            {1, 2, 3}, {4, 5, 6}, {7, 8, 9}, {1, 4, 7},
            {2, 5, 8}, {3, 6, 9}, {1, 5, 9}, {3, 5, 7}
        };
        for (char ficha : new char[] {'X', 'O'}) {
            for (int[] linea : lineas) {
                Tablero tablero = new Tablero();
                for (int posicion : linea) verificar(tablero.colocar(posicion, ficha), "Colocacion valida");
                verificar(tablero.ganador() == ficha, "Detectar todas las lineas ganadoras");
            }
        }
        Tablero tablero = new Tablero();
        verificar(!tablero.colocar(0, 'X') && !tablero.colocar(10, 'X'), "Limites del tablero");
        verificar(tablero.colocar(1, 'X') && !tablero.colocar(1, 'O'), "Casilla ocupada");
        verificar(tablero.dibujar().contains("X"), "Dibujar fichas");
        int[] orden = {2, 3, 5, 4, 6, 8, 7, 9};
        char ficha = 'O';
        for (int posicion : orden) {
            tablero.colocar(posicion, ficha);
            ficha = ficha == 'X' ? 'O' : 'X';
        }
        verificar(tablero.lleno() && tablero.ganador() == '\0', "Empate sin ganador");
    }

    private static void probarRed() throws Exception {
        int puerto;
        try (ServerSocket reserva = new ServerSocket(0)) { puerto = reserva.getLocalPort(); }
        String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        Process servidor = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
            "redes.Servidor", Integer.toString(puerto))
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            esperarServidor(puerto);
            // Una conexion de sondeo no debe quedar esperando rival.
            try (Conexion a = new Conexion(puerto, "Ana")) {
                a.esperar("ESPERA:");
                a.enviar("1");
                a.esperar("ESPERA:");
                try (Conexion b = new Conexion(puerto, "Beto")) {
                    a.esperar("Tu ficha es X");
                    b.esperar("Tu ficha es O");
                    a.esperar("Elegi una casilla");
                    b.esperar("TURNO: Ana");
                    b.enviar("5"); b.esperar("ERROR: No es tu turno");
                    a.enviar("texto"); a.esperar("ERROR: Escribi un numero");
                    a.enviar("10"); a.esperar("ERROR: Casilla ocupada o fuera de rango");
                    // Segunda partida activa en paralelo: sus jugadas no afectan a Ana y Beto.
                    try (Conexion c = new Conexion(puerto, "Cami")) {
                        c.esperar("ESPERA:");
                        try (Conexion d = new Conexion(puerto, "Dani")) {
                            c.esperar("Tu ficha es X"); d.esperar("Tu ficha es O");
                            c.enviar("9"); d.esperar("TURNO: Dani");
                            jugar(a, b, 1, "Beto");
                            b.enviar("1"); b.esperar("ERROR: Casilla ocupada o fuera de rango");
                            jugar(b, a, 4, "Ana");
                            jugar(a, b, 2, "Beto");
                            jugar(b, a, 5, "Ana");
                            a.enviar("3");
                            a.esperar("FIN: Gano Ana (X)"); b.esperar("FIN: Gano Ana (X)");
                            verificar(a.entrada.readLine() == null, "Cerrar conexion al terminar");
                            c.enviar("/salir");
                            d.esperar("Gano Dani por abandono");
                        }
                    }
                }
            }
            try (Conexion a = new Conexion(puerto, "Eva")) {
                a.esperar("ESPERA:");
                try (Conexion b = new Conexion(puerto, "Fede")) {
                    a.esperar("Tu ficha es X"); b.esperar("Tu ficha es O");
                    int[] movimientos = {1, 2, 3, 5, 4, 6, 8, 7};
                    for (int i = 0; i < movimientos.length; i++) {
                        jugar(i % 2 == 0 ? a : b, i % 2 == 0 ? b : a,
                            movimientos[i], i % 2 == 0 ? "Fede" : "Eva");
                    }
                    a.enviar("9"); a.esperar("FIN: Empate"); b.esperar("FIN: Empate");
                }
            }
            try (Conexion a = new Conexion(puerto, "Gabi")) {
                a.esperar("ESPERA:");
                try (Conexion b = new Conexion(puerto, "Hugo")) {
                    a.esperar("Tu ficha es X"); b.esperar("Tu ficha es O");
                    b.close();
                    a.esperar("Gano Gabi por abandono");
                }
            }
            try (Conexion invalida = new Conexion(puerto, "!!!")) {
                invalida.esperar("ERROR: Nombre invalido");
            }
        } finally {
            servidor.destroy();
            if (!servidor.waitFor(5, TimeUnit.SECONDS)) {
                servidor.destroyForcibly();
                servidor.waitFor();
            }
        }
    }

    private static void jugar(Conexion actual, Conexion rival, int posicion, String siguiente) throws IOException {
        actual.enviar(Integer.toString(posicion));
        actual.esperar("TURNO: " + siguiente);
        rival.esperar("TURNO: " + siguiente);
    }

    private static void esperarServidor(int puerto) throws Exception {
        for (int intento = 0; intento < 100; intento++) {
            try (Socket sondeo = new Socket("localhost", puerto)) { return; }
            catch (IOException e) { Thread.sleep(50); }
        }
        throw new AssertionError("El servidor no inicio a tiempo");
    }

    private static final class Conexion implements AutoCloseable {
        final Socket socket;
        final BufferedReader entrada;
        final PrintWriter salida;

        Conexion(int puerto, String nombre) throws IOException {
            socket = new Socket("localhost", puerto);
            socket.setSoTimeout(4000);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            esperar("NOMBRE:");
            enviar(nombre);
        }

        void enviar(String mensaje) { salida.println(mensaje); }

        void esperar(String texto) throws IOException {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.contains(texto)) { verificar(true, texto); return; }
            }
            throw new AssertionError("Conexion cerrada antes de recibir: " + texto);
        }

        @Override public void close() throws IOException { socket.close(); }
    }
}
