package new_languaje;

import java.util.*;

class SemanticAnalyzer {
    private SymbolTable currentScope;
    private int loopOrSwitchDepth = 0;
    
    // Constantes para tipos de nodos
    private static final String NODE_FUNCTION_PREFIX = "FUNC:";
    private static final String NODE_DECLARATION_PREFIX = "DECL:";
    private static final String NODE_PARAM_PREFIX = "PARAM:";
    private static final String NODE_CALL_PREFIX = "CALL:";
    
    // Constantes para valores de nodos
    private static final String NODE_WHILE = "WHILE";
    private static final String NODE_DO_WHILE = "DO_WHILE";
    private static final String NODE_FOR = "FOR";
    private static final String NODE_SWITCH = "SWITCH";
    private static final String NODE_CASE = "CASE";
    private static final String NODE_BREAK = "BREAK";
    private static final String NODE_RETURN = "RETURN";
    private static final String NODE_IF = "IF";
    private static final String NODE_BLOCK = "BLOCK";
    private static final String NODE_PRINT_VAR = "PRINT_VAR";
    private static final String NODE_PRINT = "PRINT";
    private static final String NODE_READ = "READ";
    private static final String NODE_ASSIGNMENT = "=";
    
    // Patrón para identificar identificadores válidos
    private static final String IDENTIFIER_PATTERN = "[a-zA-Z_][a-zA-Z0-9_]*";
    
