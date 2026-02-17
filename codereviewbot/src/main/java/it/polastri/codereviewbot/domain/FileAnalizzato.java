package it.polastri.codereviewbot.domain;

/**
 * Rappresenta un file sorgente sottoposto ad analisi.
 * Tiene traccia dell'esito del parsing, dell'AST generato
 * e di eventuali errori di parsing.
 */

public class FileAnalizzato {
	
	private final String id; 
	private final FileSorgente fileSorgente;
	private EsitoParsing esitoParsing; 
	private AST ast;
	private String messaggioErroreParsing; 
	
	public FileAnalizzato(String id, FileSorgente fileSorgente) {
	    if (id == null) throw new IllegalArgumentException("Id non può essere null");
	    if (fileSorgente == null) throw new IllegalArgumentException("FileSorgente non può essere null");
	    
		this.id = id;
		this.fileSorgente = fileSorgente;
		this.esitoParsing = EsitoParsing.WAITING;
		this.ast = null;
	    this.messaggioErroreParsing = null;
	}
	
	public EsitoParsing getEsitoParsing() {
		return esitoParsing;
	}
	
	public AST getAst() {
		return ast;
	}
	
	public String getId() {
		return id;
	}
	
	public FileSorgente getFileSorgente() {
		return fileSorgente;
	} 
	
	public String getMessaggioErroreParsing() {
	    return messaggioErroreParsing;
	}
	
	// Imposta lo stato di parsing a ERROR e registra il messaggio di errore. L'AST viene invalidato.
	public void marcaParsingFallito(String messaggio) {
	    if (messaggio == null) throw new IllegalArgumentException("Messaggio di errore non può essere null");
	    
	    this.esitoParsing = EsitoParsing.ERROR;
	    this.ast = null;
	    this.messaggioErroreParsing = messaggio;
	}
	
	// Imposta l'AST generato dal parsing e marca il parsing come riuscito.
	public void impostaAST(AST ast) {
	    if (ast == null) throw new IllegalArgumentException("AST non può essere null");
	    
	    this.ast = ast;
	    this.esitoParsing = EsitoParsing.OK;
	    this.messaggioErroreParsing = null;
	}
	
	// Verifica se il file può essere analizzato in base al linguaggio supportato.
	public boolean isAnalizzabile() {
	    String est = fileSorgente.getEstensione();
	    return fileSorgente.getLinguaggio().supportaEstensione(est);
	}
	
    public boolean parsingRiuscito() {
        return esitoParsing == EsitoParsing.OK;
    }
}