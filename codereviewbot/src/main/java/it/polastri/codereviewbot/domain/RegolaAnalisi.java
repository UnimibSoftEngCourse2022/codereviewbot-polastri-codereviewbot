package it.polastri.codereviewbot.domain;

/**
 * Rappresenta una regola di analisi applicabile ai nodi dell'AST.
 * Ogni regola è caratterizzata da una severità e da una categoria
 * e può produrre una o più issue se violata.
 */

import java.util.List; 
import java.util.Collections; 

public class RegolaAnalisi {
	
	private final String id; 
	private final String descrizione; 
	private final Severita severita;
	private final Categoria categoria; 
	
	public RegolaAnalisi(String id, String descrizione, Severita severita, Categoria categoria) {
	    if (id == null) throw new IllegalArgumentException("Id regola non può essere null");
	    if (descrizione == null) throw new IllegalArgumentException("Descrizione non può essere null");
	    if (severita == null) throw new IllegalArgumentException("Severità non può essere null");
	    if (categoria == null) throw new IllegalArgumentException("Categoria non può essere null");
		
		this.id = id; 
		this.descrizione = descrizione; 
		this.severita = severita; 
		this.categoria = categoria; 
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
    
    // Applica la regola a un nodo dell'AST. Se la regola è violata, produce una o più issue associate al file analizzato.
    public List<Issue> applica(NodoAST nodo, FileAnalizzato fileAnalizzato) {
        if (nodo == null) throw new IllegalArgumentException("NodoAST non può essere null");
        if (fileAnalizzato == null) throw new IllegalArgumentException("FileAnalizzato non può essere null");

        if (nodo.getTipoNodo().equalsIgnoreCase(id)) {
            Issue issue = new Issue(fileAnalizzato, nodo.getLinea(), this, "Violazione della regola " + id + " sul nodo di tipo " 
            		+ nodo.getTipoNodo());
            return List.of(issue);
        }

        return Collections.emptyList();
    }
}