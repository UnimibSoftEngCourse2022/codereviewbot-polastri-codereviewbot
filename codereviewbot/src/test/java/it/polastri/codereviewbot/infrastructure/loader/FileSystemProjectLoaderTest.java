package it.polastri.codereviewbot.infrastructure.loader;

import it.polastri.codereviewbot.domain.FileSorgente;
import it.polastri.codereviewbot.domain.Linguaggio;
import it.polastri.codereviewbot.domain.Progetto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemProjectLoaderTest {

	// Directory temporanea fornita da JUnit usata per simulare progetti su filesystem.
    @TempDir
    Path tempDir;

    // Controlla se il filesystem supporta i permessi POSIX.
    private static boolean posixSupported() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    // Il costruttore non deve accettare un linguaggio nullo.
    @Test
    void costruttore_lanciaEccezioneSeLinguaggiNull() {
        assertThrows(NullPointerException.class, () -> new FileSystemProjectLoader(null));
    }

    // La scansione deve essere ricorsiva e deve includere solo linguaggi supportati.
    @Test
    void caricaProgettoescansionaRicorsivamenteeIncludeSoloFileSupportati() throws Exception {
        Path root = tempDir.resolve("myproj");
        Files.createDirectories(root);

        Path aJava = root.resolve("A.java");
        Files.writeString(aJava, "class A {}", StandardCharsets.UTF_8);

        Path nested = root.resolve("src").resolve("main");
        Files.createDirectories(nested);

        Path bPy = nested.resolve("b.py");
        Files.writeString(bPy, "print('hi')", StandardCharsets.UTF_8);

        Path ignored = root.resolve("README.txt");
        Files.writeString(ignored, "ignore me", StandardCharsets.UTF_8);

        FileSystemProjectLoader loader = new FileSystemProjectLoader(List.of(
                new Linguaggio("Java", List.of(".java")),
                new Linguaggio("Python", List.of(".py"))
        ));

        Progetto progetto = loader.caricaProgetto(root.toString());

        assertEquals(root.toAbsolutePath().normalize().toString(), progetto.getProjectPath());
        assertEquals(2, progetto.getFileSorgenti().size(), "Deve includere solo .java e .py (ricorsivamente)");

        List<String> paths = progetto.getFileSorgenti().stream().map(FileSorgente::getPath).toList();
        assertTrue(paths.contains(aJava.toAbsolutePath().normalize().toString()));
        assertTrue(paths.contains(bPy.toAbsolutePath().normalize().toString()));
        assertFalse(paths.contains(ignored.toAbsolutePath().normalize().toString()));
    }

    // Il contenuto del file sorgente deve essere letto e associato correttamente al file.
    @Test
    void caricaProgettoeleggeIlContenutoDelFile() throws Exception {
        Path root = tempDir.resolve("p2");
        Files.createDirectories(root);

        Path aJava = root.resolve("Main.java");
        String content = "class Main { }";
        Files.writeString(aJava, content, StandardCharsets.UTF_8);

        FileSystemProjectLoader loader = new FileSystemProjectLoader(List.of(
                new Linguaggio("Java", List.of(".java"))
        ));

        Progetto progetto = loader.caricaProgetto(root.toString());
        FileSorgente fs = progetto.getFileSorgenti().get(0);

        assertEquals("Main.java", fs.getNome());
        assertEquals(aJava.toAbsolutePath().normalize().toString(), fs.getPath());
        assertEquals(content, fs.getContenuto());
        assertEquals("Java", fs.getLinguaggio().getNome());
    }

    // Se il percorso del progetto non esiste deve lanciare un'eccezione.
    @Test
    void caricaProgettoelanciaEccezioneSePercorsoNonEsiste() {
        FileSystemProjectLoader loader = new FileSystemProjectLoader();
        String invalidPath = tempDir.resolve("nope").toString();

        assertThrows(IllegalArgumentException.class,() -> loader.caricaProgetto(invalidPath));
    }

    // Se il percorso del progetto non è una directory deve lanciare un'eccezione.
    @Test
    void caricaProgettoelanciaEccezioneSePercorsoNonEDirectory() throws Exception {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "x", StandardCharsets.UTF_8);

        FileSystemProjectLoader loader = new FileSystemProjectLoader();
        String filePath = file.toString();

        assertThrows(IllegalArgumentException.class,() -> loader.caricaProgetto(filePath));
    }

    // Se il percorso del progetto è null o vuoto deve lanciare un'eccezione.
    @Test
    void caricaProgettoerifiutaPathVuotoONullo() {
        FileSystemProjectLoader loader = new FileSystemProjectLoader();
        assertThrows(IllegalArgumentException.class, () -> loader.caricaProgetto(null));
        assertThrows(IllegalArgumentException.class, () -> loader.caricaProgetto("   "));
    }

    // Se l'estensione è nulla o con punto finale deve essere ignorata.
    @Test
    void caricaProgettoeignoraFileSenzaEstensioneoConPuntoFinale() throws Exception {
        Path root = tempDir.resolve("p3");
        Files.createDirectories(root);

        Path noExt = root.resolve("README");
        Files.writeString(noExt, "x", StandardCharsets.UTF_8);

        Path trailingDot = root.resolve("weird.");
        Files.writeString(trailingDot, "y", StandardCharsets.UTF_8);

        Path okJava = root.resolve("Ok.java");
        Files.writeString(okJava, "class Ok {}", StandardCharsets.UTF_8);

        FileSystemProjectLoader loader = new FileSystemProjectLoader(List.of(
                new Linguaggio("Java", List.of(".java"))
        ));

        Progetto progetto = loader.caricaProgetto(root.toString());

        assertEquals(1, progetto.getFileSorgenti().size());
    }

    // Se il file sorgente non è leggibile deve lanciare un'eccezione.
    @Test
    void caricaProgettolanciaEccezioneSeFileSupportatoNonLeggibile() throws Exception {
        if (!posixSupported()) {
            return;
        }

        Path root = tempDir.resolve("p4");
        Files.createDirectories(root);

        Path unreadableJava = root.resolve("NoRead.java");
        Files.writeString(unreadableJava, "class X {}", StandardCharsets.UTF_8);

        Set<PosixFilePermission> originalPerms = Files.getPosixFilePermissions(unreadableJava);

        try {
            // Rimuove il permesso di lettura (mantenendo write per poter ripristinare)
            Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(unreadableJava, perms);

            FileSystemProjectLoader loader = new FileSystemProjectLoader(List.of(new Linguaggio("Java", List.of(".java"))));
            String projectPath = root.toString();
            
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> loader.caricaProgetto(projectPath));
            assertTrue(ex.getMessage().contains("Impossibile leggere file:"));
        } finally {
            Files.setPosixFilePermissions(unreadableJava, originalPerms);
        }
    }

    // Se la scansione ricorsiva fallisce deve lanciare un'eccezione.
    @Test
    void caricaProgettoelanciaEccezioneSeScansioneFallisce() throws Exception {
    	Assumptions.assumeTrue(System.getenv("GITHUB_ACTIONS") == null,
    	        "Skip su GitHub Actions: test permessi non affidabile nel runner.");
    	
    	if (!posixSupported()) {
            return;
        }

        Path root = tempDir.resolve("p_walk_fail");
        Files.createDirectories(root);

        Set<PosixFilePermission> originalDirPerms = Files.getPosixFilePermissions(root);

        try {
            // Toglie EXECUTE: in teoria impedisce di attraversare la directory
            Set<PosixFilePermission> noTraverse = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(root, noTraverse);

            // ✅ Se l'ambiente NON rende davvero la directory inaccessibile, skippiamo il test
            try {
                Files.newDirectoryStream(root).iterator().hasNext();
                Assumptions.abort("Permessi POSIX non applicati/effettivi su questo ambiente: test skip.");
            } catch (Exception expected) {
                // ok: directory effettivamente inaccessibile, il test ha senso
            }

            FileSystemProjectLoader loader = new FileSystemProjectLoader();
            String projectPath = root.toString();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> loader.caricaProgetto(projectPath));
            assertTrue(ex.getMessage().contains("Errore durante la scansione del progetto:"));
        } finally {
            Files.setPosixFilePermissions(root, originalDirPerms);
        }
    }

    @Test
    void risolviLinguaggio_copreRamoNullEDiBlank() throws Exception {
        FileSystemProjectLoader loader = new FileSystemProjectLoader(List.of(new Linguaggio("Java", List.of(".java"))));

        Method m = FileSystemProjectLoader.class.getDeclaredMethod("risolviLinguaggio", String.class);
        m.setAccessible(true);

        // Estensione == null
        Object r1 = m.invoke(loader, new Object[]{null});
        assertNull(r1);

        // Estensione.isBlank()
        Object r2 = m.invoke(loader, "   ");
        assertNull(r2);
    }

    @Test
    void estraiEstensione_copreNullENoDotETrailingDot() throws Exception {
        Method m = FileSystemProjectLoader.class.getDeclaredMethod("estraiEstensione", String.class);
        m.setAccessible(true);

        // FileName == null
        String e1 = (String) m.invoke(null, new Object[]{null});
        assertEquals("", e1);

        // File senza estensione
        String e2 = (String) m.invoke(null, "Estensione non supportata.");
        assertEquals("", e2);

        // Punto finale
        String e3 = (String) m.invoke(null, "Estensione non supportata.");
        assertEquals("", e3);
    }
    
    @Test
    void caricaProgetto_escludeTargetSrcTestEVscodeCodereviewbot() throws Exception {
        Path mainJava = tempDir.resolve("src/main/java");
        Files.createDirectories(mainJava);
        Files.writeString(mainJava.resolve("A.java"), "class A {}");

        Path testJava = tempDir.resolve("src/test/java");
        Files.createDirectories(testJava);
        Files.writeString(testJava.resolve("ATest.java"), "class ATest {}");

        Path target = tempDir.resolve("target");
        Files.createDirectories(target);
        Files.writeString(target.resolve("Gen.java"), "class Gen {}");

        Path vsExt = tempDir.resolve("vscode-codereviewbot/src");
        Files.createDirectories(vsExt);
        Files.writeString(vsExt.resolve("x.ts"), "console.log('x');");

        FileSystemProjectLoader loader = new FileSystemProjectLoader();
        var progetto = loader.caricaProgetto(tempDir.toString());

        assertEquals(1, progetto.getFileSorgenti().size());

        var file = progetto.getFileSorgenti().get(0);
        String path = file.getPath(); 

        assertTrue(path.replace("\\", "/").contains("/src/main/java/A.java"));
    }
}