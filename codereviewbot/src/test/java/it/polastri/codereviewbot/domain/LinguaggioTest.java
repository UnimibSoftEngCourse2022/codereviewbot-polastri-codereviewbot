package it.polastri.codereviewbot.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.List;

class LinguaggioTest {
	
    // supportaEstensione() restituisce true solo per estensioni supportate
    @Test
    void supportaEstensioneRestituisceTrueSoloSeSupportata() {
        Linguaggio java = new Linguaggio("Java", List.of(".java", ".jav"));
       
        List<String> estensioni = java.getEstensioni();
        assertThrows(UnsupportedOperationException.class, () -> estensioni.add(".py"));
    }

    // La lista delle estensioni non deve essere modificabile dall'esterno 
    @Test
    void listaEstensioniNonModificabile() {
        Linguaggio java = new Linguaggio("Java", List.of(".java"));
        
        // Recupero la lista una sola volta
        List<String> estensioni = java.getEstensioni();
        // La modifica deve essere rifiutata
        assertThrows(UnsupportedOperationException.class, () -> estensioni.add(".py"));
    }
}