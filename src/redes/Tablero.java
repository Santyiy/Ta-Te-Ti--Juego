package redes;

/** Logica del Ta-Te-Ti, independiente de la comunicacion por red. */
public final class Tablero {
    // Java inicializa cada char en '\0'; ese valor representa una casilla vacia.
    private final char[] casillas = new char[9];
    private int movimientos;

    public boolean colocar(int posicion, char ficha) {
        if (ficha != 'X' && ficha != 'O') {
            throw new IllegalArgumentException("La ficha debe ser X u O.");
        }
        if (posicion < 1 || posicion > 9 || casillas[posicion - 1] != '\0') {
            return false;
        }
        // El jugador usa 1-9, pero el arreglo usa indices de 0 a 8.
        casillas[posicion - 1] = ficha;
        movimientos++;
        return true;
    }

    public char ganador() {
        // Tres filas, tres columnas y dos diagonales, usando indices del arreglo.
        int[][] lineas = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
        };
        for (int[] linea : lineas) {
            char ficha = casillas[linea[0]];
            if (ficha != '\0' && ficha == casillas[linea[1]] && ficha == casillas[linea[2]]) {
                return ficha;
            }
        }
        return '\0'; // Todavia no hay una linea ganadora.
    }

    public boolean lleno() {
        // Para decidir empate, Partida tambien comprueba que no haya ganador.
        return movimientos == 9;
    }

    public String dibujar() {
        StringBuilder resultado = new StringBuilder();
        for (int fila = 0; fila < 3; fila++) {
            if (fila > 0) resultado.append("\n---+---+---\n");
            for (int columna = 0; columna < 3; columna++) {
                if (columna > 0) resultado.append('|');
                int indice = fila * 3 + columna;
                // Muestra el numero de casilla si esta vacia, o la ficha si esta ocupada.
                char valor = casillas[indice] == '\0' ? (char) ('1' + indice) : casillas[indice];
                resultado.append(' ').append(valor).append(' ');
            }
        }
        return resultado.toString();
    }
}
