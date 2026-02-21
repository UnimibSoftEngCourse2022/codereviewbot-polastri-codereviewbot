package it.polastri.codereviewbot.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.List;

class FileSorgenteTest {

	// getEstensione() deve restituire l'estensione in minuscolo (con il punto)
	// oppure una stringa vuota se il nome del file non ha estensione valida
    @Test
    void estensioneInMinuscoloOPresenteVuota() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        FileSorgente f1 = new FileSorgente("Main.JAVA", "/p/Main.JAVA", java, "class Main{}");
        assertEquals(".java", f1.getEstensione());

        FileSorgente f2 = new FileSorgente("README", "/p/README", java, "x");
        assertEquals("", f2.getEstensione());

        FileSorgente f3 = new FileSorgente("A.", "/p/A.", java, "x");
        assertEquals("", f3.getEstensione());
    }
}