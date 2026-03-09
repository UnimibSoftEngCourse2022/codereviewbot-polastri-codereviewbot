package it.polastri.codereviewbot.infrastructure.report;

import it.polastri.codereviewbot.domain.Report;
import it.polastri.codereviewbot.infrastructure.report.support.AbstractTextReportExporter;

/**
 * Exporter concreto che genera un report in formato HTML.
 * Utilizza la rappresentazione testuale fornita dalla classe astratta
 * e la incapsula in una pagina HTML, che funge anche da frontend per l'utente.
 */

public class HtmlReportExporter extends AbstractTextReportExporter implements ReportExporter {

	// Esporta il report in formato HTML.
    @Override
    public void esporta(Report report, String outputPath) {
        valida(report, outputPath);

        String plain = renderPlainText(report);

        String html =
            "<!doctype html>\n" +
            "<html lang=\"it\">\n<head>\n" +
            "  <meta charset=\"utf-8\"/>\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n" +
            "  <title>CodeReviewBot - Report Qualità</title>\n" +
            "  <style>\n" +
            "    body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;margin:24px;}\n" +
            "    pre{background:#f7f7f7;padding:12px;border:1px solid #e0e0e0;white-space:pre-wrap;}\n" +
            "  </style>\n" +
            "</head>\n<body>\n" +
            "  <h1>CodeReviewBot - Report Qualità</h1>\n" +
            "  <pre>" + escapeHtml(plain) + "</pre>\n" +
            "</body>\n</html>\n";

        scriviUtf8(outputPath, html);
    }
}