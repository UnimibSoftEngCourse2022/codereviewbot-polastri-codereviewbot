package it.polastri.codereviewbot.infrastructure.report;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import it.polastri.codereviewbot.domain.Report;
import it.polastri.codereviewbot.infrastructure.report.support.AbstractTextReportExporter;

/**
 * Exporter concreto che genera un report in formato PDF.
 * Il PDF è generato in modo minimale, senza librerie esterne,
 * utilizzando un writer interno che produce una singola pagina testuale.
 */

public class PdfReportExporter extends AbstractTextReportExporter implements ReportExporter {

    // Esporta il report in formato PDF nel percorso specificato.
    @Override
    public void esporta(Report report, String outputPath) {
        valida(report, outputPath);

        List<String> lines = renderPlainTextLines(report);
        byte[] pdf = MinimalPdfWriter.singlePageText(lines);

        scriviBytes(outputPath, pdf);
    }

    /**
     * Writer PDF minimale utilizzato per la generazione del documento.
     * Utility statica: non deve essere istanziata.
     */
    static class MinimalPdfWriter {

        private static final String NL = "\n";
        private static final String PDF_HEADER = "%PDF-1.4" + NL;

        private static final String BT = "BT" + NL;
        private static final String ET = "ET" + NL;
        private static final String FONT = "/F1 11 Tf" + NL;
        private static final String START_POS = "50 780 Td" + NL;

        private static final String TJ_SUFFIX = ") Tj" + NL;
        private static final String TD_SUFFIX = " Td" + NL;

        // Costruttore privato per impedire l'istanziazione della classe.
        private MinimalPdfWriter() {
            // non istanziabile
        }

        // Genera un PDF (una pagina) contenente le righe di testo fornite.
        static byte[] singlePageText(List<String> lines) {
            StringBuilder content = new StringBuilder();
            content.append(BT).append(FONT).append(START_POS);

            int lineHeight = 14;
            boolean first = true;

            for (String line : lines) {
                if (!first) {
                    content.append("0 -").append(lineHeight).append(TD_SUFFIX);
                }
                first = false;

                content.append('(')
                       .append(escapePdfString(line))
                       .append(TJ_SUFFIX);
            }

            content.append(ET);
            byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

            List<byte[]> objects = new ArrayList<>();
            objects.add(obj(1, "<< /Type /Catalog /Pages 2 0 R >>"));
            objects.add(obj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>"));
            objects.add(obj(3, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 4 0 R >> >> "
                    + "/Contents 5 0 R >> >>"));
            objects.add(obj(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));

            String streamHeader = "<< /Length " + contentBytes.length + " >>\nstream\n";
            String streamFooter = "\nendstream";
            objects.add(concat(
                    ("5 0 obj\n" + streamHeader).getBytes(StandardCharsets.ISO_8859_1),
                    contentBytes,
                    (streamFooter + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1)
            ));

            return buildPdf(objects);
        }

        // Costruisce il documento PDF completo e la tabella xref.
        private static byte[] buildPdf(List<byte[]> objects) {
            List<Integer> offsets = new ArrayList<>();
            ByteArrayBuilder out = new ByteArrayBuilder();

            out.append(PDF_HEADER.getBytes(StandardCharsets.ISO_8859_1));

            for (byte[] obj : objects) {
                offsets.add(out.size());
                out.append(obj);
            }

            int xrefStart = out.size();

            StringBuilder xref = new StringBuilder();
            xref.append("xref\n");
            xref.append("0 ").append(objects.size() + 1).append("\n");
            xref.append(pad10(0)).append(" 65535 f \n");
            for (int off : offsets) {
                xref.append(pad10(off)).append(" 00000 n \n");
            }
            out.append(xref.toString().getBytes(StandardCharsets.ISO_8859_1));

            String trailer =
                    "trailer\n" +
                    "<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n" +
                    "startxref\n" +
                    xrefStart + "\n" +
                    "%%EOF\n";
            out.append(trailer.getBytes(StandardCharsets.ISO_8859_1));

            return out.toByteArray();
        }

        /**
         * Converte un intero in una stringa lunga esattamente 10 caratteri,
         * con padding di zeri a sinistra.
         * Usato per le righe della tabella xref nel PDF.
         */
        private static String pad10(int n) {
            String s = Integer.toString(n);
            int missing = 10 - s.length();
            if (missing <= 0) return s;
            return "0".repeat(missing) + s;
        }

        // Escaping minimale per stringhe nel content stream PDF.
        private static String escapePdfString(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)");
        }

        // Crea un oggetto PDF numerato con body testuale.
        private static byte[] obj(int n, String body) {
            String s = n + " 0 obj\n" + body + "\nendobj\n";
            return s.getBytes(StandardCharsets.ISO_8859_1);
        }

        // Concatena più array di byte in un unico buffer.
        private static byte[] concat(byte[]... parts) {
            int len = 0;
            for (byte[] p : parts) len += p.length;
            byte[] out = new byte[len];
            int pos = 0;
            for (byte[] p : parts) {
                System.arraycopy(p, 0, out, pos, p.length);
                pos += p.length;
            }
            return out;
        }

        /**
         * Builder minimale per accumulare byte evitando dipendenze esterne.
         * Espande automaticamente il buffer interno quando necessario.
         */
        static class ByteArrayBuilder {
            private byte[] buf = new byte[4096];
            private int size = 0;

            // Ritorna numero di byte attualmente scritti nel buffer.
            int size() { return size; }

            // Appende un array di byte al buffer, espandendolo se serve.
            void append(byte[] b) {
                ensure(size + b.length);
                System.arraycopy(b, 0, buf, size, b.length);
                size += b.length;
            }

            // Ritorna array contenente esattamente i byte scritti (tagliato a size).
            byte[] toByteArray() {
                byte[] out = new byte[size];
                System.arraycopy(buf, 0, out, 0, size);
                return out;
            }

            // Garantisce una capacità minima del buffer interno.
            private void ensure(int cap) {
                if (cap <= buf.length) return;
                int n = buf.length;
                while (n < cap) n *= 2;
                byte[] nb = new byte[n];
                System.arraycopy(buf, 0, nb, 0, size);
                buf = nb;
            }
        }
    }
}