# CodeReviewBot
Strumento di analisi statica del codice Java integrato in Visual Studio Code. 

L'obiettivo del progetto è individuare automaticamente alcune problematiche comuni nel codice sorgente 
e fornire un feedback immediato allo sviluppatore tramite:
- una dashboard interattiva
- integrazione con il pannello Problems di VS Code
- generazione di report di analisi (HTML o PDF)

L'estensione analizza il codice sorgente di un progetto Java e segnala eventuali violazioni di 
regole di qualità predefinite.

## Architettura del sistema

Il sistema è composto da due componenti principali:

### motore di analisi (Java)
- analizza il codice sorgente
- applica le regole di qualità
- genera report JSON, HTML o PDF

### estensione Visual Studio Code (TypeScript)
- esegue l'analisi tramite la CLI
- visualizza i risultati nella dashboard
- integra le issue nel pannello Problems


## Funzionalità principali

CodeReviewBot offre le seguenti funzionalità:
- analisi statica del codice Java di progetti aperti in VS Code
- visualizzazione delle issue rilevate nella dashboard
- integrazione con il pannello Problems di VS Code
- navigazione diretta al file contenente l'errore
- generazione di report HTML o PDF


## Regole implementate

### R_NO_TODO
Rileva la presenza di commenti `TODO` nel codice.

I commenti TODO indicano codice incompleto o da revisionare e dovrebbero essere risolti prima del rilascio.

### R_NO_SYSOUT
Rileva l'uso di `System.out.println`.

Nelle applicazioni reali è preferibile utilizzare un framework di logging invece di stampare direttamente su console.


## Prerequisiti

> L’estensione VS Code funge da interfaccia grafica del motore di analisi Java, che viene eseguito tramite CLI.
Per utilizzare CodeReviewBot è necessario avere installato:

- Visual Studio Code
- Java (JDK 17 o superiore)
- Maven 

## Installazione 

### 1. Build backend Java 

> Importante: il file `.vsix` **non è autosufficiente**.  
> Per usare l’estensione è necessario avere anche il backend Java buildato.

Per la build: 
- scaricare la cartella .zip dal repository di github 
- estrarre lo zip e salvare in locale 
- aprire da command prompt la cartella contenente il `pom.xml` `(codereviewbot)` 
- eseguire il comando `mvn clean package`
- se la build termina correttamente, nella cartella `target/` viene generato il jar eseguibile
`codereviewbot-0.0.1-SNAPSHOT.jar`

### 2. Installazione estensione VS Code 

Installare l'estensione in Visual Studio Code: 
1. aprire VS Code 
2. aprire la sezione `Extensions`
- premere `Ctrl + Shift + X` oppure 
- selezionare direttamente nella barra laterale a sinistra 
3. selezionare l'icona menu `...` in alto a destra 
4. selezionare `Install from VSIX` 
5. selezionare il file `codereviewbot-0.0.1.vsix`

Una volta installata, l'estensione sarà disponibile nei comandi di VS Code.

### 3. Configurazione del backend  

L'estensione deve sapere come eseguire il backend Java: 
1. aprire le settings di VS Code 
- `Ctrl +`	oppure  	
- nel menu in alto  sinistra, File → Preferences → Settings
2. cercare nella barra di ricerca `CodeReviewBot: Cli Command`
3. impostare come valore il percorso del .jar, ad esempio 
`java -jar C:\percorso\codereviewbot\target\codereviewbot-0.0.1-SNAPSHOT.jar`


## Utilizzo 

1. aprire un progetto Java in VS Code 
`File → Open folder`

2. aprire la Command Palette 
`Ctrl + Shift + P`

3. scrivere il comando 
`CodeReviewBot: Open Dashboard`

4. nella dashboard, premere `Run Analysis`

Al termine dell'analisi, CodeReviewBot mostrerà: 
- il quality score
- numero totale di issue rilevate 
- issue suddivise per categoria 
- tabella con dettaglio delle issue 
- un report (HTML o PDF) attraverso il pulsante Generate Report 

Ogni issue può essere cliccata nel pannello Problems di VS Code, permettendo allo sviluppatore di aprire direttamente 
il file e la riga corrispondente.


## Report di analisi 

CodeReviewBot genera un report dell'analisi in formato 
- HTML (default)
- PDF

Per modificare il formato da default: 
1. aprire le Settings
- `Ctrl +`	oppure  	
- nel menu in alto  sinistra, File → Preferences → Settings 
2. scrivere `CodeReviewBot` nella barra di ricerca 
3. selezionare `PDF` al posto di `HTML` in `CodeReviewBot: Format`

I report vengono salvati nella cartella `.codereviewbot/` all'interno del progetto analizzato.


## Dashboard

La dashboard fornisce una visualizzazione riassuntiva dei risultati dell'analisi e include: 
- quality score
- numero totale di issue
- issue raggruppate per regola 
- tabella dettagliata delle issue  
- apertura del report generato 
- accesso rapido al pannello Problems


## Struttura del progetto 

Il repository è organizzato nelle seguenti componenti principali: 

codereviewbot/

├── src/ → motore di analisi statica (Java)

├── target/ → file generati dalla compilazione Maven

├── vscode-codereviewbot/ → estensione Visual Studio Code

└── pom.xml → configurazione del progetto Maven