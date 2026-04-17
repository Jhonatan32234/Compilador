package new_languaje;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    
    // Constantes
    private static final String DEFAULT_FILENAME = "main.txt";
    private static final String OUTPUT_BYTECODE_FILENAME = "output.bc";
    
    // Modo de ejecución
    private enum ExecutionMode {
        BYTECODE,      // Generar y ejecutar bytecode con VM
        ASSEMBLY,      // Generar assembly tradicional
        BOTH           // Generar ambos
    }
    
    private static ExecutionMode mode = ExecutionMode.BYTECODE; // Cambiar según necesidad
    
    public static void main(String[] args) {
        // Parsear argumentos para seleccionar modo
        if (args.length > 1 && args[1].equals("--asm")) {
            mode = ExecutionMode.ASSEMBLY;
        } else if (args.length > 1 && args[1].equals("--both")) {
            mode = ExecutionMode.BOTH;
        }
        
        String nombreArchivo = getFileName(args);
        
        try {
            // Leer y mostrar el contenido del archivo
            String contenido = readFileContent(nombreArchivo);
            displayFileContent(nombreArchivo, contenido);
            
            // Procesar el código
            processCode(contenido);
            
        } catch (java.nio.file.NoSuchFileException e) {
            handleFileNotFoundError(nombreArchivo);
        } catch (Exception e) {
            handleCriticalError(e);
        }
    }
    
    private static String getFileName(String[] args) {
        return (args.length > 0) ? args[0] : DEFAULT_FILENAME;
    }
    
    private static String readFileContent(String filename) throws Exception {
        return new String(Files.readAllBytes(Paths.get(filename)));
    }
    
    private static void displayFileContent(String filename, String content) {
        System.out.println(String.format("--- LEYENDO ARCHIVO: %s ---", filename));
        System.out.println(content);
        System.out.println("------------------------------------------");
    }
    
    private static void processCode(String contenido) throws Exception {
        // 1. Fase de análisis léxico
        AnalizadorLexico lexer = new AnalizadorLexico();
        List<Token> tokens = lexer.escanear(contenido);

        for (Token t : tokens) {
            System.out.println(t.tipo + " -> " + t.lexeme);
        }
        
        // 2. Fase de análisis sintáctico
        Parser parser = new Parser(tokens);
        List<Node> treeNodes = parser.parseProgram();
        
        // 3. Fase de análisis semántico
        SemanticAnalyzer semantic = new SemanticAnalyzer();
        System.out.println("\n--- RESULTADO DEL ANÁLISIS ---");
        for (Node root : treeNodes) {
            semantic.validate(root);
        }
        
        // 4. Fase de generación de código intermedio (TAC)
        TACGenerator tac = new TACGenerator();
        System.out.println("\n--- CÓDIGO INTERMEDIO (TAC) ---");
        for (Node root : treeNodes) {
            tac.processNode(root);
        }
        tac.printInstructions();
        
        // 5. Generación de código según el modo seleccionado
        if (mode == ExecutionMode.BYTECODE || mode == ExecutionMode.BOTH) {
            generateAndRunBytecode(tac);
        }
    }
    
    /**
     * Genera bytecode y lo ejecuta en una máquina virtual
     */
    private static void generateAndRunBytecode(TACGenerator tac) throws Exception {
        System.out.println("\n--- GENERANDO BYTECODE ---");
        
        List<String> listaTac = tac.getInstructions();
        BytecodeGenerator bytecodeGen = new BytecodeGenerator(
            listaTac, 
            tac.getStringTable(), 
            tac.getTypeTable()
        );
        
        byte[] bytecode = bytecodeGen.generate();
        
        // Guardar bytecode en archivo
        Files.write(Paths.get(OUTPUT_BYTECODE_FILENAME), bytecode);
        System.out.println("Bytecode guardado en: " + OUTPUT_BYTECODE_FILENAME);
        System.out.println("Tamaño del bytecode: " + bytecode.length + " bytes");
        
        // Mostrar bytecode en formato hexadecimal (opcional)
        displayBytecodeHex(bytecode);
        
        // Ejecutar bytecode en la máquina virtual
        System.out.println("\n--- EJECUTANDO BYTECODE ---");
        VirtualMachine vm = new VirtualMachine(bytecode, tac.getStringTable());
        vm.execute();
        
        System.out.println("\n[EXITO]: El programa se ejecutó correctamente.");
    }
    
    /**
     * Muestra el bytecode en formato hexadecimal para depuración
     */
    private static void displayBytecodeHex(byte[] bytecode) {
        System.out.println("\n--- BYTECODE HEX DUMP ---");
        for (int i = 0; i < bytecode.length; i++) {
            if (i % 16 == 0) {
                System.out.printf("%n%04X: ", i);
            }
            System.out.printf("%02X ", bytecode[i]);
        }
        System.out.println("\n");
    }
    
    private static void handleFileNotFoundError(String filename) {
        System.err.println(String.format("ERROR: No se encontró el archivo '%s'", filename));
    }
    
    private static void handleCriticalError(Exception e) {
        System.err.println(String.format("\n[ERROR CRÍTICO]: %s", e.getMessage()));
        e.printStackTrace();
    }
}