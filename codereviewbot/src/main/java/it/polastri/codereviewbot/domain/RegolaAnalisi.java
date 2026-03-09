package it.polastri.codereviewbot.domain;

import java.util.List;
import java.util.Objects;
import java.util.Collections; 

/**
 * Rappresenta una regola di analisi applicabile ai nodi dell'AST.
 * Ogni regola è caratterizzata da una severità e da una categoria
 * e può produrre una o più issue se violata.
 */

public class RegolaAnalisi {
	
	private final String id; 
	private final String descrizione; 
	private final Severita severita;
	private final Categoria categoria; 
	
	public RegolaAnalisi(String id, String descrizione, Severita severita, Categoria categoria) {
        this.id = Objects.requireNonNull(id, "Id regola non può essere null");
        this.descrizione = Objects.requireNonNull(descrizione, "Descrizione non può essere null");
        this.severita = Objects.requireNonNull(severita, "Severità non può essere null");
        this.categoria = Objects.requireNonNull(categoria, "Categoria non può essere null");
	}
	
	public String getId() {
		return id;
	}

	public String getDescrizione() {
		return descrizione;
	}

	public Severita getSeverita() {
		return severita;
	}

	public Categoria getCategoria() {
		return categoria;
	}
	
	// Restituisce una descrizione testuale completa della regola.
    public String descrizioneCompleta() {
        return "Regola " + id +
               ", categoria " + categoria +
               ", severità " + severita +
               ". Descrizione: " + descrizione;
    }

    @Override
    public String toString() {
        return id;
    }
    
    /**
     * Applica la regola a un nodo dell'AST.
     * La classe base non impone nessuna logica: restituisce lista vuota.
     * Le regole concrete sovrascrivono questo metodo.
     */
    public List<Issue> applica(NodoAST nodo, FileAnalizzato fileAnalizzato) {
        Objects.requireNonNull(nodo, "NodoAST non può essere null");
        Objects.requireNonNull(fileAnalizzato, "FileAnalizzato non può essere null");
        
        return Collections.emptyList();
    }
}