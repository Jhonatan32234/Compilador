package new_languaje;

import java.util.*;

class SymbolTable {
    private final Map<String, String> symbols;
    private final SymbolTable parent;
    
    public SymbolTable(SymbolTable parent) {
        this.symbols = new HashMap<>();
        this.parent = parent;
    }
    
    /**
     * Declara una nueva variable en el ámbito actual
     * @param name Nombre de la variable
     * @param type Tipo de la variable
     * @throws Exception Si la variable ya está declarada en este ámbito
     */
    public void declare(String name, String type) throws Exception {
        if (symbols.containsKey(name)) {
            throw new Exception(String.format(
                "Error Semántico: Variable '%s' ya declarada en este ámbito.", name
            ));
        }
        symbols.put(name, type);
    }
    
    /**
     * Busca una variable en la tabla de símbolos actual y en los ámbitos padres
     * @param name Nombre de la variable a buscar
     * @return El tipo de la variable
     * @throws Exception Si la variable no está definida en ningún ámbito
     */
    public String lookup(String name) throws Exception {
        // Buscar en el ámbito actual
        if (symbols.containsKey(name)) {
            return symbols.get(name);
        }
        
        // Buscar en el ámbito padre si existe
        if (parent != null) {
            return parent.lookup(name);
        }
        
        // No encontrada en ningún ámbito
        throw new Exception(String.format(
            "Error Semántico: Variable '%s' no definida.", name
        ));
    }
    
    /**
     * Verifica si una variable existe en el ámbito actual o en algún padre
     * @param name Nombre de la variable
     * @return true si la variable existe, false en caso contrario
     */
    public boolean contains(String name) {
        if (symbols.containsKey(name)) {
            return true;
        }
        return parent != null && parent.contains(name);
    }
    
    /**
     * Obtiene el tipo de una variable si existe
     * @param name Nombre de la variable
     * @return Optional con el tipo si existe, Optional.empty() si no
     */
    public Optional<String> getType(String name) {
        try {
            return Optional.of(lookup(name));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Obtiene el ámbito padre
     * @return El ámbito padre o null si es el ámbito global
     */
    public SymbolTable getParent() {
        return parent;
    }
    
    /**
     * Verifica si este ámbito es el global (sin padre)
     * @return true si es el ámbito global, false en caso contrario
     */
    public boolean isGlobalScope() {
        return parent == null;
    }
    
    /**
     * Obtiene una vista inmutable de los símbolos en el ámbito actual
     * @return Mapa inmutable de los símbolos actuales
     */
    public Map<String, String> getLocalSymbols() {
        return Collections.unmodifiableMap(symbols);
    }
    
    /**
     * Obtiene todos los símbolos accesibles desde este ámbito (incluyendo padres)
     * @return Mapa con todos los símbolos accesibles
     */
    public Map<String, String> getAllSymbols() {
        Map<String, String> allSymbols = new HashMap<>();
        
        // Agregar símbolos de los padres primero (para que sean sobrescritos por los locales si hay conflicto)
        if (parent != null) {
            allSymbols.putAll(parent.getAllSymbols());
        }
        
        // Agregar símbolos locales (sobrescriben a los padres si hay conflicto)
        allSymbols.putAll(symbols);
        
        return Collections.unmodifiableMap(allSymbols);
    }
    
    /**
     * Representación en cadena de la tabla de símbolos
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SymbolTable{");
        
        if (parent != null) {
            sb.append("parent=").append(parent.hashCode()).append(", ");
        }
        
        sb.append("symbols=").append(symbols);
        sb.append("}");
        
        return sb.toString();
    }
    
    /**
     * Imprime la tabla de símbolos de forma jerárquica
     */
    public void printHierarchy() {
        printHierarchy(0);
    }
    
    private void printHierarchy(int level) {
        String indent = "  ".repeat(level);
        System.out.println(indent + "Ámbito " + level + ": " + symbols);
        
        if (parent != null) {
            parent.printHierarchy(level + 1);
        }
    }
}