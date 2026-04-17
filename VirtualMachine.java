package new_languaje;

import java.util.*;

class VirtualMachine {
    // Estado de la VM
    private byte[] bytecode;
    private int pc;  // Program Counter
    private final Stack<Integer> stack = new Stack<>();
    private final Stack<Integer> returnAddressStack = new Stack<>();
    private final Stack<Map<Integer, Object>> callStack = new Stack<>();
    private Map<Integer, Object> memory = new HashMap<>();
    private Scanner scanner;
    
    // Constantes para opcodes (deben coincidir con BytecodeGenerator)
    private static final byte OP_PUSH_INT = 0x01;
    private static final byte OP_PUSH_FLOAT = 0x02;
    private static final byte OP_LOAD = 0x03;
    private static final byte OP_STORE = 0x04;
    private static final byte OP_ADD = 0x10;
    private static final byte OP_SUB = 0x11;
    private static final byte OP_MUL = 0x12;
    private static final byte OP_DIV = 0x13;
    private static final byte OP_MOD = 0x14;
    private static final byte OP_FADD = 0x15;
    private static final byte OP_FSUB = 0x16;
    private static final byte OP_FMUL = 0x17;
    private static final byte OP_FDIV = 0x18;
    private static final byte OP_EQ = 0x20;
    private static final byte OP_NEQ = 0x21;
    private static final byte OP_LT = 0x22;
    private static final byte OP_GT = 0x23;
    private static final byte OP_LE = 0x24;
    private static final byte OP_GE = 0x25;
    private static final byte OP_JMP = 0x30;
    private static final byte OP_JMP_IF_FALSE = 0x31;
    private static final byte OP_CALL = 0x40;
    private static final byte OP_RET = 0x41;
    private static final byte OP_PRINT_INT = 0x50;
    private static final byte OP_PRINT_FLOAT = 0x51;
    private static final byte OP_PRINT_STR = 0x52;
    private static final byte OP_PRINT_NL = 0x53;
    private static final byte OP_READ_INT = 0x60;
    private static final byte OP_READ_FLOAT = 0x61;
    private static final byte OP_HALT = (byte) 0xFF;
    
    // Constantes para mensajes
    private static final String PROMPT_INPUT = "? ";
    private static final String NULL_STRING = "null";
    private static final String SEGFAULT_MSG = "Segmentation Fault: Salto inválido a %d";
    private static final String STACK_UNDERFLOW_MSG = "Stack Underflow: No hay dirección de retorno";
    private static final String UNKNOWN_OPCODE_MSG = "Opcode desconocido: 0x%02X";
    private static final String STRING_LABEL_PREFIX = "str";
    
    // Constantes para operaciones aritméticas y lógicas
    private static final int TRUE_VALUE = 1;
    private static final int FALSE_VALUE = 0;
    
    public VirtualMachine(byte[] bytecode, Map<String, String> stringTable) {
        this.bytecode = Objects.requireNonNull(bytecode, "Bytecode no puede ser nulo");
        this.pc = 0;
        loadStrings(stringTable);
    }
    
    // ==================== MÉTODOS PÚBLICOS ====================
    
