package it.polastri.codereviewbot.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import it.polastri.codereviewbot.domain.*;
import it.polastri.codereviewbot.domain.rules.RegolaNoTODO;
import it.polastri.codereviewbot.infrastructure.logger.LogLevel;
import it.polastri.codereviewbot.infrastructure.logger.Logger;
import it.polastri.codereviewbot.infrastructure.report.ReportExporter;

class ReportServiceTest {

    /**
     * Logger spy per test: registra tutte le chiamate log(level, message),
     * così da verificare che il servizio produca log nei punti chiave
     * senza dipendere da output su console.
     */
    private static class SpyLogger implements Logger {
        static record Entry(LogLevel level, String message) {}

        private final List<Entry> entries = new ArrayList<>();

        @Override
        public void log(LogLevel level, String message) {
            entries.add(new Entry(level, message));
        }

        // Verifica se è stato emesso almeno un log con il livello indicato.
        boolean hasLevel(LogLevel level) {
            return entries.stream().anyMatch(e -> e.level() == level);
        }

        // Restituisce l'ultimo messaggio di log di livello ERROR (se presente).
        String lastErrorMessageOrNull() {
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (entries.get(i).level() == LogLevel.ERROR) return entries.get(i).message();
            }
            return null;
        }
    }

    /**
     * Exporter spy per evitare I/O su disco e verificare che ReportService
     * chiami l'exporter con i parametri corretti.
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

    // Exporter che lancia una RuntimeException per testare il ramo catch di ReportService.
    private static class ThrowingExporter implements ReportExporter {
        @Override
        public void esporta(Report report, String outputPath) {
            throw new RuntimeException("boom");
        }
    }

    // L'exporter deve rifiutare un'analisi non completata. 
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
        String outPath = "/out.json";

        Executable action = () -> service.generaReportQualita(analisi, ReportFormat.JSON, outPath);
        assertThrows(IllegalStateException.class, action);

        assertTrue(logger.hasLevel(LogLevel.ERROR),
                "Mi aspetto un ERROR se provo a generare report su analisi non completata");
    }

    // Il servizio deve generare un report, calcolare lo score, classificare e delegare l'export. 
    @Test
    void generaReportCalcolaScoreEClassificaIssueEDelegaExport() {
        SpyExporter exporter = new SpyExporter();
        SpyLogger logger = new SpyLogger();

        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter)),
                logger
        );

        Analisi analisi = creaAnalisiCompletataConUnaIssueWarningNoTODO();

        String outPath = "/out.json";
        Report report = service.generaReportQualita(analisi, ReportFormat.JSON, outPath);

        assertNotNull(report);
        assertEquals(analisi, report.getAnalisi());
        assertEquals(ReportFormat.JSON, report.getFormato());

        // Score atteso: base 100 - WARNING(2) = 98
        assertEquals(98, report.getScoreQualita());

        // Export delegato
        assertNotNull(exporter.lastReport);
        assertEquals(outPath, exporter.lastPath);
        assertEquals(report, exporter.lastReport);

        // Classificazione: categoria STILE, severità WARNING, count=1
        assertEquals(1, report.getClassificazione().get(Categoria.STILE).get(Severita.WARNING));

        assertTrue(logger.hasLevel(LogLevel.INFO), "Mi aspetto log INFO durante generazione/export report");
    }

    // Se outputPath è blank/null, l'exporter non deve essere chiamato. 
    @Test
    void nonEsportaSeOutputPathAssente() {
        SpyExporter exporter = new SpyExporter();
        SpyLogger logger = new SpyLogger();

        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter)),
                logger
        );

        Analisi analisi = creaAnalisiCompletataSenzaIssue();

        String blankPath = "   ";
        Report report = service.generaReportQualita(analisi, ReportFormat.JSON, blankPath);

        assertNotNull(report);
        assertNull(exporter.lastReport);
        assertNull(exporter.lastPath);

        assertTrue(logger.hasLevel(LogLevel.INFO), "Mi aspetto almeno un INFO anche senza esportazione");
    }

    // Se manca l'exporter per il formato richiesto (con outputPath), il servizio deve fallire. 
    @Test
    void fallisceSeMancaExporterPerFormato() {
        SpyLogger logger = new SpyLogger();

        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(),
                logger
        );

        Analisi analisi = creaAnalisiCompletataSenzaIssue();
        String outPath = "/out.pdf";

        Executable action = () -> service.generaReportQualita(analisi, ReportFormat.PDF, outPath);
        assertThrows(IllegalStateException.class, action);

        assertTrue(logger.hasLevel(LogLevel.ERROR), "Mi aspetto un ERROR se manca l'exporter per il formato");
    }

    // Il costruttore deve rifiutare binding duplicati per lo stesso formato. 
    @Test
    void rifiutaBindingsDuplicatiPerFormato() {
        SpyExporter exporter1 = new SpyExporter();
        SpyExporter exporter2 = new SpyExporter();

        QualityScoreService qualityScoreService = new QualityScoreService();

        ReportService.ReportExporterBinding binding1 =
                new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter1);

        ReportService.ReportExporterBinding binding2 =
                new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter2);

        List<ReportService.ReportExporterBinding> bindings = List.of(binding1, binding2);

        Executable action = () -> new ReportService(qualityScoreService, bindings);
        assertThrows(IllegalArgumentException.class, action);
    }

    // Verifica che la generazione funzioni anche senza outputPath (nessuna esportazione).
    @Test
    void costruttoreDefaultLogger_eOverloadSenzaOutputPath_funzionano() {
        SpyExporter exporter = new SpyExporter(); 
        List<ReportService.ReportExporterBinding> bindings =
                List.of(new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter));

        // Richiama internamente new ConsoleLogger()
        ReportService service = new ReportService(new QualityScoreService(), bindings);

        Analisi analisi = creaAnalisiCompletataConUnaIssueWarningNoTODO();

        // Delega a generaReportQualita(analisi, formato, null)
        Report report = service.generaReportQualita(analisi, ReportFormat.JSON);

        assertNotNull(report);
        assertEquals(ReportFormat.JSON, report.getFormato());
        assertNull(exporter.lastReport, "Non deve esportare se outputPath è null");
        assertNull(exporter.lastPath, "Non deve esportare se outputPath è null");
    }

    @Test
    void seExporterLanciaRuntimeIlServizioLoggaErrorERilancia() {
        ThrowingExporter exporter = new ThrowingExporter();
        SpyLogger logger = new SpyLogger();

        ReportService service = new ReportService(
                new QualityScoreService(),
                List.of(new ReportService.ReportExporterBinding(ReportFormat.JSON, exporter)),
                logger
        );

        Analisi analisi = creaAnalisiCompletataConUnaIssueWarningNoTODO();
        String outPath = "/out.json";

        Executable action = () -> service.generaReportQualita(analisi, ReportFormat.JSON, outPath);
        RuntimeException ex = assertThrows(RuntimeException.class, action);

        assertEquals("boom", ex.getMessage());
        assertTrue(logger.hasLevel(LogLevel.ERROR), "Mi aspetto un ERROR se l'exporter lancia RuntimeException");

        String lastErr = logger.lastErrorMessageOrNull();
        assertNotNull(lastErr);
        assertTrue(lastErr.contains("Errore durante l'esportazione del report."),
                "Il log di errore deve contenere il messaggio previsto");
    }

    // ---------------- Helpers per Analisi ----------------

    // Crea un'analisi COMPLETATA senza issue.
    private Analisi creaAnalisiCompletataSenzaIssue() {
        Analisi analisi = new Analisi("A1", new Progetto("/p"));
        analisi.avvia();
        analisi.concludi(new RisultatoAnalisi(0, 0, Map.of(), 0));
        return analisi;
    }

    /**
     * Crea un'analisi COMPLETATA con 1 issue WARNING (RegolaNoTODO),
     * utile per testare score e classificazione.
     */
    private Analisi creaAnalisiCompletataConUnaIssueWarningNoTODO() {
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
        return analisi;
    }
}