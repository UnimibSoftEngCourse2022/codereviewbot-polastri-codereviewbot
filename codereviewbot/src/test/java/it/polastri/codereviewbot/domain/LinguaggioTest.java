package it.polastri.codereviewbot.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.List;

class LinguaggioTest {
	
    // supportaEstensione() restituisce true solo per estensioni supportate
    @Test
    void supportaEstensioneRestituisceTrueSoloSeSupportata() {
        Linguaggio java = new Linguaggio("Java", List.of(".java", ".jav"));
        assertTrue(java.supportaEstensione(".java"));
        assertFalse(java.supportaEstensione(".py"));
    }

    // La lista delle estensioni non deve essere modificabile dall'esterno 
    @Test
    void listaEstensioniNonModificabile() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        assertThrows(UnsupportedOperationException.class, () -> java.getEstensioni().add(".py"));
    }
}