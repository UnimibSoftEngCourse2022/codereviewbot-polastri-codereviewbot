package it.polastri.codereviewbot.infrastructure.report;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import it.polastri.codereviewbot.application.exception.ReportExportException;
import it.polastri.codereviewbot.domain.*;

/**
 * Uso @TempDir per scrivere file in una cartella temporanea gestita da JUnit.
 */

class TextReportExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void esporta_lanciaEccezioneSeReportNullo() {
        // Verifica input, report obbligatorio
        TextReportExporter exporter = new TextReportExporter();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> exporter.esporta(null, "out.txt")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("report"));
    }

    @Test
    void esporta_lanciaEccezioneSeOutputPathNonValido() {
        // Verifica input, outputPath deve essere valorizzato
        TextReportExporter exporter = new TextReportExporter();
        Report report = creaReportSenzaIssue();

        assertThrows(IllegalArgumentException.class, () -> exporter.esporta(report, null));
        assertThrows(IllegalArgumentException.class, () -> exporter.esporta(report, ""));
        assertThrows(IllegalArgumentException.class, () -> exporter.esporta(report, "   "));
    }

    @Test
    void esporta_scriveFileEContieneCampiPrincipali() throws IOException {
        // Esporta un report con una issue e verifica creazione file e presenza delle parti principali del contenuto
        TextReportExporter exporter = new TextReportExporter();
        Report report = creaReportConUnaIssue();

        Path out = tempDir.resolve("reports").resolve("report.txt");

        exporter.esporta(report, out.toString());

        assertTrue(Files.exists(out), "Il file di output deve essere creato");

        String content = Files.readString(out, StandardCharsets.UTF_8);

        // Controlli 
        assertTrue(content.contains("=== CodeReviewBot - Report Qualità ==="));
        assertTrue(content.contains("Report ID: " + report.getId()));
        assertTrue(content.contains("Formato richiesto: " + report.getFormato()));
        assertTrue(content.contains("Quality Score: " + report.getScoreQualita() + "/100"));

        assertTrue(content.contains("Analisi ID: " + report.getAnalisi().getId()));
        assertTrue(content.contains("Progetto: " + report.getAnalisi().getProgetto().getProjectPath()));

        assertTrue(content.contains("--- Classificazione Issue ---"));
        assertTrue(content.contains("--- Dettaglio Issue ---"));

        // Verifica che il dettaglio della issue sia stato scritto
        Issue issue = report.getAnalisi().getIssues().get(0);
        assertTrue(content.contains(issue.getFileAnalizzato().getFileSorgente().getPath() + ":" + issue.getRiga()));
        assertTrue(content.contains(issue.getRegola().getId()));
        assertTrue(content.contains(issue.getMessaggio()));
    }

    @Test
    void esporta_lanciaReportExportExceptionSeErroreIO() throws IOException {
        // Caso errore I/O: crea una directory e la passa come file di output
        TextReportExporter exporter = new TextReportExporter();
        Report report = creaReportSenzaIssue();

        Path dir = tempDir.resolve("outDir");
        Files.createDirectories(dir);
        assertTrue(Files.isDirectory(dir));
        
        String outputPath = dir.toString();

        ReportExportException ex = assertThrows(
                ReportExportException.class,
                () -> exporter.esporta(report, outputPath)
        );

        assertTrue(ex.getMessage().contains("Errore durante l'esportazione del report"));
        assertNotNull(ex.getCause(), "La causa originale (IOException) deve essere preservata");
    }

    // Crea un report valido senza issue
    private Report creaReportSenzaIssue() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente file = new FileSorgente("A.java", "/p/A.java", java, "class A {}");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(file);

        Analisi analisi = new Analisi("AN-TEST", progetto);
        analisi.avvia();
        analisi.concludi(RisultatoAnalisi.creaDa(List.of(), List.of()));
        
       // Non assume un formato specifico
        ReportFormat formato = ReportFormat.values()[0]; 
        return Report.creaDa(analisi, formato, 100);
    }

    // Crea un report valido con una issue. L'exporter deve stampare la classificazione e il dettaglio della issue
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

        Issue issue = new Issue(fa, 1, regola, "Evita System.out");

        analisi.registraIssue(issue);
        analisi.concludi(RisultatoAnalisi.creaDa(analisi.getIssues(), analisi.getFileAnalizzati()));

        ReportFormat formato = ReportFormat.values()[0];
        return Report.creaDa(analisi, formato, 90);
    }
}