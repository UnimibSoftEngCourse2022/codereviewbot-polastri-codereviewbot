package it.polastri.codereviewbot.infrastructure.report;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.function.Executable;

import it.polastri.codereviewbot.application.exception.ReportExportException;
import it.polastri.codereviewbot.domain.*;

class HtmlReportExporterTest {

    @TempDir
    Path tempDir;

    // Se il report è null deve essere lanciata un'eccezione.
    @Test
    void esportaLanciaEccezioneSeReportNullo() {
        HtmlReportExporter exporter = new HtmlReportExporter();
        Report report = null;
        String out = "out.html";

        Executable action = () -> exporter.esporta(report, out);
        assertThrows(IllegalArgumentException.class, action);
    }

    // Se il percorso di outputh non è valido deve lanciare un'eccezione.
    @Test
    void esportaLanciaEccezioneSeOutputPathNonValido() {
        HtmlReportExporter exporter = new HtmlReportExporter();
        Report report = creaReportConUnaIssue();

        String nullPath = null;
        String emptyPath = "";
        String blankPath = "   ";

        assertThrows(IllegalArgumentException.class, () -> exporter.esporta(report, nullPath));
        assertThrows(IllegalArgumentException.class, () -> exporter.esporta(report, emptyPath));
        assertThrows(IllegalArgumentException.class, () -> exporter.esporta(report, blankPath));
    }

    // Il file HTML deve essere creato e deve contenere i dati principali del report. 
    @Test
    void esportaScriveHtmlEContienePartiPrincipali() throws IOException {
        HtmlReportExporter exporter = new HtmlReportExporter();
        Report report = creaReportConUnaIssue();

        Path outFile = tempDir.resolve("reports").resolve("report.html");
        String outPath = outFile.toString();

        exporter.esporta(report, outPath);

        assertTrue(Files.exists(outFile));

        String html = Files.readString(outFile, StandardCharsets.UTF_8);
        assertTrue(html.contains("<!doctype html>"));
        assertTrue(html.contains("CodeReviewBot - Report Qualità"));
        assertTrue(html.contains("Report ID: " + report.getId()));

        assertTrue(html.contains("&lt;tag&gt;"));
        assertTrue(html.contains("&amp;"));
        assertTrue(html.contains("&quot;"));
    }

    // Un errore di I/O deve essere correttamente propagato come ReportExportException.
    @Test
    void esportaLanciaReportExportExceptionSeErroreIO() throws IOException {
        HtmlReportExporter exporter = new HtmlReportExporter();
        Report report = creaReportConUnaIssue();

        Path dir = tempDir.resolve("outDir");
        Files.createDirectories(dir);
        String dirPath = dir.toString();

        Executable action = () -> exporter.esporta(report, dirPath);
        ReportExportException ex = assertThrows(ReportExportException.class, action);

        assertNotNull(ex.getCause());
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

        return Report.creaDa(analisi, ReportFormat.HTML, 90);
    }
}