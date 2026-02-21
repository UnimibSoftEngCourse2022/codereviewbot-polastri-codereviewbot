package it.polastri.codereviewbot.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import it.polastri.codereviewbot.domain.*;
import it.polastri.codereviewbot.domain.rules.RegolaNoTODO;
import it.polastri.codereviewbot.infrastructure.report.ReportExporter;

class ReportServiceTest {

	/* Oggetto utilizzato per non scrivere file su disco, ma poter 
	 * comunque testare che ReportService chiami ReportExporter correttamente 
	 */
    private static class SpyExporter implements ReportExporter {
        Report lastReport;
        String lastPath;

        @Override
        public void esporta(Report report, String outputPath) {
            this.lastReport = report;
            this.lastPath = outputPath;
        }
    }

    // Il servizio deve rifiutare un'analisi non completata (RD5)
    @Test
    void generaReportRichiedeAnalisiCompletata() {
        SpyExporter exporter = new SpyExporter();
        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter))
        );

        Analisi analisi = new Analisi("A1", new Progetto("/p")); // stato CREATA
        assertThrows(IllegalStateException.class,
                () -> service.generaReportQualita(analisi, ReportFormat.JSON, "/out.json"));
    }

    // Il servizio genera report, calcola lo score e delega l'export all'exporter
    @Test
    void generaReportCalcolaScoreEClassificaIssueEDelegaExport() {
        SpyExporter exporter = new SpyExporter();
        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter))
        );

        // Analisi completata con 1 issue WARNING (NoTODO)
        Analisi analisi = new Analisi("A1", new Progetto("/p"));
        analisi.avvia();

        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", java, "// TODO");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        RegolaAnalisi regola = new RegolaNoTODO();
        Issue issue = new Issue(fa, 1, regola, "todo");

        analisi.aggiungiFileAnalizzato(fa);
        analisi.registraIssue(issue);

        // per poter generare report, l'analisi deve essere COMPLETATA
        analisi.concludi(new RisultatoAnalisi(0, 1, Map.of(), 1));

        Report report = service.generaReportQualita(analisi, ReportFormat.JSON, "/out.json");

        assertNotNull(report);
        assertEquals(analisi, report.getAnalisi());
        assertEquals(ReportFormat.JSON, report.getFormato());

        // Score atteso: base 100 - WARNING(2) = 98 
        assertEquals(98, report.getScoreQualita());

        // Verifica export delegato
        assertNotNull(exporter.lastReport);
        assertEquals("/out.json", exporter.lastPath);
        assertEquals(report, exporter.lastReport);

        // Verifica classificazione coerente: categoria STILE, severità WARNING, count=1
        assertEquals(1, report.getClassificazione().get(Categoria.STILE).get(Severita.WARNING));
    }

    // Se outputPath è null/blank il servizio non deve chiamare l'exporter
    @Test
    void nonEsportaSeOutputPathAssente() {
        SpyExporter exporter = new SpyExporter();
        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter))
        );

        Analisi analisi = new Analisi("A1", new Progetto("/p"));
        analisi.avvia();
        analisi.concludi(new RisultatoAnalisi(0, 0, Map.of(), 0));

        Report report = service.generaReportQualita(analisi, ReportFormat.JSON, "   ");

        assertNotNull(report);
        assertNull(exporter.lastReport);
        assertNull(exporter.lastPath);
    }

    // Se viene richiesto export ma non esiste un exporter registrato per quel formato, deve fallire
    @Test
    void fallisceSeMancaExporterPerFormato() {
        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of() // nessun exporter
        );

        Analisi analisi = new Analisi("A1", new Progetto("/p"));
        analisi.avvia();
        analisi.concludi(new RisultatoAnalisi(0, 0, Map.of(), 0));

        assertThrows(IllegalStateException.class,
                () -> service.generaReportQualita(analisi, ReportFormat.PDF, "/out.pdf"));
    }

    // Il costruttore deve rifiutare binding duplicati per lo stesso formato
    @Test
    void rifiutaBindingsDuplicatiPerFormato() {
        SpyExporter exporter1 = new SpyExporter();
        SpyExporter exporter2 = new SpyExporter();

        assertThrows(IllegalArgumentException.class, () -> new ReportService(
                new QualityScoreService(),
                List.of(
                    new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter1),
                    new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter2)
                )
        ));
    }
}