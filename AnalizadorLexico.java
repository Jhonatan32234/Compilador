package new_languaje;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// --- ANALIZADOR LÉXICO Y MAIN ---
class AnalizadorLexico {
    
    // Constantes para los patrones regex
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "//.*|==|!=|>=|<=|[=+*/(){}<>%,;:-]|\"[^\"]*\"|[a-zA-Z_][a-zA-Z0-9_]*|\\d+\\.\\d+|\\d+"
    );
    
    private static final Pattern NUMERO_ENTERO = Pattern.compile("\\d+");
    private static final Pattern NUMERO_DECIMAL = Pattern.compile("\\d+\\.\\d+");
    private static final Pattern IDENTIFICADOR = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    
    // Mapas de palabras clave y símbolos
    private static final Map<String, String> KEYWORDS;
    private static final Map<String, String> SYMBOLS;
    
    // Inicialización estática usando Map.ofEntries para mayor claridad
    static {
        KEYWORDS = Map.ofEntries(
            Map.entry("int", "KW_INT"),
            Map.entry("float", "KW_FLOAT"),
            Map.entry("if", "KW_IF"),
            Map.entry("else", "KW_ELSE"),
            Map.entry("while", "KW_WHILE"),
            Map.entry("do", "KW_DO"),
            Map.entry("switch", "KW_SWITCH"),
            Map.entry("case", "KW_CASE"),
            Map.entry("break", "KW_BREAK"),
            Map.entry("default", "KW_DEFAULT"),
            Map.entry("for", "KW_FOR"),
            Map.entry("return", "KW_RETURN"),
            Map.entry("void", "KW_VOID"),
            Map.entry("print", "KW_PRINT"),
            Map.entry("bool", "KW_BOOL"),
            Map.entry("true", "TRUE_LIT"),
            Map.entry("false", "FALSE_LIT"),
            Map.entry("read", "KW_READ"),
            Map.entry("string", "KW_STRING")
        );
        
        SYMBOLS = Map.ofEntries(
            Map.entry("{", "ABRE_LLAVE"),
            Map.entry("}", "CIERRA_LLAVE"),
            Map.entry("(", "ABRE_PARENTESIS"),
            Map.entry(")", "CIERRA_PARENTESIS"),
            Map.entry(">=", "OP_MAYOR_IGUAL"),
            Map.entry("<=", "OP_MENOR_IGUAL"),
            Map.entry("%", "OP_MODULO"),
            Map.entry("=", "OP_ASIGN"),
            Map.entry("<", "OP_MENOR"),
            Map.entry(">", "OP_MAYOR"),
            Map.entry("-", "OPERA_RESTA"),
            Map.entry("+", "OPERA_SUMA"),
            Map.entry("/", "OPERA_DIVID"),
            Map.entry("*", "OPERA_MULT"),
            Map.entry("==", "OP_IGUAL"),
            Map.entry("!=", "OP_DISTINTO"),
            Map.entry(",", "COMA"),
            Map.entry(";", "PUNTO_COMA"),
            Map.entry(":", "DOS_PUNTOS")
        );
    }
    
    public List<Token> escanear(String contenido) {
        if (contenido == null || contenido.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(contenido);
        
        // Variables para rastrear posición
        int lineaActual = 1;
        int ultimaPosicion = 0;
        
        while (matcher.find()) {
            String tokenValue = matcher.group();
            int startPos = matcher.start();
            
            // Contar saltos de línea antes del token actual
            lineaActual = actualizarLinea(contenido, ultimaPosicion, startPos, lineaActual);
            int columna = calcularColumna(contenido, startPos);
            
            // Actualizar última posición procesada
            ultimaPosicion = startPos + tokenValue.length();
            
            // Ignorar comentarios
            if (tokenValue.startsWith("//")) {
                continue;
            }
            
            procesarToken(tokenValue, tokens, lineaActual, columna);
        }
        
        return tokens;
    }
    
    private int actualizarLinea(String contenido, int ultimaPosicion, int startPos, int lineaActual) {
        if (startPos <= ultimaPosicion) {
            return lineaActual;
        }
        
        String textoPrevio = contenido.substring(ultimaPosicion, startPos);
        for (char c : textoPrevio.toCharArray()) {
            if (c == '\n') {
                lineaActual++;
            }
        }
        return lineaActual;
    }
    
    private int calcularColumna(String contenido, int position) {
        int ultimaNuevaLinea = contenido.lastIndexOf('\n', position);
        if (ultimaNuevaLinea == -1) {
            return position + 1;
        }
        return position - ultimaNuevaLinea;
    }
    
    private void procesarToken(String tokenValue, List<Token> tokens, int linea, int columna) {
        // 1. Palabras clave
        if (KEYWORDS.containsKey(tokenValue)) {
            tokens.add(crearToken(KEYWORDS.get(tokenValue), tokenValue, linea, columna));
            return;
        }
        
        // 2. Símbolos
        if (SYMBOLS.containsKey(tokenValue)) {
            tokens.add(crearToken(SYMBOLS.get(tokenValue), tokenValue, linea, columna));
            return;
        }
        
        // 3. Literales e Identificadores
        String tipo = determinarTipoToken(tokenValue, linea, columna);
        if (tipo != null) {
            tokens.add(crearToken(tipo, tokenValue, linea, columna));
        }
    }
    
    private String determinarTipoToken(String tokenValue, int linea, int columna) {
        if (tokenValue.startsWith("\"")) {
            return "STRING";
        }
        
        if (NUMERO_DECIMAL.matcher(tokenValue).matches()) {
            return "FLOAT_LIT";
        }
        
        if (NUMERO_ENTERO.matcher(tokenValue).matches()) {
            return "NUM";
        }
        
        if (IDENTIFICADOR.matcher(tokenValue).matches()) {
            return "ID";
        }
        
        // Error léxico
        System.err.printf("Error léxico en línea %d, col %d: Token inesperado '%s'%n", 
                         linea, columna, tokenValue);
        return null;
    }
    
    private Token crearToken(String tipo, String valor, int linea, int columna) {
        return new Token(tipo, valor, linea, columna);
    }
}