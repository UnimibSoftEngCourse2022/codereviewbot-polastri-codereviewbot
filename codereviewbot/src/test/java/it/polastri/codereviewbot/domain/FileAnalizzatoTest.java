package it.polastri.codereviewbot.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.List;

class FileAnalizzatoTest {
	
	// Un file analizzato parte in stato WAITING senza AST né messaggi di errore
    @Test
    void constructor_shouldStartWaitingWithNoAstAndNoErrorMessage() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("Main.java", "/p/Main.java", java, "class Main{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        assertEquals(EsitoParsing.WAITING, fa.getEsitoParsing());
        assertNull(fa.getAst());
        assertNull(fa.getMessaggioErroreParsing());
        assertFalse(fa.parsingRiuscito());
    }
    
    // impostaAST() imposta lo stato OK e resetta eventuali errori
    @Test
    void impostaAST_shouldSetOkAndClearErrorMessage_andRejectNullAst() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("Main.java", "/p/Main.java", java, "class Main{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        assertThrows(IllegalArgumentException.class, () -> fa.impostaAST(null));

        AST ast = new AST();
        fa.impostaAST(ast);

        assertEquals(EsitoParsing.OK, fa.getEsitoParsing());
        assertEquals(ast, fa.getAst());
        assertNull(fa.getMessaggioErroreParsing());
        assertTrue(fa.parsingRiuscito());
    }
    
    // marcaParsingFallito() imposta lo stato ERROR e invalida l'AST
    @Test
    void marcaParsingFallito_shouldSetErrorAndNullAstAndStoreMessage() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente fs = new FileSorgente("Main.java", "/p/Main.java", java, "class Main{}");
        FileAnalizzato fa = new FileAnalizzato("F1", fs);

        AST ast = new AST();
        fa.impostaAST(ast);
        assertNotNull(fa.getAst());

        fa.marcaParsingFallito("Errore di parsing");

        assertEquals(EsitoParsing.ERROR, fa.getEsitoParsing());
        assertNull(fa.getAst());
        assertEquals("Errore di parsing", fa.getMessaggioErroreParsing());
        assertFalse(fa.parsingRiuscito());
    }

    // Un file è analizzabile solo se il linguaggio supporta la sua estensione
    @Test
    void isAnalizzabile_shouldDependOnLinguaggioExtensions() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));

        FileSorgente ok = new FileSorgente("A.java", "/p/A.java", java, "class A{}");
        FileAnalizzato faOk = new FileAnalizzato("F1", ok);
        assertTrue(faOk.isAnalizzabile());

        FileSorgente no = new FileSorgente("A.py", "/p/A.py", java, "print('x')");
        FileAnalizzato faNo = new FileAnalizzato("F2", no);
        assertFalse(faNo.isAnalizzabile());
    }
}