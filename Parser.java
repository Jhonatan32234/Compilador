package new_languaje;

import java.util.*;

// --- ANALIZADOR SINTÁCTICO (PARSER) ---
class Parser {
    private final List<Token> tokens;
    private int posicion = 0;
    
    // Constantes para mensajes de error comunes
    private static final String ERROR_ESPERABA_TIPO = "Se esperaba un tipo de dato";
    private static final String ERROR_ESPERABA_ID = "Se esperaba un identificador";
    private static final String ERROR_ESPERABA_PARENTESIS_APERTURA = "Se esperaba '('";
    private static final String ERROR_ESPERABA_PARENTESIS_CIERRE = "Se esperaba ')'";
    private static final String ERROR_ESPERABA_LLAVE_APERTURA = "Se esperaba '{'";
    private static final String ERROR_ESPERABA_LLAVE_CIERRE = "Se esperaba '}'";
    private static final String ERROR_ESPERABA_PUNTO_COMA = "Se esperaba ';'";
    private static final String ERROR_ESPERABA_DOS_PUNTOS = "Se esperaba ':'";

    public Parser(List<Token> tokens) { 
        this.tokens = Objects.requireNonNull(tokens, "La lista de tokens no puede ser nula");
    }
    
    private boolean hayTokens() {
        return posicion < tokens.size();
    }
    
    private Token tokenActual() {
        return hayTokens() ? tokens.get(posicion) : null;
    }
    
    private String tipoActual() {
        Token t = tokenActual();
        return t != null ? t.tipo : "";
    }
    
    private boolean esTipoActual(String tipo) {
        return tipoActual().equals(tipo);
    }

    private Token match(String... tiposEsperados) {
        if (!hayTokens()) return null;
        
        Token token = tokenActual();
        for (String tipo : tiposEsperados) {
            if (token.tipo.equals(tipo)) {
                posicion++;
                return token;
            }
        }
        return null;
    }

    private Token consume(String tipoEsperado, String mensajeError) throws Exception {
        Token token = match(tipoEsperado);
        if (token != null) return token;
        
        String detalle = hayTokens() 
            ? String.format(" cerca de '%s'", tokenActual().lexeme) 
            : " al final del archivo";
        
        throw new Exception(String.format("Error sintáctico: %s%s", mensajeError, detalle));
    }
    
