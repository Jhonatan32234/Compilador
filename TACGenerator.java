package new_languaje;

import java.util.*;

class TACGenerator {
    // Contadores para nombres únicos
    private int tempCount = 0;
    private int labelCount = 0;
    private int strCount = 0;
    
    // Estructuras de datos
    private final List<String> instructions = new ArrayList<>();
    private final Stack<String> breakLabels = new Stack<>();
    private final Map<String, String> stringTable = new LinkedHashMap<>();
    private final Map<String, String> typeTable = new LinkedHashMap<>();
    
    // Constantes para tipos de nodos
    private static final String NODE_ASSIGNMENT = "=";
    private static final String NODE_DECLARATION_PREFIX = "DECL:";
    private static final String NODE_FUNCTION_PREFIX = "FUNC:";
    private static final String NODE_CALL_PREFIX = "CALL:";
    private static final String NODE_PARAM_PREFIX = "PARAM:";
    private static final String NODE_BLOCK = "BLOCK";
    private static final String NODE_IF = "IF";
    private static final String NODE_WHILE = "WHILE";
    private static final String NODE_DO_WHILE = "DO_WHILE";
    private static final String NODE_SWITCH = "SWITCH";
    private static final String NODE_BREAK = "BREAK";
    private static final String NODE_FOR = "FOR";
    private static final String NODE_RETURN = "RETURN";
    private static final String NODE_PRINT_VAR = "PRINT_VAR";
    private static final String NODE_READ = "READ";
    
    // Constantes para tipos de datos
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_INT = "int";
    private static final String TYPE_BOOL = "bool";
    private static final String TYPE_STRING = "string";
    
    // ==================== MÉTODOS PÚBLICOS ====================
    
    public void processNode(Node node) {
        if (node == null) return;
        process(node);
    }
    
    public void printInstructions() {
        System.out.println("\n--- CÓDIGO INTERMEDIO (TAC) ---");
        if (instructions.isEmpty()) {
            System.out.println("(No se generaron instrucciones)");
        } else {
            instructions.forEach(System.out::println);
        }
    }
    
    public List<String> getInstructions() {
        return new ArrayList<>(instructions);
    }
    
    public Map<String, String> getStringTable() {
        return new LinkedHashMap<>(stringTable);
    }
    
    public Map<String, String> getTypeTable() {
        return new LinkedHashMap<>(typeTable);
    }
    
    // ==================== MÉTODOS DE GENERACIÓN DE NOMBRES ====================
    
    private String newStrLabel() { 
        return "str" + (strCount++); 
    }
    
    private String newTemp() { 
        return "T" + (tempCount++); 
    }
    
    private String newLabel() { 
        return "L" + (labelCount++); 
    }
    
    // ==================== MÉTODO PRINCIPAL DE PROCESAMIENTO ====================
    
    private String process(Node node) {
        if (node == null) return "";
        
        // Usar el patrón Chain of Responsibility
        return processByNodeType(node);
    }
    
    /**
     * Procesa el nodo según su tipo usando un enfoque de despacho
     */
    private String processByNodeType(Node node) {
        // Asignación
        if (node.value.equals(NODE_ASSIGNMENT)) {
            return processAssignment(node);
        }
        
        // Declaración
        if (node.value.startsWith(NODE_DECLARATION_PREFIX)) {
            processDeclaration(node);
            return "";
        }
        
        // Función
        if (node.value.startsWith(NODE_FUNCTION_PREFIX)) {
            processFunction(node);
            return "";
        }
        
        // Llamada a función
        if (node.value.startsWith(NODE_CALL_PREFIX)) {
            return processFunctionCall(node);
        }
        
        // Usar switch para valores exactos
        switch (node.value) {
            case NODE_BLOCK:
                processBlock(node);
                return "";
            case NODE_IF:
                processIf(node);
                return "";
            case NODE_WHILE:
                processWhile(node);
                return "";
            case NODE_DO_WHILE:
                processDoWhile(node);
                return "";
            case NODE_SWITCH:
                processSwitch(node);
                return "";
            case NODE_BREAK:
                processBreak(node);
                return "";
            case NODE_FOR:
                processFor(node);
                return "";
            case NODE_RETURN:
                processReturn(node);
                return "";
            case NODE_PRINT_VAR:
                processPrint(node);
                return "";
            case NODE_READ:
                processRead(node);
                return "";
            default:
                return processValueNode(node);
        }
    }
    
    // ==================== PROCESAMIENTO DE NODOS ESPECÍFICOS ====================
    
    private String processAssignment(Node node) {
        if (node.left == null) return "";
        
        String rightSide = process(node.right);
        instructions.add(String.format("%s = %s", node.left.value, rightSide));
        
        // Si es parte de una expresión, devolver el valor asignado
        return node.left.value;
    }
    
