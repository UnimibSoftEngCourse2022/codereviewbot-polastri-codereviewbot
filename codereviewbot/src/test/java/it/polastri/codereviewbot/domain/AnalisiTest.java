package it.polastri.codereviewbot.domain; 

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.List;

class AnalisiTest {
	
	// Un'analisi appena creata deve partire nello stato CREATA
    @Test
    void constructor_shouldStartInCreata() {
        Progetto progetto = new Progetto("/tmp/progetto");
        Analisi analisi = new Analisi("A1", progetto);

        assertEquals(StatoAnalisi.CREATA, analisi.getStatoAnalisi());
        assertNull(analisi.getRisultato());
        assertNotNull(analisi.getDataOra());
        assertTrue(analisi.getIssues().isEmpty());
        assertTrue(analisi.getFileAnalizzati().isEmpty());
    }
    
    // avvia() è consentito solo se lo stato è CREATA
    @Test
    void avvia_shouldMoveToInEsecuzione_onlyIfCreata() {
        Analisi analisi = new Analisi("A1", new Progetto("/tmp/progetto"));

        analisi.avvia();
        assertEquals(StatoAnalisi.IN_ESECUZIONE, analisi.getStatoAnalisi());

        assertThrows(IllegalStateException.class, analisi::avvia);
    }
    
    // concludi() è consentito solo se l'analisi è in esecuzione
    @Test
    void concludi_shouldWorkOnlyIfInEsecuzione_andSetRisultato() {
        Analisi analisi = new Analisi("A1", new Progetto("/tmp/progetto"));

        RisultatoAnalisi risultatoFittizio = new RisultatoAnalisi(
                0, 0, java.util.Map.of(), java.util.List.of()
        );

        // Non è possibile concludere se non è in esecuzione
        assertThrows(IllegalStateException.class, () -> analisi.concludi(risultatoFittizio));

        analisi.avvia();

        // Il risultato non può essere null
        assertThrows(IllegalArgumentException.class, () -> analisi.concludi(null));

        analisi.concludi(risultatoFittizio);

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertEquals(risultatoFittizio, analisi.getRisultato());

        RisultatoAnalisi altro = new RisultatoAnalisi(0, 0, java.util.Map.of(), java.util.List.of());
        assertThrows(IllegalStateException.class, () -> analisi.concludi(altro));
    }
    
    // fallisci() è consentito solo se l'analisi è in esecuzione
    @Test
    void fallisci_shouldWorkOnlyIfInEsecuzione() {
        Analisi analisi = new Analisi("A1", new Progetto("/tmp/progetto"));

        // non in esecuzione -> errore
        assertThrows(IllegalStateException.class, analisi::fallisci);

        analisi.avvia();
        analisi.fallisci();

        assertEquals(StatoAnalisi.FALLITA, analisi.getStatoAnalisi());

        // non dovrebbe fallire di nuovo
        assertThrows(IllegalStateException.class, analisi::fallisci);
    }

    // registraIssue() e aggiungiFileAnalizzato() devono accettare solo valori validi e 
    // garantire l'incapsulamento delle collezioni interne
    @Test
    void registraIssue_and_aggiungiFileAnalizzato_shouldAddElements_andRejectNull() {
        Analisi analisi = new Analisi("A1", new Progetto("/tmp/progetto"));

        assertThrows(IllegalArgumentException.class, () -> analisi.registraIssue(null));
        assertThrows(IllegalArgumentException.class, () -> analisi.aggiungiFileAnalizzato(null));

        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/tmp/A.java", java, "class A{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        RegolaAnalisi regola = new RegolaAnalisi("R1", "desc", Severita.WARNING, Categoria.STILE);
        Issue issue = new Issue(fa, 1, regola, "msg");

        analisi.aggiungiFileAnalizzato(fa);
        analisi.registraIssue(issue);

        assertEquals(1, analisi.getFileAnalizzati().size());
        assertEquals(1, analisi.getIssues().size());

        assertThrows(UnsupportedOperationException.class, () -> analisi.getIssues().add(issue));
        assertThrows(UnsupportedOperationException.class, () -> analisi.getFileAnalizzati().add(fa));
    }
}