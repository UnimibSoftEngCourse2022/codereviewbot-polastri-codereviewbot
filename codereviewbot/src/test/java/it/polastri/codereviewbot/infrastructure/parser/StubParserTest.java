package it.polastri.codereviewbot.infrastructure.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.polastri.codereviewbot.domain.AST;
import it.polastri.codereviewbot.domain.FileSorgente;
import it.polastri.codereviewbot.domain.Linguaggio;

class StubParserTest {

    // Il parser stub deve creare un nodo per ogni riga del contenuto
    @Test
    void creaUnNodoPerOgniRiga() {
        Parser parser = new StubParser();

        Linguaggio javaLang = new Linguaggio("Java", List.of(".java"));
        String contenuto = "riga1\nriga2\nriga3";
        FileSorgente fs = new FileSorgente("A.java", "/p/A.java", javaLang, contenuto);

        AST ast = parser.parse(fs);

        assertEquals(3, ast.getNodiRilevanti().size());
        assertEquals(1, ast.getNodiRilevanti().get(0).getLinea());
        assertEquals("riga1", ast.getNodiRilevanti().get(0).getValore());
        assertEquals(3, ast.getNodiRilevanti().get(2).getLinea());
    }

    // Il parser deve rifiutare input null
    @Test
    void lanciaEccezioneSeFileNullo() {
        Parser parser = new StubParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }
}