    private void processDeclaration(Node node) {
        if (node.left == null) return;
        
        String tipo = extractTypeFromNodeValue(node.value);
        String variable = node.left.value;
        
        instructions.add(String.format("alloc %s, %s", variable, tipo));
        typeTable.put(variable, tipo);
    }
    
    private void processFunction(Node node) {
        String funcName = extractNameFromNodeValue(node.value);
        
        instructions.add("begin_func " + funcName);
        
        // Registrar parámetros
        if (node.parameters != null) {
            for (Node param : node.parameters) {
                instructions.add("param " + param.left.value);
            }
        }
        
        // Procesar cuerpo de la función
        process(node.right);
        
        instructions.add("end_func " + funcName);
    }
    
    private String processFunctionCall(Node node) {
        String funcName = extractNameFromNodeValue(node.value);
        int argCount = 0;
        
        if (node.parameters != null) {
            // Empujar los argumentos a la pila en ORDEN INVERSO
            // para que la función llamada los extraiga en el orden correcto (param1, param2, etc.).
            for (int i = node.parameters.size() - 1; i >= 0; i--) {
                Node arg = node.parameters.get(i);
                String argVal = process(arg);
                instructions.add("arg " + argVal);
                argCount++;
            }
        }
        
        String temp = newTemp();
        instructions.add(String.format("%s = call %s, %d", temp, funcName, argCount));
        return temp;
    }
    
    private void processIf(Node node) {
        String condition = process(node.left);
        String endLabel = newLabel();
        String elseLabel = (node.elseNode != null) ? newLabel() : null;
        
        // Salto condicional
        String targetLabel = (elseLabel != null) ? elseLabel : endLabel;
        instructions.add(String.format("ifFalse %s goto %s", condition, targetLabel));
        
        // Bloque THEN
        process(node.right);
        
        // Bloque ELSE si existe
        if (elseLabel != null) {
            instructions.add("goto " + endLabel);
            instructions.add(elseLabel + ":");
            process(node.elseNode);
        }
        
        instructions.add(endLabel + ":");
    }
    
    private void processWhile(Node node) {
        String startLabel = newLabel();
        String endLabel = newLabel();
        
        breakLabels.push(endLabel);
        
        instructions.add(startLabel + ":");
        String condition = process(node.left);
        instructions.add(String.format("ifFalse %s goto %s", condition, endLabel));
        
        process(node.right);
        instructions.add("goto " + startLabel);
        instructions.add(endLabel + ":");
        
        breakLabels.pop();
    }
    
    private void processDoWhile(Node node) {
        String startLabel = newLabel();
        String endLabel = newLabel();
        
        breakLabels.push(endLabel);
        
        instructions.add(startLabel + ":");
        process(node.right);
        
        String condition = process(node.left);
        instructions.add(String.format("ifFalse %s goto %s", condition, endLabel));
        instructions.add("goto " + startLabel);
        instructions.add(endLabel + ":");
        
        breakLabels.pop();
    }
    
    private void processFor(Node node) {
        String startLabel = newLabel();
        String endLabel = newLabel();
        
        breakLabels.push(endLabel);
        
        // Inicialización
        process(node.parameters.get(0));
        
        instructions.add(startLabel + ":");
        
        // Condición
        String condition = process(node.parameters.get(1));
        instructions.add(String.format("ifFalse %s goto %s", condition, endLabel));
        
        // Cuerpo
        process(node.right);
        
        // Incremento
        process(node.parameters.get(2));
        
        instructions.add("goto " + startLabel);
        instructions.add(endLabel + ":");
        
        breakLabels.pop();
    }
    
    private void processSwitch(Node node) {
        String switchExpr = process(node.left);
        String endSwitchLabel = newLabel();
        String defaultLabel = (node.defaultNode != null) ? newLabel() : endSwitchLabel;
        
        breakLabels.push(endSwitchLabel);
        
        List<String> caseBodyLabels = generateCaseLabels(node.parameters.size());
        
        // Generar comparaciones para cada case
        for (int i = 0; i < node.parameters.size(); i++) {
            Node caseNode = node.parameters.get(i);
            String caseValue = process(caseNode.left);
            String condition = newTemp();
            String nextCheckLabel = newLabel();
            
            instructions.add(String.format("%s = %s == %s", condition, switchExpr, caseValue));
            instructions.add(String.format("ifFalse %s goto %s", condition, nextCheckLabel));
            instructions.add("goto " + caseBodyLabels.get(i));
            instructions.add(nextCheckLabel + ":");
        }
        
        instructions.add("goto " + defaultLabel);
        
        // Procesar cuerpos de los cases
        for (int i = 0; i < node.parameters.size(); i++) {
            instructions.add(caseBodyLabels.get(i) + ":");
            process(node.parameters.get(i).right);
        }
        
        // Procesar default si existe
        if (node.defaultNode != null) {
            instructions.add(defaultLabel + ":");
            process(node.defaultNode);
        }
        
        instructions.add(endSwitchLabel + ":");
        breakLabels.pop();
    }
    
