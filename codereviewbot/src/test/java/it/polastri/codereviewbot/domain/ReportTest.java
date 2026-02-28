package it.polastri.codereviewbot.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ReportTest {

    // creaDa() rifiuta analisi non completata e input null
    @Test
    void creaDaRichiedeAnalisiCompletata() {
        Analisi a = new Analisi("A1", new Progetto("/p"));
        assertThrows(NullPointerException.class, () -> Report.creaDa(null, ReportFormat.JSON, 100));
        assertThrows(NullPointerException.class, () -> Report.creaDa(a, null, 100));
        assertThrows(IllegalStateException.class, () -> Report.creaDa(a, ReportFormat.JSON, 100));
    }

    // creaDa() classifica le issue per categoria e severità e rende la mappa non modificabile
    @Test
    void classificaIssuePerCategoriaESeverita() {
        Analisi analisi = new Analisi("A1", new Progetto("/p"));
        analisi.avvia();

        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", java, "x");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        RegolaAnalisi r1 = new RegolaAnalisi("R1", "d", Severita.WARNING, Categoria.STILE);
        RegolaAnalisi r2 = new RegolaAnalisi("R2", "d", Severita.ERROR, Categoria.COMPLESSITA);

        Issue i1 = new Issue(fa, 1, r1, "w");
        Issue i2 = new Issue(fa, 2, r1, "w");
        Issue i3 = new Issue(fa, 3, r2, "e");

        analisi.aggiungiFileAnalizzato(fa);
        analisi.registraIssue(i1);
        analisi.registraIssue(i2);
        analisi.registraIssue(i3);

        // Concludi analisi (serve per poter creare report)
        RisultatoAnalisi risultato = new RisultatoAnalisi(1, 2, Map.of(), 3);
        analisi.concludi(risultato);

        Report report = Report.creaDa(analisi, ReportFormat.JSON, 80);

        assertEquals("REP-" + analisi.getId(), report.getId());
        assertEquals(analisi, report.getAnalisi());
        assertEquals(ReportFormat.JSON, report.getFormato());
        assertEquals(80, report.getScoreQualita());
        assertNotNull(report.getGeneratoIl());

        // Verifica classificazione: STILE/WARNING = 2, COMPLESSITA/ERROR = 1
        Map<Categoria, Map<Severita, Integer>> c = report.getClassificazione();
        assertEquals(2, c.get(Categoria.STILE).get(Severita.WARNING));
        assertEquals(1, c.get(Categoria.COMPLESSITA).get(Severita.ERROR));

        // mappa esterna non modificabile
        Map<Categoria, Map<Severita, Integer>> classificazioneView = c;
        Map<Severita, Integer> nuovaMappa = Map.of();

        assertThrows(UnsupportedOperationException.class,
                () -> classificazioneView.put(Categoria.STILE, nuovaMappa));

        // mappa interna non modificabile
        Map<Severita, Integer> stileView = c.get(Categoria.STILE);
        assertThrows(UnsupportedOperationException.class,
                () -> stileView.put(Severita.INFO, 99));
    }
}