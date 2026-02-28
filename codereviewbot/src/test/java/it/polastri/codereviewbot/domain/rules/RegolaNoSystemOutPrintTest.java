package it.polastri.codereviewbot.domain.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.polastri.codereviewbot.domain.FileAnalizzato;
import it.polastri.codereviewbot.domain.FileSorgente;
import it.polastri.codereviewbot.domain.Linguaggio;
import it.polastri.codereviewbot.domain.NodoAST;

class RegolaNoSystemOutPrintlnTest {

    // La regola deve generare una issue se nel nodo compare System.out.println
    @Test
    void generaIssueSeSystemOutPresente() {
        RegolaNoSystemOutPrintln regola = new RegolaNoSystemOutPrintln();

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        NodoAST nodo = new NodoAST("LINE", "System.out.println(\"ciao\");", 7);

        assertEquals(1, regola.applica(nodo, fa).size());
    }

    // La regola non deve generare issue se System.out.println non è presente
    @Test
    void nonGeneraIssueSeSystemOutAssente() {
        RegolaNoSystemOutPrintln regola = new RegolaNoSystemOutPrintln();

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        NodoAST nodo = new NodoAST("LINE", "logger.info(\"ciao\");", 3);

        assertTrue(regola.applica(nodo, fa).isEmpty());
    }
    
    // La regola deve lanciare NullPointerException se il nodo AST è null
    @Test
    void lanciaEccezioneSeNodoNullo() {
        RegolaNoSystemOutPrintln regola = new RegolaNoSystemOutPrintln();

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        assertThrows(NullPointerException.class, () -> regola.applica(null, fa));
    }

    // La regola deve lanciare NullPointerException se il file analizzato è null
    @Test
    void anciaEccezioneSeFileAnalizzatoNullo() {
        RegolaNoSystemOutPrintln regola = new RegolaNoSystemOutPrintln();

        NodoAST nodo = new NodoAST("LINE", "System.out.println(\"ciao\");", 7);

        assertThrows(NullPointerException.class, () -> regola.applica(nodo, null));
    }
}