    private void processBreak(Node node) {
        if (breakLabels.isEmpty()) {
            throw new IllegalStateException("TACGenerator: 'break' sin bucle/switch.");
        }
        instructions.add("goto " + breakLabels.peek());
    }
    
    private void processReturn(Node node) {
        String value = process(node.left);
        if (value.isEmpty()) {
            instructions.add("return");
        } else {
            instructions.add("return " + value);
        }
    }
    
    private void processPrint(Node node) {
        if (node.parameters == null) return;
        
        for (Node arg : node.parameters) {
            if (arg.value.startsWith("\"")) {
                processStringLiteral(arg);
            } else {
                processExpressionPrint(arg);
            }
        }
        instructions.add("print_nl");
    }
    
    private void processStringLiteral(Node arg) {
        String cleanContent = arg.value.substring(1, arg.value.length() - 1);
        String label = newStrLabel();
        stringTable.put(label, cleanContent);
        instructions.add("print_str " + label);
    }
    
    private void processExpressionPrint(Node arg) {
        String val = process(arg);
        String printType = getExpressionType(val);
        
        // Mapear el tipo interno al sufijo de instrucción TAC
        String suffix = "int";
        if (TYPE_FLOAT.equals(printType)) suffix = "float";
        else if (TYPE_STRING.equals(printType)) suffix = "string";
        
        instructions.add("print_" + suffix + " " + val);
    }
    
    private String getExpressionType(String val) {
        if (val == null) return TYPE_INT;
        if (val.contains(".")) return TYPE_FLOAT;
        if (val.startsWith("\"")) return TYPE_STRING;
        return typeTable.getOrDefault(val, TYPE_INT);
    }
    
    
    private void processRead(Node node) {
        if (node.left == null) return;
        
        String varName = node.left.value;
        String type = typeTable.get(varName);
        String readType;
        if (TYPE_FLOAT.equals(type)) readType = "float";
        else if (TYPE_STRING.equals(type)) readType = "str";
        else readType = "int";
        
        instructions.add("read_" + readType + " " + varName);
    }
    
    private void processBlock(Node node) {
        Node current = node.left;
        while (current != null) {
            process(current);
            current = current.next;
        }
    }
    
    // ==================== PROCESAMIENTO DE NODOS DE VALOR ====================
    
    private String processValueNode(Node node) {
        // Nodos hoja (literales, identificadores)
        if (isLeafNode(node)) {
            return node.value;
        }
        
        // Operaciones binarias
        return processBinaryOperation(node);
    }
    
    private String processBinaryOperation(Node node) {
        String left = process(node.left);
        String right = process(node.right);
        
        // Si alguno de los lados está vacío, retornar el otro
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;
        
        // Generar temporal para el resultado
        String temp = newTemp();
        
        // Inferencia de tipos: si algún operando es float, el temporal es float
        String typeL = getExpressionType(left);
        String typeR = getExpressionType(right);
        if (TYPE_FLOAT.equals(typeL) || TYPE_FLOAT.equals(typeR)) {
            typeTable.put(temp, TYPE_FLOAT);
        }
        
        instructions.add(String.format("%s = %s %s %s", temp, left, node.value, right));
        return temp;
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private boolean isLeafNode(Node node) {
        return node.left == null && node.right == null;
    }
    
    private String extractTypeFromNodeValue(String nodeValue) {
        return nodeValue.split(":")[1];
    }
    
    private String extractNameFromNodeValue(String nodeValue) {
        return nodeValue.split(":")[1];
    }
    
    private List<String> generateCaseLabels(int count) {
        List<String> labels = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            labels.add(newLabel());
        }
        return labels;
    }
    
    // ==================== MÉTODOS DE UTILIDAD (opcionales) ====================
    
    /**
     * Limpia todos los contadores y estructuras (útil para reiniciar el generador)
     */
    public void reset() {
        tempCount = 0;
        labelCount = 0;
        strCount = 0;
        instructions.clear();
        breakLabels.clear();
        stringTable.clear();
        typeTable.clear();
    }
    
    /**
     * Obtiene el número total de instrucciones generadas
     */
    public int getInstructionCount() {
        return instructions.size();
    }
}