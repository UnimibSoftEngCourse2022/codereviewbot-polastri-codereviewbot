package it.polastri.codereviewbot.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.polastri.codereviewbot.domain.*;
import it.polastri.codereviewbot.domain.rules.RegolaNoSystemOutPrintln;
import it.polastri.codereviewbot.domain.rules.RegolaNoTODO;
import it.polastri.codereviewbot.infrastructure.loader.ProjectLoader;
import it.polastri.codereviewbot.infrastructure.parser.Parser;
import it.polastri.codereviewbot.infrastructure.parser.StubParser;

class AnalisiServiceTest {
	
	// Il costruttore rifiuta dipendenze nulle
	@Test
	void costruttoreRifiutaDipendenzeNulle() {
	    ProjectLoader loader = path -> new Progetto("/p");
	    Parser parser = new StubParser();
	    List<RegolaAnalisi> regoleVuote = List.of();

	    assertThrows(NullPointerException.class, () -> new AnalisiService(null, parser, regoleVuote));
	    assertThrows(NullPointerException.class, () -> new AnalisiService(loader, null, regoleVuote));
	    assertThrows(NullPointerException.class, () -> new AnalisiService(loader, parser, null));
	}

    // Esegue analisi, applica regole e conclude l'analisi con un risultato
    @Test
    void esegueAnalisiEClassificaIssue() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente a = new FileSorgente(
                "A.java",
                "/p/A.java",
                java,
                "System.out.println(\"x\");\n// TODO: fix\nfine"
        );

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(a);

        ProjectLoader loaderFinto = path -> progetto;
        Parser parser = new StubParser(); // non restituisce null

        AnalisiService service = new AnalisiService(
                loaderFinto,
                parser,
                List.of(new RegolaNoTODO(), new RegolaNoSystemOutPrintln())
        );

        Analisi analisi = service.eseguiAnalisi("/p");

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertNotNull(analisi.getRisultato());

        // 2 issue attese (println + TODO)
        assertEquals(2, analisi.getIssues().size());

