package it.polastri.codereviewbot.domain.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.polastri.codereviewbot.domain.FileAnalizzato;
import it.polastri.codereviewbot.domain.FileSorgente;
import it.polastri.codereviewbot.domain.Linguaggio;
import it.polastri.codereviewbot.domain.NodoAST;

class RegolaMetodoTroppoLungoTest {

    // La regola non si applica se il nodo non rappresenta un metodo
    @Test
    void nonGeneraIssueSeNodoNonMetodo() {
        RegolaMetodoTroppoLungo regola = new RegolaMetodoTroppoLungo(3);

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        NodoAST nodo = new NodoAST("CLASS", "A", 1);

        assertTrue(regola.applica(nodo, fa).isEmpty());
    }

    // La regola deve produrre una issue se il metodo ha più statement (figli) della soglia
    @Test
    void generaIssueSeMetodoTroppoLungo() {
        RegolaMetodoTroppoLungo regola = new RegolaMetodoTroppoLungo(3);

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        NodoAST metodo = new NodoAST("METHOD", "m", 10);

        // 4 figli => supera soglia 3
        metodo.aggiungiFiglio(new NodoAST("STMT", "s1", 11));
        metodo.aggiungiFiglio(new NodoAST("STMT", "s2", 12));
        metodo.aggiungiFiglio(new NodoAST("STMT", "s3", 13));
        metodo.aggiungiFiglio(new NodoAST("STMT", "s4", 14));

        assertEquals(1, regola.applica(metodo, fa).size());
    }

    // La regola non deve produrre issue se la soglia non è superata
    @Test
    void nonGeneraIssueSeMetodoEntroSoglia() {
        RegolaMetodoTroppoLungo regola = new RegolaMetodoTroppoLungo(3);

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        NodoAST metodo = new NodoAST("METHOD", "m", 10);

        // 3 figli => soglia 3 (non supera)
        metodo.aggiungiFiglio(new NodoAST("STMT", "s1", 11));
        metodo.aggiungiFiglio(new NodoAST("STMT", "s2", 12));
        metodo.aggiungiFiglio(new NodoAST("STMT", "s3", 13));

        assertTrue(regola.applica(metodo, fa).isEmpty());
    }
    
    // La regola deve lanciare NullPointerException se il nodo AST è null
    @Test
    void lanciaEccezioneSeNodoNullo() {
        RegolaMetodoTroppoLungo regola = new RegolaMetodoTroppoLungo(3);

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        assertThrows(NullPointerException.class, () -> regola.applica(null, fa));
    }
    
    // La regola deve lanciare NullPointerException se il file analizzato è null
    @Test
    void anciaEccezioneSeFileAnalizzatoNullo() {
        RegolaMetodoTroppoLungo regola = new RegolaMetodoTroppoLungo(3);

        NodoAST metodo = new NodoAST("METHOD", "m", 10);

        assertThrows(NullPointerException.class, () -> regola.applica(metodo, null));
    }
}