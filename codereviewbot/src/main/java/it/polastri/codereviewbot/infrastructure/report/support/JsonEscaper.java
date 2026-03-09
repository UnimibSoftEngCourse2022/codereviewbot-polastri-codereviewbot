package it.polastri.codereviewbot.infrastructure.report.support;

/**
 * Utility statica per l'escaping di stringhe JSON.
 * Centralizza la logica per evitare duplicazione di codice.
 */

public final class JsonEscaper {

    private JsonEscaper() {
        // utility class
    }

    // Effettua l'escaping dei caratteri speciali secondo le regole JSON. 
    public static String escape(String s) {
        if (s == null) return "";

        StringBuilder sb = new StringBuilder(s.length() + 16);

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }

        return sb.toString();
    }
}