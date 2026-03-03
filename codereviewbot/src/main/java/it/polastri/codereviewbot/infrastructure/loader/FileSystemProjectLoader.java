package it.polastri.codereviewbot.infrastructure.loader;

import it.polastri.codereviewbot.domain.FileSorgente;
import it.polastri.codereviewbot.domain.Linguaggio;
import it.polastri.codereviewbot.domain.Progetto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Implementazione di ProjectLoader che carica un progetto dal filesystem
 * locale: - valida il percorso del progetto - scansiona ricorsivamente la
 * directory - include solo file con estensione supportata - legge contenuto
 * file sorgente - costruisce l'oggetto Progetto con i relativi FileSorgente
 */

public class FileSystemProjectLoader implements ProjectLoader {

	private final List<Linguaggio> linguaggiSupportati;

	private static final Set<String> EXCLUDED_DIRS = Set.of("/target/", "/.git/", "/src/test/",
			"/vscode-codereviewbot/", "/node_modules/");

	// Lista dei linguaggi supportati dal sistema. Filtra i file durante la
	// scansione.
	public FileSystemProjectLoader() {
		this(List.of(new Linguaggio("Java", List.of(".java")), new Linguaggio("Python", List.of(".py")),
				new Linguaggio("C++", List.of(".cpp", ".cc", ".cxx", ".hpp", ".h"))));
	}

	public FileSystemProjectLoader(List<Linguaggio> linguaggiSupportati) {
		this.linguaggiSupportati = Objects.requireNonNull(linguaggiSupportati,
				"linguaggiSupportati non può essere null");
	}

	// Carica un progetto a partire da un percorso filesystem.
	@Override
	public Progetto caricaProgetto(String path) {
		if (path == null || path.isBlank()) {
			throw new IllegalArgumentException("path non può essere nullo o vuoto");
		}

		// verifica che il percorso esista e sia una directory
		Path root = Paths.get(path).toAbsolutePath().normalize();
		if (!Files.exists(root)) {
			throw new IllegalArgumentException("Il percorso non esiste: " + root);
		}
		if (!Files.isDirectory(root)) {
			throw new IllegalArgumentException("Il percorso non è una directory: " + root);
		}

		Progetto progetto = new Progetto(root.toString());

		// scansiona ricorsivamente i file
		try (var stream = Files.walk(root)) {
			stream.filter(Files::isRegularFile).filter(p -> !isExcluded(p)).forEach(filePath -> {
				String fileName = filePath.getFileName().toString();
				String ext = estraiEstensione(fileName);

				Linguaggio lang = risolviLinguaggio(ext);
				if (lang == null) {
					// considera solo linguaggi supportati
					return;
				}

				// legge il contenuto del file sorgente
				String contenuto;
				try {
					contenuto = Files.readString(filePath, StandardCharsets.UTF_8);
				} catch (IOException e) {
					throw new IllegalStateException("Impossibile leggere file: " + filePath, e);
				}

				FileSorgente fs = new FileSorgente(fileName, filePath.toAbsolutePath().normalize().toString(), lang,
						contenuto);

				progetto.aggiungiFileSorgente(fs);
			});
		} catch (IOException e) {
			throw new IllegalStateException("Errore durante la scansione del progetto: " + root, e);
		}

		return progetto;
	}

	// Determina il linguaggio associato a un'estensione di file.
	private Linguaggio risolviLinguaggio(String estensione) {
		if (estensione == null || estensione.isBlank())
			return null;
		for (Linguaggio l : linguaggiSupportati) {
			if (l.supportaEstensione(estensione))
				return l;
		}
		// returna null se non riconosciuto
		return null;
	}

	// Estrae l'estensione di un file a partire dal nome (oppure stringa vuota)
	private static String estraiEstensione(String fileName) {
		if (fileName == null)
			return "";
		int dot = fileName.lastIndexOf('.');
		if (dot < 0 || dot == fileName.length() - 1)
			return "";
		return fileName.substring(dot).toLowerCase();
	}

	private boolean isExcluded(Path path) {
		String normalized = "/" + path.toString().replace("\\", "/") + "/";
		return EXCLUDED_DIRS.stream().anyMatch(normalized::contains);
	}
}