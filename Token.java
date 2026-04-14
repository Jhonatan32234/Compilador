package new_languaje;

class Token {
    String tipo;
    String lexeme;
    int linea;   // <--- Añadir esto
    int columna; // <--- Añadir esto
    public Token(String tipo, String lexeme, int linea, int columna) {
        this.tipo = tipo;
        this.lexeme = lexeme;
        this.linea = linea;
        this.columna = columna;
    }
    @Override
    public String toString() { return String.format("<%s, \"%s\">", tipo, lexeme); }
}