    // Operadores válidos
    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
        "+", "-", "*", "/", "=", "<", ">", "==", "!=", "<=", ">=", "%"
    ));
    
    public SemanticAnalyzer() {
        this.currentScope = new SymbolTable(null);
    }
    
    // Método público principal
    public void validate(Node node) throws Exception {
        if (node == null) return;
        
        // Usar el patrón Chain of Responsibility con validadores especializados
        if (!validateByNodeType(node)) {
            // Si no es un nodo especial, validar como variable
            validateVariableUsage(node);
            
            // Continuar con la validación recursiva
            validate(node.left);
            validate(node.right);
        }
    }
    
    /**
     * Valida el nodo según su tipo usando validadores específicos
     * @return true si el nodo fue procesado por algún validador especializado
     */
    private boolean validateByNodeType(Node node) throws Exception {
        // Función
        if (node.value.startsWith(NODE_FUNCTION_PREFIX)) {
            validateFunctionNode(node);
            return true;
        }
        
        // Declaración
        if (node.value.startsWith(NODE_DECLARATION_PREFIX)) {
            validateDeclarationNode(node);
            return true;
        }
        
        // Llamada a función
        if (node.value.startsWith(NODE_CALL_PREFIX)) {
            validateFunctionCallNode(node);
            return true;
        }
        
        // Usar switch para los valores exactos
        switch (node.value) {
            case NODE_WHILE:
                validateWhileNode(node);
                return true;
            case NODE_DO_WHILE:
                validateDoWhileNode(node);
                return true;
            case NODE_FOR:
                validateForNode(node);
                return true;
            case NODE_SWITCH:
                validateSwitchNode(node);
                return true;
            case NODE_CASE:
                validateCaseNode(node);
                return true;
            case NODE_BREAK:
                validateBreakNode(node);
                return true;
            case NODE_RETURN:
                validateReturnNode(node);
                return true;
            case NODE_IF:
                validateIfNode(node);
                return true;
            case NODE_BLOCK:
                validateBlockNode(node);
                return true;
            case NODE_PRINT_VAR:
                validatePrintVarNode(node);
                return true;
            case NODE_PRINT:
                validatePrintNode(node);
                return true;
            case NODE_READ:
                validateReadNode(node);
                return true;
            case NODE_ASSIGNMENT:
                validateAssignmentNode(node);
                return true;
            default:
                return false;
        }
    }
    
    // ==================== VALIDADORES ESPECÍFICOS ====================
    
    private void validateFunctionNode(Node node) throws Exception {
        // Crear un nuevo ámbito para la función
        enterNewScope();
        
        // Declarar los parámetros dentro del nuevo ámbito
        declareParameters(node.parameters);
        
        // Validar el cuerpo de la función
        validate(node.right);
        
        // Salir del ámbito
        exitCurrentScope();
    }
    
    private void validateFunctionCallNode(Node node) throws Exception {
        // Validar que la función existe (se puede implementar más adelante)
        // Por ahora solo validamos los argumentos
        if (node.parameters != null) {
            for (Node arg : node.parameters) {
                validate(arg);
            }
        }
    }
    
    private void validateDeclarationNode(Node node) throws Exception {
        String tipo = extractTypeFromNodeValue(node.value);
        if (node.left != null) {
            declareVariable(node.left.value, tipo, node.left.linea, node.left.columna);
        }
    }
    
    private void validateWhileNode(Node node) throws Exception {
        loopOrSwitchDepth++;
        validate(node.left);   // Condición
        validate(node.right);  // Cuerpo
        loopOrSwitchDepth--;
    }
    
    private void validateDoWhileNode(Node node) throws Exception {
        loopOrSwitchDepth++;
        validate(node.left);   // Condición
        validate(node.right);  // Cuerpo
        loopOrSwitchDepth--;
    }
    
    private void validateForNode(Node node) throws Exception {
        loopOrSwitchDepth++;
        enterNewScope(); // Scope para variables del for
        
        // Validar inicialización, condición y paso
        for (Node part : node.parameters) {
            validate(part);
        }
        
        // Validar el cuerpo
        validate(node.right);
        
        exitCurrentScope();
        loopOrSwitchDepth--;
    }
    
    private void validateSwitchNode(Node node) throws Exception {
        loopOrSwitchDepth++;
        
        validate(node.left); // Expresión del switch
        
        // Validar todos los casos
        for (Node caseNode : node.parameters) {
            validate(caseNode);
        }
        
        // Validar default si existe
        if (node.defaultNode != null) {
            validate(node.defaultNode);
        }
        
        loopOrSwitchDepth--;
    }
    
    private void validateCaseNode(Node node) throws Exception {
        validate(node.left);  // Valor literal del case
        validate(node.right); // Bloque de sentencias
    }
    
    private void validateBreakNode(Node node) throws Exception {
        if (loopOrSwitchDepth == 0) {
            throw new Exception(String.format(
                "Error semántico [%d:%d]: 'break' solo puede usarse dentro de un bucle o switch.",
                node.linea, node.columna
            ));
        }
    }
    
    private void validateReturnNode(Node node) throws Exception {
        validate(node.left); // Expresión de retorno
    }
    
    private void validateIfNode(Node node) throws Exception {
        enterNewScope();
        validate(node.left);  // Condición
        validate(node.right); // Bloque then
        
        if (node.elseNode != null) {
            validate(node.elseNode);
        }
        exitCurrentScope();
    }
    
    private void validateBlockNode(Node node) throws Exception {
        Node current = node.left;
        while (current != null) {
            validate(current);
            current = current.next;
        }
    }
    
    private void validatePrintVarNode(Node node) throws Exception {
        if (node.parameters != null) {
            for (Node arg : node.parameters) {
                validate(arg);
            }
        }
    }
    
    private void validatePrintNode(Node node) throws Exception {
        validate(node.left);
    }
    
    private void validateReadNode(Node node) throws Exception {
        if (node.left != null) {
            validate(node.left);
        }
    }
    
    private void validateAssignmentNode(Node node) throws Exception {
        // Validar que la variable existe
        if (node.left != null && node.left.value.matches(IDENTIFIER_PATTERN)) {
            validateVariableUsage(node.left);
        }
        
        // Validar la expresión asignada
        validate(node.right);
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Declara una variable en el ámbito actual con manejo de errores
     */
    private void declareVariable(String name, String type, int linea, int columna) throws Exception {
        try {
            currentScope.declare(name, type);
        } catch (Exception e) {
            throw new Exception(String.format(
                "Error semántico [%d:%d]: %s", 
                linea, columna, e.getMessage()
            ));
        }
    }
    
    /**
     * Declara los parámetros de una función
     */
    private void declareParameters(List<Node> parameters) throws Exception {
        if (parameters == null) return;
        
        for (Node param : parameters) {
            String type = extractTypeFromNodeValue(param.value);
            declareVariable(param.left.value, type, param.left.linea, param.left.columna);
        }
    }
    
    /**
     * Valida que una variable esté definida
     */
    private void validateVariableUsage(Node node) throws Exception {
        if (isIdentifier(node.value) && !isOperator(node.value)) {
            try {
                currentScope.lookup(node.value);
            } catch (Exception e) {
                throw new Exception(String.format(
                    "Error semántico [%d:%d]: La variable '%s' no está definida",
                    node.linea, node.columna, node.value
                ));
            }
        }
    }
    
    /**
     * Extrae el tipo de un valor de nodo con prefijo (ej: "DECL:int" -> "int")
     */
    private String extractTypeFromNodeValue(String nodeValue) {
        return nodeValue.split(":")[1];
    }
    
    /**
     * Verifica si un valor es un identificador válido
     */
    private boolean isIdentifier(String value) {
        return value != null && value.matches(IDENTIFIER_PATTERN);
    }
    
    /**
     * Verifica si un valor es un operador
     */
    private boolean isOperator(String value) {
        return OPERATORS.contains(value);
    }
    
    /**
     * Entra en un nuevo ámbito
     */
    private void enterNewScope() {
        currentScope = new SymbolTable(currentScope);
    }
    
    /**
     * Sale del ámbito actual
     */
    private void exitCurrentScope() {
        currentScope = currentScope.getParent();
    }
    
    // ==================== MÉTODOS PÚBLICOS DE UTILIDAD ====================
    
    /**
     * Declara una variable en el ámbito actual (API pública)
     */
    public void declare(String name, String type) throws Exception {
        currentScope.declare(name, type);
    }
    
    /**
     * Obtiene el ámbito actual (para debugging)
     */
    public SymbolTable getCurrentScope() {
        return currentScope;
    }
}