    private boolean esFunctionLookahead() {
        int posicionGuardada = posicion;
        try {
            if (match("KW_INT", "KW_FLOAT", "KW_VOID") != null) {
                if (match("ID") != null && esTipoActual("ABRE_PARENTESIS")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignorar excepciones durante la verificación
        } finally {
            posicion = posicionGuardada;
        }
        return false;
    }
    
    private boolean esDeclaracion() {
        return esTipoActual("KW_INT") || esTipoActual("KW_FLOAT") || esTipoActual("KW_BOOL") || esTipoActual("KW_STRING");
    }
    
    private boolean esPrint() {
        return esTipoActual("KW_PRINT");
    }
    
    private boolean esRead() {
        return esTipoActual("KW_READ");
    }
    
    private boolean esReturn() {
        return esTipoActual("KW_RETURN");
    }
    
    private boolean esBreak() {
        return esTipoActual("KW_BREAK");
    }
    
    private boolean esIf() {
        return esTipoActual("KW_IF");
    }
    
    private boolean esWhile() {
        return esTipoActual("KW_WHILE");
    }
    
    private boolean esDo() {
        return esTipoActual("KW_DO");
    }
    
    private boolean esFor() {
        return esTipoActual("KW_FOR");
    }
    
    private boolean esSwitch() {
        return esTipoActual("KW_SWITCH");
    }
    
    private boolean esCase() {
        return esTipoActual("KW_CASE");
    }
    
    private boolean esDefault() {
        return esTipoActual("KW_DEFAULT");
    }
    
    private boolean esAsignacion() {
        int posicionGuardada = posicion;
        try {
            return match("ID") != null && match("OP_ASIGN") != null;
        } catch (Exception e) {
            return false;
        } finally {
            posicion = posicionGuardada;
        }
    }

    public List<Node> parseProgram() throws Exception {
        List<Node> statements = new ArrayList<>();
        
        while (hayTokens() && !esTipoActual("CIERRA_LLAVE")) {
            if (esFunctionLookahead()) {
                statements.add(parseFunction());
            } else {
                Node stmt = parseStatement();
                if (stmt != null) {
                    statements.add(stmt);
                } else {
                    throw new Exception(String.format(
                        "Error sintáctico: No se puede procesar el token '%s'", 
                        tokenActual().lexeme
                    ));
                }
            }
        }
        return statements;
    }

    private Node parseRead() throws Exception {
        Token startToken = consume("KW_READ", "Se esperaba 'read'");
        consume("ABRE_PARENTESIS", ERROR_ESPERABA_PARENTESIS_APERTURA);
        Token idToken = consume("ID", ERROR_ESPERABA_ID);
        consume("CIERRA_PARENTESIS", ERROR_ESPERABA_PARENTESIS_CIERRE);

        Node readNode = new Node("READ", startToken.linea, startToken.columna);
        readNode.left = new Node(idToken.lexeme, idToken.linea, idToken.columna);
        return readNode;
    }

    private Node parseFunction() throws Exception {
        match("KW_INT", "KW_FLOAT", "KW_VOID");
        Token name = match("ID");
        if (name == null) {
            throw new Exception("Error: Se esperaba nombre de función.");
        }
        
        match("ABRE_PARENTESIS");
        Node funcNode = new Node("FUNC:" + name.lexeme, name.linea, name.columna);
        funcNode.parameters = parseParameters();
        match("CIERRA_PARENTESIS");
        funcNode.right = parseBlock(funcNode.linea, funcNode.columna);
        
        return funcNode;
    }

    private List<Node> parseParameters() throws Exception {
        List<Node> params = new ArrayList<>();
        
        if (esTipoActual("CIERRA_PARENTESIS")) {
            return params;
        }
    
        do {
            Token type = match("KW_INT", "KW_FLOAT");
            if (type == null) {
                throw new Exception(ERROR_ESPERABA_TIPO);
            }
            
            Token id = match("ID");
            if (id == null) {
                throw new Exception(ERROR_ESPERABA_ID);
            }
            
            Node paramNode = new Node("PARAM:" + type.lexeme, type.linea, type.columna);
            paramNode.left = new Node(id.lexeme, id.linea, id.columna);
            params.add(paramNode);
        } while (match("COMA") != null); 
    
        return params;
    }

    private Node parseStatement() throws Exception {
        // Estructura IF
        if (esIf()) {
            return parseIfStatement();
        }

        // Estructura WHILE
        if (esWhile()) {
            return parseWhileStatement();
        }

        // Estructura DO-WHILE
        if (esDo()) {
            return parseDoWhileStatement();
        }

        // Estructura FOR
        if (esFor()) {
            return parseForStatement();
        }

        // Estructura SWITCH
        if (esSwitch()) {
            return parseSwitchStatement();
        }

        // Sentencia BREAK
        if (esBreak()) {
            return parseBreakStatement();
        }

        // Sentencia READ
        if (esRead()) {
            return parseRead();
        }

        // Sentencia RETURN
        if (esReturn()) {
            return parseReturnStatement();
        }

        // Declaraciones
        if (esDeclaracion()) {
            return parseDeclaracion();
        }

        // Sentencia PRINT
        if (esPrint()) {
            return parsePrintStatement();
        }

        // Asignaciones
        if (esAsignacion()) {
            return parseAsignacion();
        }

        return parseExpression();
    }
    
    private Node parseIfStatement() throws Exception {
        Token ifToken = consume("KW_IF", "Se esperaba 'if'");
        Node node = new Node("IF", ifToken.linea, ifToken.columna);
        
        consume("ABRE_PARENTESIS", ERROR_ESPERABA_PARENTESIS_APERTURA);
        node.left = parseExpression();
        consume("CIERRA_PARENTESIS", ERROR_ESPERABA_PARENTESIS_CIERRE);
        node.right = parseBlock(ifToken.linea, ifToken.columna);
        
        Token elseToken = match("KW_ELSE");
        if (elseToken != null) {
            node.elseNode = parseBlock(elseToken.linea, elseToken.columna);
        }
        
        return node;
    }
    
    private Node parseWhileStatement() throws Exception {
        Token whileToken = consume("KW_WHILE", "Se esperaba 'while'");
        Node node = new Node("WHILE", whileToken.linea, whileToken.columna);
        
        consume("ABRE_PARENTESIS", ERROR_ESPERABA_PARENTESIS_APERTURA);
        node.left = parseExpression();
        consume("CIERRA_PARENTESIS", ERROR_ESPERABA_PARENTESIS_CIERRE);
        node.right = parseBlock(whileToken.linea, whileToken.columna);
        
        return node;
    }
    
    private Node parseDoWhileStatement() throws Exception {
        Token doToken = consume("KW_DO", "Se esperaba 'do'");
        Node node = new Node("DO_WHILE", doToken.linea, doToken.columna);
        
        node.right = parseBlock(doToken.linea, doToken.columna);
        consume("KW_WHILE", "Se esperaba 'while' después del bloque 'do'");
        consume("ABRE_PARENTESIS", ERROR_ESPERABA_PARENTESIS_APERTURA);
        node.left = parseExpression();
        consume("CIERRA_PARENTESIS", ERROR_ESPERABA_PARENTESIS_CIERRE);
        
        return node;
    }
    
    private Node parseForStatement() throws Exception {
        Token forToken = consume("KW_FOR", "Se esperaba 'for'");
        Node node = new Node("FOR", forToken.linea, forToken.columna);
        
        consume("ABRE_PARENTESIS", ERROR_ESPERABA_PARENTESIS_APERTURA);
        node.parameters.add(parseStatement());
        consume("PUNTO_COMA", ERROR_ESPERABA_PUNTO_COMA);
        node.parameters.add(parseExpression());
        consume("PUNTO_COMA", ERROR_ESPERABA_PUNTO_COMA);
        node.parameters.add(parseStatement());
        consume("CIERRA_PARENTESIS", ERROR_ESPERABA_PARENTESIS_CIERRE);
        node.right = parseBlock(forToken.linea, forToken.columna);
        
        return node;
    }
    
    private Node parseSwitchStatement() throws Exception {
        Token switchToken = consume("KW_SWITCH", "Se esperaba 'switch'");
        Node node = new Node("SWITCH", switchToken.linea, switchToken.columna);
        
        consume("ABRE_PARENTESIS", ERROR_ESPERABA_PARENTESIS_APERTURA);
        node.left = parseExpression();
        consume("CIERRA_PARENTESIS", ERROR_ESPERABA_PARENTESIS_CIERRE);
        consume("ABRE_LLAVE", ERROR_ESPERABA_LLAVE_APERTURA);
        
        while (esCase() || esDefault()) {
            if (esCase()) {
                node.parameters.add(parseCase());
            } else {
                if (node.defaultNode != null) {
                    throw new Exception("Error sintáctico: Múltiples 'default' en switch.");
                }
                node.defaultNode = parseDefault();
            }
        }
        
        consume("CIERRA_LLAVE", ERROR_ESPERABA_LLAVE_CIERRE);
        return node;
    }
    
    private Node parseBreakStatement() throws Exception {
        Token breakToken = consume("KW_BREAK", "Se esperaba 'break'");
        consume("PUNTO_COMA", ERROR_ESPERABA_PUNTO_COMA);
        return new Node("BREAK", breakToken.linea, breakToken.columna);
    }
    
    private Node parseReturnStatement() throws Exception {
        Token retToken = consume("KW_RETURN", "Se esperaba 'return'");
        Node retNode = new Node("RETURN", retToken.linea, retToken.columna);
        retNode.left = parseExpression();
        return retNode;
    }
    
    private Node parseDeclaracion() throws Exception {
        Token t = match("KW_INT", "KW_FLOAT", "KW_BOOL", "KW_STRING");
        if (t == null) {
            throw new Exception(ERROR_ESPERABA_TIPO);
        }
        
        Token id = match("ID");
        if (id == null) {
            throw new Exception(ERROR_ESPERABA_ID);
        }
        
        Node declNode = new Node("DECL:" + t.lexeme, t.linea, t.columna);
        declNode.left = new Node(id.lexeme, id.linea, id.columna);
        return declNode;
    }
    
    private Node parsePrintStatement() throws Exception {
        Token printToken = consume("KW_PRINT", "Se esperaba 'print'");
        consume("ABRE_PARENTESIS", ERROR_ESPERABA_PARENTESIS_APERTURA);
        
        Node printNode = new Node("PRINT_VAR", printToken.linea, printToken.columna);
        List<Node> args = new ArrayList<>();
        
        do {
            Token strToken = match("STRING");
            if (strToken != null) {
                args.add(new Node(strToken.lexeme, strToken.linea, strToken.columna));
            } else {
                args.add(parseExpression());
            }
        } while (match("COMA") != null);
        
        printNode.parameters = args;
        consume("CIERRA_PARENTESIS", ERROR_ESPERABA_PARENTESIS_CIERRE);
        return printNode;
    }
    
    private Node parseAsignacion() throws Exception {
        Token id = consume("ID", ERROR_ESPERABA_ID);
        consume("OP_ASIGN", "Se esperaba '='");
        
        Node node = new Node("=", id.linea, id.columna);
        node.left = new Node(id.lexeme, id.linea, id.columna);
        node.right = parseExpression();
        
        return node;
    }

    private Node parseBlock(int linea, int columna) throws Exception {
        consume("ABRE_LLAVE", ERROR_ESPERABA_LLAVE_APERTURA);
        Node blockNode = new Node("BLOCK", linea, columna);
        
        List<Node> statements = parseProgram();
        blockNode.left = encadenarSentencias(statements);
        
        consume("CIERRA_LLAVE", ERROR_ESPERABA_LLAVE_CIERRE);
        return blockNode;
    }
    
    private Node encadenarSentencias(List<Node> statements) {
        if (statements.isEmpty()) return null;
        
        Node first = statements.get(0);
        Node current = first;
        for (int i = 1; i < statements.size(); i++) {
            current.next = statements.get(i);
            current = current.next;
        }
        return first;
    }

    private Node parseCase() throws Exception {
        Token caseToken = consume("KW_CASE", "Se esperaba 'case'");
        Node caseNode = new Node("CASE", caseToken.linea, caseToken.columna);
        
        caseNode.left = parseFactor();
        consume("DOS_PUNTOS", ERROR_ESPERABA_DOS_PUNTOS);
        
        Node blockNode = parseCaseBlock(caseToken.linea, caseToken.columna);
        caseNode.right = blockNode;
        
        return caseNode;
    }

    private Node parseDefault() throws Exception {
        Token defToken = consume("KW_DEFAULT", "Se esperaba 'default'");
        consume("DOS_PUNTOS", ERROR_ESPERABA_DOS_PUNTOS);
        return parseCaseBlock(defToken.linea, defToken.columna);
    }
    
    private Node parseCaseBlock(int linea, int columna) throws Exception {
        Node blockNode = new Node("BLOCK", linea, columna);
        List<Node> statements = new ArrayList<>();
        
        while (!esCase() && !esDefault() && !esTipoActual("CIERRA_LLAVE")) {
            statements.add(parseStatement());
        }
        
        blockNode.left = encadenarSentencias(statements);
        return blockNode;
    }

    public Node parseExpression() throws Exception {
        Node node = parseTerm();
        
        String[] operadores = {"OPERA_SUMA", "OPERA_RESTA", "OP_MENOR", "OP_MAYOR", 
                               "OP_IGUAL", "OP_DISTINTO", "OP_MAYOR_IGUAL", 
                               "OP_MENOR_IGUAL", "OP_MODULO"};
        
        Token t;
        while ((t = match(operadores)) != null) {
            Node newNode = new Node(t.lexeme, t.linea, t.columna);
            newNode.left = node;
            newNode.right = parseTerm();
            node = newNode;
        }
        
        return node;
    }

    private Node parseTerm() throws Exception {
        Node node = parseFactor();
        
        Token t;
        while ((t = match("OPERA_MULT", "OPERA_DIVID")) != null) {
            Node newNode = new Node(t.lexeme, t.linea, t.columna);
            newNode.left = node;
            newNode.right = parseFactor();
            node = newNode;
        }
        
        return node;
    }

    private Node parseFactor() throws Exception {
        Token token;
        
        // Números enteros
        if ((token = match("NUM")) != null) {
            return new Node(token.lexeme, token.linea, token.columna);
        }
        
        // Números flotantes
        if ((token = match("FLOAT_LIT")) != null) {
            return new Node(token.lexeme, token.linea, token.columna);
        }
        
        // Literales booleanos
        if ((token = match("TRUE_LIT")) != null) {
            return new Node("1", token.linea, token.columna);
        }
        
        if ((token = match("FALSE_LIT")) != null) {
            return new Node("0", token.linea, token.columna);
        }
        
        // Identificadores
        if ((token = match("ID")) != null) {
            String name = token.lexeme;
            int linea = token.linea;
            int columna = token.columna;
            
            if (match("ABRE_PARENTESIS") != null) {
                Node callNode = new Node("CALL:" + name, linea, columna);
                callNode.parameters = parseArguments();
                match("CIERRA_PARENTESIS");
                return callNode;
            }
            
            return new Node(name, linea, columna);
        }
        
        throw new Exception("Se esperaba un valor o llamada a función");
    }
    
    private List<Node> parseArguments() throws Exception {
        List<Node> args = new ArrayList<>();
        if (esTipoActual("CIERRA_PARENTESIS")) return args;
        
        do {
            args.add(parseExpression());
        } while (match("COMA") != null);
        
        return args;
    }
}