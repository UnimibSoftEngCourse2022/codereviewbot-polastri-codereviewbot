package it.polastri.codereviewbot.domain; 

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.List;

class AnalisiTest {
	
	// Un'analisi appena creata deve partire nello stato CREATA
    @Test
    void iniziaInStatoCreata() {
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
    void avviaPassaInEsecuzioneSoloSeCreata() {
        Analisi analisi = new Analisi("A1", new Progetto("/tmp/progetto"));

        analisi.avvia();
        assertEquals(StatoAnalisi.IN_ESECUZIONE, analisi.getStatoAnalisi());

        assertThrows(IllegalStateException.class, analisi::avvia);
    }
    
    // concludi() è consentito solo se l'analisi è in esecuzione
    @Test
    void concludiSoloInEsecuzioneEImpostaRisultato() {
        Analisi analisi = new Analisi("A1", new Progetto("/tmp/progetto"));

        RisultatoAnalisi risultatoFittizio = new RisultatoAnalisi(0, 0, java.util.Map.of(), 0);

        // Non è possibile concludere se non è in esecuzione
        assertThrows(IllegalStateException.class, () -> analisi.concludi(risultatoFittizio));

        analisi.avvia();

        // Il risultato non può essere null
        assertThrows(NullPointerException.class, () -> analisi.concludi(null));

        analisi.concludi(risultatoFittizio);

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertEquals(risultatoFittizio, analisi.getRisultato());

        RisultatoAnalisi altro = new RisultatoAnalisi(0, 0, java.util.Map.of(), 0);
        assertThrows(IllegalStateException.class, () -> analisi.concludi(altro));
    }
    
    // fallisci() è consentito solo se l'analisi è in esecuzione
    @Test
    void fallisciSoloInEsecuzione() {
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
    void aggiungeElementiSoloInEsecuzioneERifiutaNull() {
        Analisi analisi = new Analisi("A1", new Progetto("/tmp/progetto"));

        // Arrange: costruisco l'Issue fuori dalla lambda (così non "inquina" assertThrows)
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente sorgente = new FileSorgente("A.java", "/tmp/A.java", java, "class A{}");
        FileAnalizzato fileAnalizzato = new FileAnalizzato("F1", sorgente);
        RegolaAnalisi regola = new RegolaAnalisi("R1", "desc", Severita.WARNING, Categoria.STILE);
        Issue issue = new Issue(fileAnalizzato, 1, regola, "msg");

        // è permesso registrare issue solo in esecuzione
        assertThrows(IllegalStateException.class, () -> analisi.registraIssue(issue));

        analisi.avvia();

        assertThrows(NullPointerException.class, () -> analisi.registraIssue(null));
    }
}