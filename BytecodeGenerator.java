package new_languaje;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.*;

class BytecodeGenerator {
    // Constantes de opcodes
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
    
    // Constantes para strings
    private static final String PRINT_STR = "print_str";
    private static final String PRINT_INT = "print_int";
    private static final String PRINT_FLOAT = "print_float";
    private static final String PRINT_NL = "print_nl";
    private static final String ALLOC = "alloc";
    private static final String ARG = "arg";
    private static final String BEGIN_FUNC = "begin_func";
    private static final String END_FUNC = "end_func";
    private static final String PARAM = "param";
    private static final String RETURN = "return";
    private static final String GOTO = "goto";
    private static final String IF_FALSE = "ifFalse";
    private static final String READ_INT = "read_int";
    private static final String READ_FLOAT = "read_float";
    private static final String CALL = "call";
    
    // Constantes para patrones de operadores
    private static final String EQ_OP = " == ";
    private static final String NEQ_OP = " != ";
    private static final String GE_OP = " >= ";
    private static final String LE_OP = " <= ";
    private static final String GT_OP = " > ";
    private static final String LT_OP = " < ";
    private static final String ADD_OP = " + ";
    private static final String SUB_OP = " - ";
    private static final String MUL_OP = " * ";
    private static final String DIV_OP = " / ";
    private static final String MOD_OP = " % ";
    
    // Constantes para patrones de valores
    private static final String INTEGER_PATTERN = "-?\\d+";
    private static final String FLOAT_PATTERN = "-?\\d+\\.\\d+";
    private static final String STRING_LABEL_PREFIX = "str";
    
    // Constantes para nombres especiales
    private static final String MAIN_FUNCTION = "main";
    private static final int BYTE_SIZE = 4;
    
    // Datos de entrada
    private final List<String> tac;
    private final Map<String, String> typeTable;
    
    // Estado del generador
    private final Map<String, Integer> stackMap = new HashMap<>();
    private final List<BytecodeInstruction> bytecode = new ArrayList<>();
    private final Map<String, Integer> labelMap = new HashMap<>();
    private int stackOffset = 0;
    private String currentFunction = "";
    
    public BytecodeGenerator(List<String> tac, Map<String, String> stringTable, 
                             Map<String, String> typeTable) {
        this.tac = Objects.requireNonNull(tac, "TAC no puede ser nulo");
        this.typeTable = Objects.requireNonNull(typeTable, "TypeTable no puede ser nulo");
        // stringTable no se usa actualmente pero se mantiene para compatibilidad
    }
    
    // ==================== MÉTODO PRINCIPAL ====================
    
    public byte[] generate() {
        reset();
        
        // Entry Point: Forzamos un salto a la función 'main'
        addInstruction(OP_JMP);
        addLabelOperand(MAIN_FUNCTION);
        
        // Procesar todas las instrucciones TAC
        for (String line : tac) {
            processInstruction(line);
        }
        
        resolveLabels();
        return bytecodeToArray();
    }
    
    // ==================== MÉTODOS DE PROCESAMIENTO ====================
    
