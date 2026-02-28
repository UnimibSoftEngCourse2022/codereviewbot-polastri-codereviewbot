package it.polastri.codereviewbot.domain.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.polastri.codereviewbot.domain.FileAnalizzato;
import it.polastri.codereviewbot.domain.FileSorgente;
import it.polastri.codereviewbot.domain.Linguaggio;
import it.polastri.codereviewbot.domain.NodoAST;

class RegolaNoTODOTest {

    // La regola deve generare una issue se nel nodo compare "TODO"
    @Test
    void generaIssueSeTodoPresent() {
        RegolaNoTODO regola = new RegolaNoTODO();

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        NodoAST nodo = new NodoAST("LINE", "// TODO: fix", 12);

        assertEquals(1, regola.applica(nodo, fa).size());
    }

    // La regola non deve generare issue se non c'è "TODO"
    @Test
    void nonGeneraIssueSeTodoAssente() {
        RegolaNoTODO regola = new RegolaNoTODO();

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        NodoAST nodo = new NodoAST("LINE", "System.out.println(\"ok\");", 5);

        assertTrue(regola.applica(nodo, fa).isEmpty());
    }
    
    // La regola deve lanciare NullPointerException se il nodo AST è null
    @Test
    void lanciaEccezioneSeNodoNullo() {
        RegolaNoTODO regola = new RegolaNoTODO();

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        assertThrows(NullPointerException.class, () -> regola.applica(null, fa));
    }

    // La regola deve lanciare NullPointerException se il file analizzato è null
    @Test
    void lanciaEccezioneSeFileAnalizzatoNullo() {
        RegolaNoTODO regola = new RegolaNoTODO();

        NodoAST nodo = new NodoAST("LINE", "// TODO: fix", 12);

        assertThrows(NullPointerException.class, () -> regola.applica(nodo, null));
    }
}