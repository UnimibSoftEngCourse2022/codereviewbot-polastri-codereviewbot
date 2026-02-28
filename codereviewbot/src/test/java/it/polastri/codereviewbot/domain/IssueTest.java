package it.polastri.codereviewbot.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class IssueTest {

    // Crea correttamente una Issue valida e popola i campi accessibili
    @Test
    void creaIssueValida() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", java, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        RegolaAnalisi regola = new RegolaAnalisi("R1", "desc", Severita.WARNING, Categoria.STILE);
        Issue issue = new Issue(fa, 10, regola, "msg");

        assertEquals(fa, issue.getFileAnalizzato());
        assertEquals(10, issue.getRiga());
        assertEquals(regola, issue.getRegola());
        assertEquals("msg", issue.getMessaggio());
        assertNotNull(issue.toString());
    }

    // Il costruttore rifiuta parametri null e numeri di riga non validi
    @Test
    void rifiutaParametriNonValidi() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", java, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);
        RegolaAnalisi regola = new RegolaAnalisi("R1", "desc", Severita.WARNING, Categoria.STILE);

        assertThrows(NullPointerException.class, () -> new Issue(null, 1, regola, "msg"));
        assertThrows(NullPointerException.class, () -> new Issue(fa, 1, null, "msg"));
        assertThrows(NullPointerException.class, () -> new Issue(fa, 1, regola, null));

        assertThrows(IllegalArgumentException.class, () -> new Issue(fa, 0, regola, "msg"));
        assertThrows(IllegalArgumentException.class, () -> new Issue(fa, -1, regola, "msg"));
        assertThrows(IllegalArgumentException.class, () -> new Issue(fa, 1, regola, "   "));
    }
}