    private void processInstruction(String line) {
        if (line == null || line.trim().isEmpty()) return;
        
        // Usar un enfoque de despacho por prefijo
        if (line.startsWith(PRINT_STR)) {
            processPrintStr(line);
        } 
        else if (line.startsWith(PRINT_INT)) {
            processPrintInt(line);
        }
        else if (line.startsWith(PRINT_FLOAT)) {
            processPrintFloat(line);
        }
        else if (line.startsWith(ALLOC)) {
            processAlloc(line);
        }
        else if (line.startsWith(ARG)) {
            processArg(line);
        }
        else if (line.equals(PRINT_NL)) {
            addInstruction(OP_PRINT_NL);
        }
        else if (line.startsWith(BEGIN_FUNC)) {
            processBeginFunction(line);
        }
        else if (line.startsWith(END_FUNC)) {
            processEndFunction();
        }
        else if (line.startsWith(PARAM)) {
            processParam(line);
        }
        else if (line.startsWith(RETURN)) {
            processReturn(line);
        }
        else if (line.startsWith(GOTO)) {
            processGoto(line);
        }
        else if (line.startsWith(IF_FALSE)) {
            processIfFalse(line);
        }
        else if (line.startsWith(READ_INT)) {
            processReadInt(line);
        }
        else if (line.startsWith(READ_FLOAT)) {
            processReadFloat(line);
        }
        else if (isBinaryOperation(line)) {
            processBinaryOperation(line);
        }
        else if (isCallOperation(line)) {
            processCallOperation(line);
        }
        else if (isAssignmentOperation(line)) {
            processAssignment(line);
        }
        else if (line.endsWith(":")) {
            processLabel(line);
        }
    }
    
    // ==================== PROCESAMIENTO DE INSTRUCCIONES ESPECÍFICAS ====================
    
    private void processPrintStr(String line) {
        addInstruction(OP_PRINT_STR);
        addStringConstant(extractOperand(line));
    }
    
    private void processPrintInt(String line) {
        loadOperand(extractOperand(line));
        addInstruction(OP_PRINT_INT);
    }
    
    private void processPrintFloat(String line) {
        loadOperand(extractOperand(line));
        addInstruction(OP_PRINT_FLOAT);
    }
    
    private void processAlloc(String line) {
        String[] parts = line.split(" ");
        String varName = parts[1].replace(",", "");
        String type = parts[2];
        typeTable.put(varName, type);
        allocateVariable(varName);
    }
    
    private void processArg(String line) {
        // Cargar argumento en la pila antes de llamar a la función
        loadOperand(extractOperand(line));
    }
    
    private void processBeginFunction(String line) {
        currentFunction = extractOperand(line);
        BytecodeInstruction instr = new BytecodeInstruction((byte)0);
        instr.isLabelDef = true;
        instr.tempLabel = currentFunction;
        instr.writeOpcode = false;
        bytecode.add(instr);
    }
    
    private void processEndFunction() {
        if (MAIN_FUNCTION.equals(currentFunction)) {
            addInstruction(OP_HALT);
        } else {
            addInstruction(OP_RET);
        }
    }
    
    private void processParam(String line) {
        String paramName = extractOperand(line);
        addInstruction(OP_STORE);
        addVariableOperand(paramName);
    }
    
    private void processReturn(String line) {
        String[] parts = line.split(" ");
        if (parts.length > 1) {
            loadOperand(parts[1]);
        } else {
            addInstruction(OP_PUSH_INT);
            addIntOperand(0);
        }
        
        if (MAIN_FUNCTION.equals(currentFunction)) {
            addInstruction(OP_HALT);
        } else {
            addInstruction(OP_RET);
        }
    }
    
    private void processGoto(String line) {
        addInstruction(OP_JMP);
        addLabelOperand(extractOperand(line));
    }
    
    private void processIfFalse(String line) {
        String[] parts = line.split(" ");
        loadOperand(parts[1]); // Cargar condición
        addInstruction(OP_JMP_IF_FALSE);
        addLabelOperand(parts[3]);
    }
    
    private void processReadInt(String line) {
        String varName = extractOperand(line);
        addInstruction(OP_READ_INT);
        addInstruction(OP_STORE);
        addVariableOperand(varName);
    }
    
    private void processReadFloat(String line) {
        String varName = extractOperand(line);
        addInstruction(OP_READ_FLOAT);
        addInstruction(OP_STORE);
        addVariableOperand(varName);
    }
    
    private void processBinaryOperation(String line) {
        String[] parts = line.split(" ");
        String target = parts[0];
        String left = parts[2];
        String right = parts[4];
        
        loadOperand(left);
        loadOperand(right);
        
        addBinaryOpcode(line, target);
        
        addInstruction(OP_STORE);
        addVariableOperand(target);
    }
    
