package new_languaje;

import java.util.ArrayList;
import java.util.List;

class Node {
    String value;
    Node left, right, next;
    Node elseNode, defaultNode;
    // Lista para parámetros de funciones
    List<Node> parameters = new ArrayList<>(); 
    int linea;
    int columna;

    public Node(String value, int linea, int columna) {
        this.value = value;
        this.linea = linea;
        this.columna = columna;
    }
    
    public void print(String prefix, boolean isLeft) {
        // Si el nodo es una función, imprimimos sus parámetros primero
        if (value.startsWith("FUNC:") && !parameters.isEmpty()) {
            System.out.println(prefix + " (Params: " + getParamsString() + ")");
        }

        if (right != null) right.print(prefix + (isLeft ? "│   " : "    "), false);
        System.out.println(prefix + (isLeft ? "└── " : "┌── ") + value);
        if (left != null) left.print(prefix + (isLeft ? "    " : "│   "), true);
        
        // Si hay un nodo siguiente (en un bloque), también lo imprimimos
        if (next != null) {
            next.print(prefix, isLeft);
        }
    }

    // Auxiliar para ver los parámetros en el print
    private String getParamsString() {
        List<String> p = new ArrayList<>();
        for (Node n : parameters) p.add(n.value + " " + (n.left != null ? n.left.value : ""));
        return String.join(", ", p);
    }
}