        // 1 file analizzato
        assertEquals(1, analisi.getFileAnalizzati().size());
        assertEquals(EsitoParsing.OK, analisi.getFileAnalizzati().get(0).getEsitoParsing());
        assertTrue(analisi.getFileAnalizzati().get(0).parsingRiuscito());
    }
    
    // Se un file non è analizzabile, viene ignorato
    @Test
    void ignoraFileNonAnalizzabile() {
        Linguaggio soloTxt = new Linguaggio("TXT", List.of(".txt"));
        FileSorgente f = new FileSorgente("A.java", "/p/A.java", soloTxt, "System.out.println(\"x\");");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(f);

        ProjectLoader loaderFinto = path -> progetto;

        // Contatore chiamate al parser: deve restare 0
        final int[] chiamateParser = {0};
        Parser parser = file -> {
            chiamateParser[0]++;
            return null;
        };

        AnalisiService service = new AnalisiService(loaderFinto, parser, List.of(new RegolaNoSystemOutPrintln()));

        Analisi analisi = service.eseguiAnalisi("/p");

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertNotNull(analisi.getRisultato());
        assertEquals(1, analisi.getFileAnalizzati().size());
        assertEquals(0, analisi.getIssues().size());
        assertEquals(0, chiamateParser[0], "Il parser non deve essere invocato per file non analizzabili");
    }
    
    // Se il parser restituisce AST null, il service non applica regole e non produce issue
    @Test
    void nonApplicaRegoleSeParserRestituisceAstNull() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente f = new FileSorgente(
                "A.java",
                "/p/A.java",
                java,
                "System.out.println(\"x\");\n// TODO: fix"
        );

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(f);

        ProjectLoader loaderFinto = path -> progetto;

        // AST null -> deve scattare il return in analizzaFile
        Parser parserCheRitornaNull = file -> null;

        AnalisiService service = new AnalisiService(
                loaderFinto,
                parserCheRitornaNull,
                List.of(new RegolaNoTODO(), new RegolaNoSystemOutPrintln())
        );

        Analisi analisi = service.eseguiAnalisi("/p");

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertNotNull(analisi.getRisultato());

        // Nessuna issue perché non si applicano regole senza AST valido
        assertEquals(0, analisi.getIssues().size());
        assertEquals(1, analisi.getFileAnalizzati().size());
    }

    // Se il parsing fallisce su un file, lo marca in ERROR e continua sugli altri
    @Test
    void continuaSeUnFileFallisceParsing() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente ok = new FileSorgente("OK.java", "/p/OK.java", java, "System.out.println(\"x\");");
        FileSorgente bad = new FileSorgente("BAD.java", "/p/BAD.java", java, "qualcosa");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(ok);
        progetto.aggiungiFileSorgente(bad);

        ProjectLoader loaderFinto = path -> progetto;

        Parser parserCheFallisceSoloSuBad = new Parser() {
            private final Parser okParser = new StubParser();

            @Override
            public AST parse(FileSorgente file) {
                if ("BAD.java".equals(file.getNome())) {
                    throw new RuntimeException("boom");
                }
                return okParser.parse(file); // mai null
            }
        };

        AnalisiService service = new AnalisiService(
                loaderFinto,
                parserCheFallisceSoloSuBad,
                List.of(new RegolaNoSystemOutPrintln())
        );

        Analisi analisi = service.eseguiAnalisi("/p");

        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());

        // Solo OK.java produce 1 issue (println)
        assertEquals(1, analisi.getIssues().size());

        FileAnalizzato faOk = analisi.getFileAnalizzati().stream()
                .filter(fa -> "OK.java".equals(fa.getFileSorgente().getNome()))
                .findFirst().orElseThrow();

        FileAnalizzato faBad = analisi.getFileAnalizzati().stream()
                .filter(fa -> "BAD.java".equals(fa.getFileSorgente().getNome()))
                .findFirst().orElseThrow();

        assertEquals(EsitoParsing.OK, faOk.getEsitoParsing());
        assertEquals(EsitoParsing.ERROR, faBad.getEsitoParsing());
        assertNotNull(faBad.getMessaggioErroreParsing());
    }

    // Il service rifiuta projectPath null
    @Test
    void lanciaEccezioneSeProjectPathNullo() {
        ProjectLoader loaderFinto = path -> new Progetto("/p");
        Parser parser = new StubParser();

        AnalisiService service = new AnalisiService(loaderFinto, parser, List.of());

        assertThrows(NullPointerException.class, () -> service.eseguiAnalisi(null));
    }
    
 // Ogni esecuzione genera una nuova istanza di Analisi, indipendentemente dalle precedenti
    @Test
    void generaUnaNuovaAnalisiOgniVolta() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente a = new FileSorgente("A.java", "/p/A.java", java, "class A {}");
        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(a);

        ProjectLoader loaderFinto = path -> progetto;
        Parser parser = new StubParser();

        AnalisiService service = new AnalisiService(loaderFinto, parser, List.of());

        Analisi analisi1 = service.eseguiAnalisi("/p");
        Analisi analisi2 = service.eseguiAnalisi("/p");

        assertNotSame(analisi1, analisi2, "Ogni esecuzione deve restituire una nuova istanza di Analisi");
        assertNotEquals(analisi1.getId(), analisi2.getId(), "Ogni Analisi deve avere un id diverso (RD4)");
    }
    
    // Se avviene un errore non gestito dal singolo file, il servizio deve marcare l'analisi come fallita
    @Test
    void marcaAnalisiFallitaSeErroreGraveAvvieneNelProcessoGlobale() {
        // Progetto che lancia eccezione quando si tenta di ottenere i file sorgenti.
        Progetto progettoCheEsplode = new Progetto("/p") {
            @Override
            public List<FileSorgente> getFileSorgenti() {
                throw new RuntimeException("Errore grave");
            }
        };

        ProjectLoader loaderFinto = path -> progettoCheEsplode;
        Parser parser = new StubParser();

        AnalisiService service = new AnalisiService(loaderFinto, parser, List.of());

        Analisi analisi = service.eseguiAnalisi("/p");

        // Entra nel catch globale
        assertEquals(StatoAnalisi.FALLITA, analisi.getStatoAnalisi());
        assertNull(analisi.getRisultato(), "Se l'analisi fallisce non deve esserci un risultato");
    }
    
    // Le regole non vengono applicate se AST vuoto
    @Test
    void nonApplicaRegoleSeAstVuoto() {
        // File java analizzabile
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente f = new FileSorgente("A.java", "/p/A.java", java, "System.out.println(\"x\");\n// TODO: fix");

        Progetto progetto = new Progetto("/p");
        progetto.aggiungiFileSorgente(f);

        ProjectLoader loaderFinto = path -> progetto;

        // Parser che restituisce un AST valido ma vuoto 
        Parser parserCheRitornaAstVuoto = file -> new AST();

        AnalisiService service = new AnalisiService(loaderFinto, parserCheRitornaAstVuoto,
                List.of(new RegolaNoTODO(), new RegolaNoSystemOutPrintln()));

        Analisi analisi = service.eseguiAnalisi("/p");

        // L'analisi termina correttamente ma senza issue, perché non si applicano regole con AST vuoto
        assertEquals(StatoAnalisi.COMPLETATA, analisi.getStatoAnalisi());
        assertNotNull(analisi.getRisultato());
        assertTrue(analisi.getIssues().isEmpty(), "Con AST vuoto non devono essere applicate regole");
        assertEquals(1, analisi.getFileAnalizzati().size());
    }
}