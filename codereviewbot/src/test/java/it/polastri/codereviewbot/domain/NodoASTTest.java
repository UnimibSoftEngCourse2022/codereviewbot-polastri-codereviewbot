package it.polastri.codereviewbot.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class NodoASTTest {

    // aggiungiFiglio() aggiunge correttamente un figlio e rifiuta null
    @Test
    void aggiungeFiglioERifiutaNull() {
        NodoAST padre = new NodoAST("Class", "A", 1);

        assertThrows(NullPointerException.class, () -> padre.aggiungiFiglio(null));

        NodoAST figlio = new NodoAST("Method", "m", 2);
        padre.aggiungiFiglio(figlio);

        assertEquals(1, padre.getFigli().size());
        assertEquals(figlio, padre.getFigli().get(0));

        assertThrows(UnsupportedOperationException.class, () -> padre.getFigli().add(figlio));
    }

    // accettaRegola() con una regola base non produce issue
    @Test
    void accettaRegolaConRegolaBaseNonGeneraIssue() {
        NodoAST nodo = new NodoAST("X", "val", 10);

        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", java, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        RegolaAnalisi regola = new RegolaAnalisi("R1", "desc", Severita.WARNING, Categoria.STILE);

        List<Issue> issues = nodo.accettaRegola(regola, fa);
        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    // accettaRegola() deve sollevare un'eccezione se regola o file analizzato sono null
    @Test
    void lanciaEccezioneSeArgomentiNull() {
        NodoAST nodo = new NodoAST("X", "val", 10);

        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", java, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        RegolaAnalisi regola = new RegolaAnalisi("R1", "desc", Severita.WARNING, Categoria.STILE);

        assertThrows(NullPointerException.class, () -> nodo.accettaRegola(null, fa));
        assertThrows(NullPointerException.class, () -> nodo.accettaRegola(regola, null));
    }
}