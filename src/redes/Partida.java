package redes;

/** Controla una partida de dos jugadores con su propio tablero. */
final class Partida {
    private final Jugador[] jugadores;
    private final Tablero tablero = new Tablero();
    private int turno;
    private boolean terminada;

    Partida(Jugador primero, Jugador segundo) {
        jugadores = new Jugador[] {primero, segundo};
    }

    synchronized void iniciar() {
        for (int i = 0; i < jugadores.length; i++) {
            jugadores[i].enviar("PARTIDA: " + jugadores[0].nombre() + " (X) contra "
                + jugadores[1].nombre() + " (O). Tu ficha es " + ficha(i) + ".");
        }
        publicarEstado();
    }

    // synchronized evita que dos hilos modifiquen la partida al mismo tiempo.
    synchronized void mover(Jugador jugador, String entrada) {
        if (terminada) return;
        if (jugadores[turno] != jugador) {
            jugador.enviar("ERROR: No es tu turno.");
            return;
        }
        int posicion;
        try {
            posicion = Integer.parseInt(entrada);
        } catch (NumberFormatException e) {
            jugador.enviar("ERROR: Escribi un numero del 1 al 9. Usa /ayuda para ver los comandos.");
            return;
        }
        if (!tablero.colocar(posicion, ficha(turno))) {
            jugador.enviar("ERROR: Casilla ocupada o fuera de rango. Elegi una casilla libre del 1 al 9.");
            return;
        }
        todos("JUGADA: " + jugador.nombre() + " coloco " + ficha(turno) + " en " + posicion + ".");
        if (tablero.ganador() != '\0') {
            todos(tablero.dibujar());
            finalizar("FIN: Gano " + jugador.nombre() + " (" + ficha(turno) + ").");
        } else if (tablero.lleno()) {
            todos(tablero.dibujar());
            finalizar("FIN: Empate.");
        } else {
            // Alterna el indice del jugador: de 0 a 1 o de 1 a 0.
            turno = 1 - turno;
            publicarEstado();
        }
    }

    // Si la partida seguia en juego, el rival gana por abandono.
    synchronized void desconectar(Jugador jugador) {
        if (!terminada) {
            Jugador restante = jugadores[0] == jugador ? jugadores[1] : jugadores[0];
            finalizar("FIN: " + jugador.nombre() + " se desconecto. Gano " + restante.nombre() + " por abandono.");
        }
    }

    private char ficha(int indice) {
        return indice == 0 ? 'X' : 'O';
    }

    private void publicarEstado() {
        todos(tablero.dibujar());
        todos("TURNO: " + jugadores[turno].nombre() + " (" + ficha(turno) + ").");
        jugadores[turno].enviar("Elegi una casilla del 1 al 9:");
    }

    private void todos(String mensaje) {
        for (Jugador jugador : jugadores) jugador.enviar(mensaje);
    }

    private void finalizar(String mensaje) {
        // Marcarla primero evita volver a finalizar cuando se cierren los sockets.
        terminada = true;
        todos(mensaje);
        for (Jugador jugador : jugadores) jugador.cerrar();
    }
}