    public void execute() {
        try {
            while (pc < bytecode.length) {
                byte opcode = bytecode[pc++];
                executeInstruction(opcode);
            }
        } catch (Exception e) {
            System.err.printf("Error en VM en PC=%d: %s%n", pc - 1, e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== CARGA DE RECURSOS ====================
    
    private void loadStrings(Map<String, String> stringTable) {
        if (stringTable == null) return;
        
        for (Map.Entry<String, String> entry : stringTable.entrySet()) {
            String label = entry.getKey();
            if (label.startsWith(STRING_LABEL_PREFIX)) {
                int id = Integer.parseInt(label.substring(3));
                // Usamos clave negativa para diferenciar strings de variables
                memory.put(-id - 1, entry.getValue());
            }
        }
    }
    
    // ==================== EJECUCIÓN DE INSTRUCCIONES ====================
    
    private void executeInstruction(byte opcode) {
        switch (opcode) {
            case OP_PUSH_INT:
                executePushInt();
                break;
            case OP_PUSH_FLOAT:
                executePushFloat();
                break;
            case OP_LOAD:
                executeLoad();
                break;
            case OP_STORE:
                executeStore();
                break;
            case OP_ADD:
                executeBinaryOperation(this::addOperation);
                break;
            case OP_SUB:
                executeBinaryOperation(this::subtractOperation);
                break;
            case OP_MUL:
                executeBinaryOperation(this::multiplyOperation);
                break;
            case OP_DIV:
                executeBinaryOperation(this::divideOperation);
                break;
            case OP_MOD:
                executeBinaryOperation(this::moduloOperation);
                break;
            case OP_FADD:
                executeFloatBinaryOperation((l, r) -> l + r);
                break;
            case OP_FSUB:
                executeFloatBinaryOperation((l, r) -> l - r);
                break;
            case OP_FMUL:
                executeFloatBinaryOperation((l, r) -> l * r);
                break;
            case OP_FDIV:
                executeFloatBinaryOperation((l, r) -> l / r);
                break;
            case OP_EQ:
                executeBinaryOperation(this::equalOperation);
                break;
            case OP_NEQ:
                executeBinaryOperation(this::notEqualOperation);
                break;
            case OP_LT:
                executeBinaryOperation(this::lessThanOperation);
                break;
            case OP_GT:
                executeBinaryOperation(this::greaterThanOperation);
                break;
            case OP_LE:
                executeBinaryOperation(this::lessOrEqualOperation);
                break;
            case OP_GE:
                executeBinaryOperation(this::greaterOrEqualOperation);
                break;
            case OP_JMP:
                executeJump();
                break;
            case OP_JMP_IF_FALSE:
                executeConditionalJump();
                break;
            case OP_CALL:
                executeCall();
                break;
            case OP_RET:
                executeReturn();
                break;
            case OP_PRINT_INT:
                executePrintInt();
                break;
            case OP_PRINT_FLOAT:
                executePrintFloat();
                break;
            case OP_PRINT_STR:
                executePrintString();
                break;
            case OP_PRINT_NL:
                System.out.println();
                break;
            case OP_READ_INT:
                executeReadInt();
                break;
            case OP_READ_FLOAT:
                executeReadFloat();
                break;
            case OP_HALT:
                pc = bytecode.length;
                break;
            default:
                throw new RuntimeException(String.format(UNKNOWN_OPCODE_MSG, opcode & 0xFF));
        }
    }
    
    // ==================== OPERACIONES DE PILA Y MEMORIA ====================
    
    private void executePushInt() {
        int value = readInt();
        stack.push(value);
    }
    
    private void executePushFloat() {
        float value = readFloat();
        stack.push(Float.floatToIntBits(value));
    }
    
    private void executeLoad() {
        int address = readInt();
        Object value = memory.get(address);
        
        if (value instanceof Integer) {
            stack.push((Integer) value);
        } else if (value instanceof Float) {
            stack.push(Float.floatToIntBits((Float) value));
        } else if (value == null) {
            stack.push(0); // Valor por defecto si no existe
        }
    }
    
    private void executeStore() {
        int address = readInt();
        int value = stack.pop();
        memory.put(address, value);
    }
    
    // ==================== OPERACIONES ARITMÉTICAS Y LÓGICAS ====================
    
    @FunctionalInterface
    private interface BinaryOperation {
        int apply(int left, int right);
    }
    
    private void executeBinaryOperation(BinaryOperation operation) {
        int right = stack.pop();
        int left = stack.pop();
        stack.push(operation.apply(left, right));
    }

    @FunctionalInterface
    private interface FloatBinaryOperation {
        float apply(float left, float right);
    }

    private void executeFloatBinaryOperation(FloatBinaryOperation operation) {
        float right = Float.intBitsToFloat(stack.pop());
        float left = Float.intBitsToFloat(stack.pop());
        stack.push(Float.floatToIntBits(operation.apply(left, right)));
    }
    
    private int addOperation(int left, int right) {
        return left + right;
    }
    
    private int subtractOperation(int left, int right) {
        return left - right;
    }
    
    private int multiplyOperation(int left, int right) {
        return left * right;
    }
    
    private int divideOperation(int left, int right) {
        if (right == 0) {
            throw new ArithmeticException("División por cero");
        }
        return left / right;
    }
    
    private int moduloOperation(int left, int right) {
        if (right == 0) {
            throw new ArithmeticException("Módulo por cero");
        }
        return left % right;
    }
    
    private int equalOperation(int left, int right) {
        return (left == right) ? TRUE_VALUE : FALSE_VALUE;
    }
    
    private int notEqualOperation(int left, int right) {
        return (left != right) ? TRUE_VALUE : FALSE_VALUE;
    }
    
    private int lessThanOperation(int left, int right) {
        return (left < right) ? TRUE_VALUE : FALSE_VALUE;
    }
    
    private int greaterThanOperation(int left, int right) {
        return (left > right) ? TRUE_VALUE : FALSE_VALUE;
    }
    
    private int lessOrEqualOperation(int left, int right) {
        return (left <= right) ? TRUE_VALUE : FALSE_VALUE;
    }
    
    private int greaterOrEqualOperation(int left, int right) {
        return (left >= right) ? TRUE_VALUE : FALSE_VALUE;
    }
    
    // ==================== OPERACIONES DE CONTROL DE FLUJO ====================
    
    private void executeJump() {
        int target = readInt();
        validateJumpTarget(target);
        pc = target;
    }
    
    private void executeConditionalJump() {
        int condition = stack.pop();
        int target = readInt();
        validateJumpTarget(target);
        
        if (condition == FALSE_VALUE) {
            pc = target;
        }
    }
    
    private void executeCall() {
        int functionAddress = readInt();
        validateJumpTarget(functionAddress);
        
        // Guardar dirección de retorno
        returnAddressStack.push(pc);
        
        // Guardar contexto actual
        callStack.push(new HashMap<>(memory));
        
        // Crear nuevo ámbito (solo preservar strings globales)
        Map<Integer, Object> newScope = new HashMap<>();
        for (Map.Entry<Integer, Object> entry : memory.entrySet()) {
            if (entry.getKey() < 0) {
                newScope.put(entry.getKey(), entry.getValue());
            }
        }
        memory = newScope;
        
        pc = functionAddress;
    }
    
    private void executeReturn() {
        int returnValue = stack.pop();
        
        if (returnAddressStack.isEmpty()) {
            throw new RuntimeException(STACK_UNDERFLOW_MSG);
        }
        
        pc = returnAddressStack.pop();
        
        if (!callStack.isEmpty()) {
            memory = callStack.pop();
        }
        
        stack.push(returnValue);
    }
    
    // ==================== OPERACIONES DE ENTRADA/SALIDA ====================
    
    private void executePrintInt() {
        int value = stack.pop();
        System.out.print(value);
    }
    
    private void executePrintFloat() {
        int bits = stack.pop();
        float value = Float.intBitsToFloat(bits);
        System.out.print(value);
    }
    
    private void executePrintString() {
        int stringId = readInt();
        String value = (String) memory.get(-stringId - 1);
        System.out.print(value != null ? value : NULL_STRING);
    }
    
    private void executeReadInt() {
        System.out.print(PROMPT_INPUT);
        System.out.flush();
        
        if (scanner == null) {
            scanner = new Scanner(System.in);
        }
        
        int value = scanner.nextInt();
        stack.push(value);
    }
    
    private void executeReadFloat() {
        System.out.print(PROMPT_INPUT);
        System.out.flush();
        
        if (scanner == null) {
            scanner = new Scanner(System.in);
        }
        
        float value = scanner.nextFloat();
        stack.push(Float.floatToIntBits(value));
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private int readInt() {
        int value = ((bytecode[pc] & 0xFF) << 24) |
                    ((bytecode[pc + 1] & 0xFF) << 16) |
                    ((bytecode[pc + 2] & 0xFF) << 8) |
                    (bytecode[pc + 3] & 0xFF);
        pc += 4;
        return value;
    }
    
    private float readFloat() {
        return Float.intBitsToFloat(readInt());
    }
    
    private void validateJumpTarget(int target) {
        if (target < 0 || target >= bytecode.length) {
            throw new RuntimeException(String.format(SEGFAULT_MSG, target));
        }
    }
    
    // ==================== MÉTODOS DE UTILIDAD (OPCIONALES) ====================
    
    /**
     * Reinicia la VM a su estado inicial
     */
    public void reset() {
        pc = 0;
        stack.clear();
        returnAddressStack.clear();
        callStack.clear();
        memory.clear();
        scanner = null;
    }
    
    /**
     * Obtiene el valor de una variable en memoria (para debugging)
     */
    public Object getVariable(int address) {
        return memory.get(address);
    }
    
    /**
     * Obtiene el estado actual de la pila (para debugging)
     */
    public List<Integer> getStack() {
        return new ArrayList<>(stack);
    }
    
    /**
     * Establece un punto de interrupción (para debugging)
     */
    public void setBreakpoint(int pc) {
        // Implementación de punto de interrupción (se puede expandir)
        System.out.printf("Breakpoint en PC=%d%n", pc);
    }
}