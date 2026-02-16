package it.polastri.codereviewbot.domain;

public class RegolaAnalisi {
	
	private final String id; 
	private final String descrizione; 
	private final Severita severita;
	private final Categoria categoria; 
	
	public RegolaAnalisi(String id, String descrizione, Severita severita, Categoria categoria) {
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

	public Severita getSeverità() {
		return severita;
	}

	public Categoria getCategoria() {
		return categoria;
	}
	
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
}
