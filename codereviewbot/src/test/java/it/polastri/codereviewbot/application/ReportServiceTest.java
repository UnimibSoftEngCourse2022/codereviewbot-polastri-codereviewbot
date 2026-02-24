package it.polastri.codereviewbot.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import it.polastri.codereviewbot.domain.*;
import it.polastri.codereviewbot.domain.rules.RegolaNoTODO;
import it.polastri.codereviewbot.infrastructure.report.ReportExporter;
import it.polastri.codereviewbot.infrastructure.logger.LogLevel;
import it.polastri.codereviewbot.infrastructure.logger.Logger;

class ReportServiceTest {

    /**
     * Logger spy per test: registra tutte le chiamate log(level, message)
     * così da verificare che il servizio produca log nei punti chiave
     * senza dipendere da output su console o timestamp.
     */
    private static class SpyLogger implements Logger {
        static record Entry(LogLevel level, String message) {}

        private final List<Entry> entries = new ArrayList<>();

        @Override
        public void log(LogLevel level, String message) {
            entries.add(new Entry(level, message));
        }

        boolean hasLevel(LogLevel level) {
            return entries.stream().anyMatch(e -> e.level() == level);
        }
    }

	/* Oggetto utilizzato per non scrivere file su disco, ma poter
	 * comunque testare che ReportService chiami ReportExporter correttamente.
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

    // Il servizio deve rifiutare un'analisi non completata.
    @Test
    void generaReportRichiedeAnalisiCompletata() {
        SpyExporter exporter = new SpyExporter();
        SpyLogger logger = new SpyLogger();

        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter)),
                logger
        );

        Analisi analisi = new Analisi("A1", new Progetto("/p")); // stato CREATA

        assertThrows(IllegalStateException.class,
                () -> service.generaReportQualita(analisi, ReportFormat.JSON, "/out.json"));

        // Ci si aspetta un ERROR (analisi non completata)
        assertTrue(logger.hasLevel(LogLevel.ERROR),
                "Mi aspetto un ERROR se provo a generare report su analisi non completata");
    }

    // Il servizio genera report, calcola lo score e delega l'export all'exporter.
    @Test
    void generaReportCalcolaScoreEClassificaIssueEDelegaExport() {
        SpyExporter exporter = new SpyExporter();
        SpyLogger logger = new SpyLogger();

        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter)),
                logger
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

        // Deve esserci almeno un INFO durante generazione/esportazione
        assertTrue(logger.hasLevel(LogLevel.INFO), "Mi aspetto log INFO durante generazione/export report");
    }

    // Se outputPath è null/blank il servizio non deve chiamare l'exporter.
    @Test
    void nonEsportaSeOutputPathAssente() {
        SpyExporter exporter = new SpyExporter();
        SpyLogger logger = new SpyLogger();

        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter)),
                logger
        );

        Analisi analisi = new Analisi("A1", new Progetto("/p"));
        analisi.avvia();
        analisi.concludi(new RisultatoAnalisi(0, 0, Map.of(), 0));

        Report report = service.generaReportQualita(analisi, ReportFormat.JSON, "   ");

        assertNotNull(report);
        assertNull(exporter.lastReport);
        assertNull(exporter.lastPath);

        // Ci si aspetta comunque INFO (generazione report avviata)
        assertTrue(logger.hasLevel(LogLevel.INFO), "Mi aspetto almeno un INFO anche senza esportazione");
    }

    // Se viene richiesto export ma non esiste un exporter registrato per quel formato, deve fallire.
    @Test
    void fallisceSeMancaExporterPerFormato() {
        SpyLogger logger = new SpyLogger();

        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(),
                logger
        );

        Analisi analisi = new Analisi("A1", new Progetto("/p"));
        analisi.avvia();
        analisi.concludi(new RisultatoAnalisi(0, 0, Map.of(), 0));

        assertThrows(IllegalStateException.class,
                () -> service.generaReportQualita(analisi, ReportFormat.PDF, "/out.pdf"));

        // Ci si aspetta un ERROR (manca exporter per formato richiesto)
        assertTrue(logger.hasLevel(LogLevel.ERROR), "Mi aspetto un ERROR se manca l'exporter per il formato");
    }

    // Il costruttore deve rifiutare binding duplicati per lo stesso formato.
    @Test
    void rifiutaBindingsDuplicatiPerFormato() {
        // Due exporter diversi ma con stesso formato -> duplicato
        SpyExporter exporter1 = new SpyExporter();
        SpyExporter exporter2 = new SpyExporter();

        QualityScoreService qualityScoreService = new QualityScoreService();

        ReportService.ReportExporterBinding binding1 =
                new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter1);

        ReportService.ReportExporterBinding binding2 =
                new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter2);

        List<ReportService.ReportExporterBinding> bindings = List.of(binding1, binding2);

        // La costruzione del servizio deve rifiutare duplicati sul formato
        assertThrows(IllegalArgumentException.class, () -> new ReportService(qualityScoreService, bindings));
    }
}