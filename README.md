# Compilador de "New Language"

Este proyecto es un compilador completo desarrollado en Java para un lenguaje de programación personalizado. El sistema abarca desde el análisis léxico hasta la ejecución en una Máquina Virtual (VM) propia.

## Características del Lenguaje

El lenguaje soporta las siguientes funcionalidades:
- **Tipos de datos:** `int`, `float`, `bool`, `void` y `string` (literales).
- **Estructuras de control:** 
  - Condicionales: `if`, `else`, `switch`, `case`, `default`.
  - Bucles: `while`, `do-while`, `for`.
  - Saltos: `break`, `return`.
- **Funciones:** Declaración de funciones con parámetros y valores de retorno.
- **Entrada/Salida:** Funciones nativas `print()` y `read()`.
- **Operadores:** Aritméticos (`+`, `-`, `*`, `/`, `%`), Relacionales (`==`, `!=`, `<`, `>`, `<=`, `>=`) y Asignación (`=`).

## 🛠 Arquitectura del Compilador

El proceso de compilación se divide en las siguientes etapas:

1.  **Analizador Léxico (`AnalizadorLexico.java`):** Convierte el código fuente en una lista de tokens utilizando expresiones regulares.
2.  **Analizador Sintáctico (`Parser.java`):** Construye el Árbol de Sintaxis Abstracta (AST) validando la gramática.
3.  **Analizador Semántico (`SemanticAnalyzer.java`):** Valida el uso de variables, tipos y ámbitos (scopes) mediante una Tabla de Símbolos.
4.  **Generador de Código Intermedio (`TACGenerator.java`):** Transforma el AST en Código de Tres Direcciones (TAC).
5.  **Generador de Bytecode (`BytecodeGenerator.java`):** Convierte las instrucciones TAC en instrucciones binarias optimizadas.
6.  **Máquina Virtual (`VirtualMachine.java`):** Ejecuta el bytecode generado utilizando una arquitectura basada en pila.

## Requisitos

- Java JDK 11 o superior.

## Instrucciones de Uso

### Ejecución Estándar
Para compilar y ejecutar el archivo de código fuente por defecto (`main.txt`):

```bash
java .\Main.java main.txt

En linux
1. compilar: javac -d . *.java
2. ejecutar: java new_languaje.Main main.txt
```

### Opciones de Ejecución
El compilador soporta diferentes modos de salida mediante flags:

- **Generar solo Bytecode (por defecto):**
  ```bash
  java .\Main.java <archivo.txt>
  ```
- **Generar Assembly (TAC):**
  ```bash
  java .\Main.java <archivo.txt> --asm
  ```
- **Generar ambos:**
  ```bash
  java .\Main.java <archivo.txt> --both
  ```

## 📂 Archivos Generados

- **`output.bc`:** Es el archivo binario (bytecode) generado tras una compilación exitosa. La Máquina Virtual lee este archivo para su ejecución. **No eliminar** si se desea depurar el binario.

## Ejemplo de Código (`main.txt`)

```cpp
int main() {
    int dia
    print("Introduce un numero de dia (1-3):")
    read(dia)
    
    switch (dia) {
        case 1: print("Lunes") break;
        default: print("Otro dia") break;
    }
    return 0
}
```

---
*Desarrollado como parte del curso de Compiladores.*