    private void processCallOperation(String line) {
        String[] parts = line.split(" ");
        String targetTemp = parts[0];
        String funcName = parts[3].replace(",", "");
        
        addInstruction(OP_CALL);
        addFunctionCall(funcName);
        addInstruction(OP_STORE);
        addVariableOperand(targetTemp);
    }
    
    private void processAssignment(String line) {
        String[] parts = line.split(" = ");
        String target = parts[0];
        String value = parts[1];
        
        loadOperand(value);
        addInstruction(OP_STORE);
        addVariableOperand(target);
    }
    
    private void processLabel(String line) {
        String label = line.substring(0, line.length() - 1);
        BytecodeInstruction instr = new BytecodeInstruction((byte)0);
        instr.isLabelDef = true;
        instr.tempLabel = label;
        instr.writeOpcode = false;
        bytecode.add(instr);
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private void addBinaryOpcode(String line, String target) {
        String type = typeTable.get(target);
        boolean isFloat = "float".equals(type);

        if (line.contains(EQ_OP)) addInstruction(OP_EQ);
        else if (line.contains(NEQ_OP)) addInstruction(OP_NEQ);
        else if (line.contains(GE_OP)) addInstruction(OP_GE);
        else if (line.contains(LE_OP)) addInstruction(OP_LE);
        else if (line.contains(GT_OP)) addInstruction(OP_GT);
        else if (line.contains(LT_OP)) addInstruction(OP_LT);
        else if (line.contains(ADD_OP)) addInstruction(isFloat ? OP_FADD : OP_ADD);
        else if (line.contains(SUB_OP)) addInstruction(isFloat ? OP_FSUB : OP_SUB);
        else if (line.contains(MUL_OP)) addInstruction(isFloat ? OP_FMUL : OP_MUL);
        else if (line.contains(DIV_OP)) addInstruction(isFloat ? OP_FDIV : OP_DIV);
        else if (line.contains(MOD_OP)) addInstruction(OP_MOD);
    }
    
    private boolean isBinaryOperation(String line) {
        return line.contains(EQ_OP) || line.contains(NEQ_OP) || line.contains(GE_OP) ||
               line.contains(LE_OP) || line.contains(GT_OP) || line.contains(LT_OP) ||
               line.contains(ADD_OP) || line.contains(SUB_OP) || line.contains(MUL_OP) ||
               line.contains(DIV_OP) || line.contains(MOD_OP);
    }
    
    private boolean isCallOperation(String line) {
        return line.contains(CALL);
    }
    
    private boolean isAssignmentOperation(String line) {
        return line.contains(" = ") && !line.contains(CALL) && !isBinaryOperation(line);
    }
    
    private String extractOperand(String line) {
        String[] parts = line.split(" ");
        return parts[1];
    }
    
    private void loadOperand(String val) {
        if (val.matches(INTEGER_PATTERN)) {
            addInstruction(OP_PUSH_INT);
            addIntOperand(Integer.parseInt(val));
        } else if (val.matches(FLOAT_PATTERN)) {
            addInstruction(OP_PUSH_FLOAT);
            bytecode.add(new BytecodeInstruction(BytecodeType.FLOAT, Float.parseFloat(val)));
        } else {
            // Es una variable/temporal
            addInstruction(OP_LOAD);
            addVariableOperand(val);
        }
    }
    
    private void addInstruction(byte opcode) {
        bytecode.add(new BytecodeInstruction(opcode));
    }
    
    private void addIntOperand(int value) {
        bytecode.add(new BytecodeInstruction(BytecodeType.INT, value));
    }
    
    private void addStringConstant(String label) {
        if (label.startsWith(STRING_LABEL_PREFIX)) {
            int id = Integer.parseInt(label.substring(3));
            addIntOperand(id);
        }
    }
    
    private void addVariableOperand(String varName) {
        int offset = getVariableOffset(varName);
        bytecode.add(new BytecodeInstruction(BytecodeType.VAR, offset));
    }
    
    private void addLabelOperand(String label) {
        bytecode.add(new BytecodeInstruction(BytecodeType.LABEL, label));
    }
    
    private void addFunctionCall(String funcName) {
        addLabelOperand(funcName);
    }
    
    private int getVariableOffset(String varName) {
        if (!stackMap.containsKey(varName)) {
            stackOffset += BYTE_SIZE;
            stackMap.put(varName, stackOffset);
        }
        return stackMap.get(varName);
    }
    
    private void allocateVariable(String varName) {
        getVariableOffset(varName);
    }
    
    private void resolveLabels() {
        int currentAddr = 0;
        
        // Primera pasada: calcular direcciones
        for (BytecodeInstruction instr : bytecode) {
            if (instr.isLabelDef) {
                labelMap.put(instr.tempLabel, currentAddr);
            } else {
                currentAddr += instr.getSize();
            }
        }
        
        // Segunda pasada: resolver referencias
        for (BytecodeInstruction instr : bytecode) {
            if (instr.isLabelRef) {
                Integer addr = labelMap.get(instr.tempLabel);
                if (addr == null) {
                    throw new RuntimeException("Etiqueta no encontrada: " + instr.tempLabel);
                }
                instr.data = ByteBuffer.allocate(BYTE_SIZE).putInt(addr).array();
                instr.isLabelRef = false;
            }
        }
    }
    
    private byte[] bytecodeToArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (BytecodeInstruction instr : bytecode) {
            if (instr.writeOpcode) {
                baos.write(instr.opcode);
            }
            if (instr.hasData()) {
                baos.write(instr.data, 0, instr.data.length);
            }
        }
        return baos.toByteArray();
    }
    
