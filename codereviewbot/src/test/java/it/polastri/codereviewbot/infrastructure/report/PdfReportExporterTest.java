package it.polastri.codereviewbot.infrastructure.report;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.function.Executable;

import it.polastri.codereviewbot.application.exception.ReportExportException;
import it.polastri.codereviewbot.domain.*;

class PdfReportExporterTest {

    private static final String PDF_SIGNATURE = "%PDF-";

    @TempDir
    Path tempDir;

    // Se il report è null deve essere lanciata un'eccezione. 
    @Test
    void esportaLanciaEccezioneSeReportNullo() {
        PdfReportExporter exporter = new PdfReportExporter();
        Report report = null;
        String outPath = "out.pdf";

        Executable action = () -> exporter.esporta(report, outPath);
        assertThrows(IllegalArgumentException.class, action);
    }

    // Se il percorso di output non è valido deve essere lanciata un'eccezione. 
    @Test
    void esportaLanciaEccezioneSeOutputPathNonValido() {
        PdfReportExporter exporter = new PdfReportExporter();
        Report report = creaReportConUnaIssue();

        String nullPath = null;
        String emptyPath = "";
        String blankPath = "   ";

        assertThrows(IllegalArgumentException.class, () -> exporter.esporta(report, nullPath));
        assertThrows(IllegalArgumentException.class, () -> exporter.esporta(report, emptyPath));
        assertThrows(IllegalArgumentException.class, () -> exporter.esporta(report, blankPath));
    }

    // Il file PDF deve essere creato e deve contenere la signature "%PDF-". 
    @Test
    void esportaScrivePdfConHeader() throws IOException {
        PdfReportExporter exporter = new PdfReportExporter();
        Report report = creaReportConUnaIssue();

        Path outFile = tempDir.resolve("reports").resolve("report.pdf");
        String outPath = outFile.toString();

        exporter.esporta(report, outPath);

        assertTrue(Files.exists(outFile));

        byte[] bytes = Files.readAllBytes(outFile);
        assertTrue(bytes.length > 200);

        String header = new String(bytes, 0, Math.min(bytes.length, 8), StandardCharsets.ISO_8859_1);
        assertTrue(header.startsWith(PDF_SIGNATURE));
    }

    // Un errore di I/O deve essere propagato come ReportExportException. 
    @Test
    void esportaLanciaReportExportExceptionSeErroreIO() throws IOException {
        PdfReportExporter exporter = new PdfReportExporter();
        Report report = creaReportConUnaIssue();

        Path dir = tempDir.resolve("outDir");
        Files.createDirectories(dir);
        String dirPath = dir.toString(); // scrivere su directory -> errore IO

        Executable action = () -> exporter.esporta(report, dirPath);
        ReportExportException ex = assertThrows(ReportExportException.class, action);

        assertNotNull(ex.getCause());
    }

    // Verifica direttamente il writer PDF minimale invocandolo staticamente. 
    @Test
    void minimalPdfWriterGeneraPdfValido_chiamatoDirettamente() {

        List<String> lines = List.of(
                "Titolo report",
                "Riga 2 (con parentesi) (test)",
                "Riga 3 con backslash \\ ok"
        );

        byte[] pdfBytes = PdfReportExporter.MinimalPdfWriter.singlePageText(lines);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 200);

        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 8), StandardCharsets.ISO_8859_1);
        assertTrue(header.startsWith(PDF_SIGNATURE));
    }

    // Forza la crescita del buffer interno del writer.
    @Test
    void minimalPdfWriterForzaEspansioneBuffer_perCoperturaEnsure() {

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            lines.add("RIGA-" + i + " " + "X".repeat(40) + " (abc) \\ fine");
        }

        byte[] pdfBytes = PdfReportExporter.MinimalPdfWriter.singlePageText(lines);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 4096, "Il PDF deve superare 4096 bytes per attivare ensure()");
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 8), StandardCharsets.ISO_8859_1);
        assertTrue(header.startsWith(PDF_SIGNATURE));
    }

    // ---- Builder di un report minimo valido ----

    private Report creaReportConUnaIssue() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente file = new FileSorgente("A.java", "/p/A.java", java, "System.out.println(\"x\");");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(file);

        Analisi analisi = new Analisi("AN-TEST", progetto);
        analisi.avvia();

        FileAnalizzato fa = new FileAnalizzato("FA-TEST", file);
        analisi.aggiungiFileAnalizzato(fa);

        RegolaAnalisi regola = new RegolaAnalisi(
                "R-001",
                "Evita System.out.println",
                Severita.WARNING,
                Categoria.STILE
        );

        Issue issue = new Issue(fa, 1, regola, "Evita println() e usa logger (info)");

        analisi.registraIssue(issue);
        analisi.concludi(RisultatoAnalisi.creaDa(analisi.getIssues(), analisi.getFileAnalizzati()));

        return Report.creaDa(analisi, ReportFormat.PDF, 90);
    }
}