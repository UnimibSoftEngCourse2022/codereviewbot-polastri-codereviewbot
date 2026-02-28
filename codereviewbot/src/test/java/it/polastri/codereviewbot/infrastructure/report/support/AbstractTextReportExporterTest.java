package it.polastri.codereviewbot.infrastructure.report.support;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import it.polastri.codereviewbot.domain.*;

class AbstractTextReportExporterTest {

    static class TestableExporter extends AbstractTextReportExporter {
        String callRenderPlainText(Report report) { return renderPlainText(report); }
        List<String> callRenderPlainTextLines(Report report) { return renderPlainTextLines(report); }
        String callEscapeHtml(String s) { return escapeHtml(s); }
        String callEscapeJson(String s) { return escapeJson(s); }
        void callValidate(Report report, String outputPath) { valida(report, outputPath); }
    }

    // La validazione deve fallire se il report è null.
    @Test
    void validaeLanciaEccezioneSeReportNullo() {
        TestableExporter exp = new TestableExporter();
        Report report = null;
        String out = "out.txt";

        Executable action = () -> exp.callValidate(report, out);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, action);

        assertTrue(ex.getMessage().toLowerCase().contains("report"));
    }

    // La validazione deve fallire se il percorso di output non è valido.
    @Test
    void validaeLanciaEccezioneSeOutputPathNonValido() {
        TestableExporter exp = new TestableExporter();
        Report report = creaReportSenzaIssue();

        String nullPath = null;
        String emptyPath = "";
        String blankPath = "   ";

        assertThrows(IllegalArgumentException.class, () -> exp.callValidate(report, nullPath));
        assertThrows(IllegalArgumentException.class, () -> exp.callValidate(report, emptyPath));
        assertThrows(IllegalArgumentException.class, () -> exp.callValidate(report, blankPath));
    }

    // Il testo generato deve contenere tutte le sezioni principali del report.
    @Test
    void renderPlainTextContieneSezioniPrincipali() {
        TestableExporter exp = new TestableExporter();
        Report report = creaReportConUnaIssue();

        String text = exp.callRenderPlainText(report);

        assertTrue(text.contains("CodeReviewBot"));
        assertTrue(text.contains("Report ID: " + report.getId()));
        assertTrue(text.contains("Quality Score: " + report.getScoreQualita() + "/100"));
        assertTrue(text.contains("Analisi ID: " + report.getAnalisi().getId()));
        assertTrue(text.contains("Progetto: " + report.getAnalisi().getProgetto().getProjectPath()));
        assertTrue(text.contains("Classificazione Issue:"));
        assertTrue(text.contains("Dettaglio Issue:"));

        Issue issue = report.getAnalisi().getIssues().get(0);
        assertTrue(text.contains(issue.getRegola().getId()));
        assertTrue(text.contains(issue.getMessaggio()));
    }

    // Il rendering a righe deve produrre una lista non vuota.
    @Test
    void renderPlainTextLinesRestituisceListaNonVuota() {
        TestableExporter exp = new TestableExporter();
        Report report = creaReportConUnaIssue();

        List<String> lines = exp.callRenderPlainTextLines(report);

        assertNotNull(lines);
        assertFalse(lines.isEmpty());
        assertTrue(lines.get(0).contains("CodeReviewBot"));
    }

    // L'escaping dei caratteri speciali HTML deve essere corretto.
    @Test
    void escapeHtmlSostituisceCaratteriSpeciali() {
        TestableExporter exp = new TestableExporter();
        String in = "<tag attr=\"x\">& ' </tag>";

        String out = exp.callEscapeHtml(in);

        assertTrue(out.contains("&lt;"));
        assertTrue(out.contains("&gt;"));
        assertTrue(out.contains("&quot;"));
        assertTrue(out.contains("&amp;"));
        assertTrue(out.contains("&#39;"));
    }

    // L'escaping dei caratteri speciali JSON deve essere corretto.
    @Test
    void escapeJsonSostituisceVirgoletteBackslashENewline() {
        TestableExporter exp = new TestableExporter();
        String in = "a\"b\\c\nd";

        String out = exp.callEscapeJson(in);

        assertTrue(out.contains("\\\""));
        assertTrue(out.contains("\\\\"));
        assertTrue(out.contains("\\n"));
    }

    // ---- builders ----
    private Report creaReportSenzaIssue() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente file = new FileSorgente("A.java", "/p/A.java", java, "class A {}");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(file);

        Analisi analisi = new Analisi("AN-TEST", progetto);
        analisi.avvia();
        analisi.concludi(RisultatoAnalisi.creaDa(List.of(), List.of()));

        ReportFormat formato = ReportFormat.values()[0];
        return Report.creaDa(analisi, formato, 100);
    }

    private Report creaReportConUnaIssue() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente file = new FileSorgente("A.java", "/p/A.java", java, "System.out.println(\"x\");");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(file);

        Analisi analisi = new Analisi("AN-TEST", progetto);
        analisi.avvia();

        FileAnalizzato fa = new FileAnalizzato("FA-TEST", file);
        analisi.aggiungiFileAnalizzato(fa);

        RegolaAnalisi regola = new RegolaAnalisi("R-001", "Evita System.out.println", Severita.WARNING, Categoria.STILE);
        Issue issue = new Issue(fa, 1, regola, "Evita System.out \"x\" <tag> & altro");

        analisi.registraIssue(issue);
        analisi.concludi(RisultatoAnalisi.creaDa(analisi.getIssues(), analisi.getFileAnalizzati()));

        ReportFormat formato = ReportFormat.values()[0];
        return Report.creaDa(analisi, formato, 90);
    }
}