    private void reset() {
        bytecode.clear();
        labelMap.clear();
        stackMap.clear();
        stackOffset = 0;
        currentFunction = "";
    }
    
    // ==================== CLASES AUXILIARES ====================
    
    private static class BytecodeInstruction {
        byte opcode;
        byte[] data;
        boolean writeOpcode;
        
        boolean isLabelDef = false;
        boolean isLabelRef = false;
        String tempLabel = null;
        
        BytecodeInstruction(byte opcode) {
            this.opcode = opcode;
            this.data = new byte[0];
            this.writeOpcode = true;
        }
        
        BytecodeInstruction(BytecodeType type, int value) {
            this.opcode = type.opcode;
            this.data = ByteBuffer.allocate(BYTE_SIZE).putInt(value).array();
            this.writeOpcode = false;
        }
        
        BytecodeInstruction(BytecodeType type, float value) {
            this.opcode = type.opcode;
            this.data = ByteBuffer.allocate(BYTE_SIZE).putFloat(value).array();
            this.writeOpcode = false;
        }
        
        BytecodeInstruction(BytecodeType type, String str) {
            this.opcode = type.opcode;
            if (type == BytecodeType.LABEL) {
                this.isLabelRef = true;
                this.tempLabel = str;
                this.writeOpcode = false;
                this.data = new byte[0];
            } else {
                this.data = str.getBytes();
                this.writeOpcode = true;
            }
        }
        
        boolean hasData() {
            return data != null && data.length > 0;
        }
        
        int getSize() {
            if (isLabelDef) return 0;
            if (isLabelRef) return BYTE_SIZE;
            
            int size = 0;
            if (writeOpcode) size++;
            if (hasData()) size += data.length;
            return size;
        }
    }
    
    private enum BytecodeType {
        INT(0x01), FLOAT(0x02), VAR(0x03), STRING(0x04), 
        ADDRESS(0x05), LABEL(0x06), FUNC_MARKER(0x07);
        
        final byte opcode;
        BytecodeType(int opcode) { this.opcode = (byte) opcode; }
    }
}