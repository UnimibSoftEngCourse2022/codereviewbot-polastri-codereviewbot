package it.polastri.codereviewbot.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RisultatoAnalisiTest {

    // Il costruttore rifiuta metriche null e conteggi negativi
    @Test
    void rifiutaMetricheNullECampiNegativi() {
        assertThrows(NullPointerException.class,
                () -> new RisultatoAnalisi(0, 0, null, 0));

        assertThrows(IllegalArgumentException.class,
                () -> new RisultatoAnalisi(-1, 0, Map.of(), 0));

        assertThrows(IllegalArgumentException.class,
                () -> new RisultatoAnalisi(0, -1, Map.of(), 0));

        assertThrows(IllegalArgumentException.class,
                () -> new RisultatoAnalisi(0, 0, Map.of(), -1));
    }

    // Le metriche salvate nel risultato devono essere non modificabili
    @Test
    void metricheNonModificabiliDallEsterno() {
        RisultatoAnalisi r = new RisultatoAnalisi(0, 0, Map.of("files_analizzati", 1), 0);
        assertThrows(UnsupportedOperationException.class,
                () -> r.getMetrichePreliminari().put("x", 1));
    }

    // creaDa() calcola correttamente conteggi e metriche a partire da issue e file analizzati
    @Test
    void creaDaCalcolaConteggiEMetricheBase() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente fs1 = new FileSorgente("A.java", "/p/A.java", java, "x");
        FileSorgente fs2 = new FileSorgente("B.java", "/p/B.java", java, "x");

        FileAnalizzato fa1 = new FileAnalizzato("F1", fs1);
        FileAnalizzato fa2 = new FileAnalizzato("F2", fs2);

        // fa1 parsing OK, fa2 parsing ERROR
        fa1.impostaAST(new AST());
        fa2.marcaParsingFallito("errore");

        RegolaAnalisi rErr = new RegolaAnalisi("R_ERR", "desc", Severita.ERROR, Categoria.COMPLESSITA);
        RegolaAnalisi rWarn = new RegolaAnalisi("R_WARN", "desc", Severita.WARNING, Categoria.STILE);

        Issue i1 = new Issue(fa1, 1, rErr, "e1");
        Issue i2 = new Issue(fa1, 2, rWarn, "w1");
        Issue i3 = new Issue(fa2, 3, rWarn, "w2");

        RisultatoAnalisi res = RisultatoAnalisi.creaDa(List.of(i1, i2, i3), List.of(fa1, fa2));

        assertEquals(1, res.getNumeroErrori());
        assertEquals(2, res.getNumeroWarning());
        assertEquals(3, res.getNumeroIssueTotali());
        assertNotNull(res.getGeneratoIl());

        Map<String, Integer> m = res.getMetrichePreliminari();
        assertEquals(2, m.get("files_analizzati"));
        assertEquals(1, m.get("files_parsing_ok"));
        assertEquals(1, m.get("files_parsing_error"));

        assertEquals(3, m.get("issues_totali"));
        assertEquals(1, m.get("issues_error"));
        assertEquals(2, m.get("issues_warning"));
        assertEquals(2, m.get("issues_per_file_max"));
    }

    // creaDa() rifiuta liste null
    @Test
    void creaDaRifiutaListeNull() {
        assertThrows(NullPointerException.class,
                () -> RisultatoAnalisi.creaDa(null, List.of()));

        assertThrows(NullPointerException.class,
                () -> RisultatoAnalisi.creaDa(List.of(), null));
    }
}