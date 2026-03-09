import * as vscode from "vscode";
import * as path from "path";
import * as fs from "fs";
import { spawn } from "child_process";
import { ExtensionConfig } from "./config";

export class CliRunner {
  constructor(private readonly cfg: ExtensionConfig) {}

  async runAnalysis(projectPath: string, requestedFormat: string): Promise<{ jsonPath: string; humanReportPath?: string }> {
    if (!projectPath || projectPath.trim().length === 0) {
      throw new Error("projectPath vuoto: apri una cartella progetto in VS Code.");
    }

    const outDirName = this.cfg.outputDir || ".codereviewbot";
    const outDirAbs = path.isAbsolute(outDirName) ? outDirName : path.join(projectPath, outDirName);

    fs.mkdirSync(outDirAbs, { recursive: true });

    // Nomi file coerenti
    const jsonPath = path.join(outDirAbs, "report.json");
    const humanExt = (requestedFormat || "JSON").toUpperCase() === "PDF" ? "pdf"
      : (requestedFormat || "JSON").toUpperCase() === "HTML" ? "html"
      : "json";

    const humanReportPath =
      (requestedFormat || "JSON").toUpperCase() === "JSON"
        ? undefined
        : path.join(outDirAbs, `report.${humanExt}`);

    const { executable, args } = this.resolveCliCommand(projectPath);

    // Args CLI: sempre projectPath, formato, output
    // JSON (diagnostics) sempre generato
    const cliArgs: string[] = [
      ...args,
      "--project", projectPath,
      "--format", "JSON",
      "--out", jsonPath
    ];

    // Se richiesto HTML/PDF, facciamo una seconda run per il report "umano"
    // (manteniamo semplice e compatibile col tuo backend attuale)
    const needHuman = requestedFormat && requestedFormat.toUpperCase() !== "JSON";

    await this.exec(executable, cliArgs, projectPath);

    if (needHuman && humanReportPath) {
      const humanArgs: string[] = [
        ...args,
        "--project", projectPath,
        "--format", requestedFormat.toUpperCase(),
        "--out", humanReportPath
      ];
      await this.exec(executable, humanArgs, projectPath);
    }

    return { jsonPath, humanReportPath };
  }

  // --- helper: esegue processo ---
	private exec(executable: string, args: string[], cwd: string): Promise<void> {
	  return new Promise((resolve, reject) => {
		const p = spawn(executable, args, { cwd, shell: false, windowsHide: true });

		let stderr = "";
		let stdout = "";

		p.stderr.on("data", (d) => (stderr += d.toString()));
		p.stdout.on("data", (d) => (stdout += d.toString()));

		const timeoutMs = 120000; // 2 minuti
		const t = setTimeout(() => {
		  try { p.kill(); } catch {}
		  reject(new Error("Timeout: analisi troppo lunga (processo terminato)."));
		}, timeoutMs);

		p.on("error", (err) => {
		  clearTimeout(t);
		  reject(err);
		});

		p.on("close", (code) => {
		  clearTimeout(t);
		  if (code === 0) return resolve();
		  reject(new Error(`CodeReviewBot exit code ${code}. ${stderr || stdout}`));
		});
	  });
	}

  // --- helper: parse CLI command config / auto-detect jar ---
  private resolveCliCommand(projectPath: string): { executable: string; args: string[] } {
    if (this.cfg.cliCommand && this.cfg.cliCommand.trim().length > 0) {
      return this.parseCliCommand(this.cfg.cliCommand);
    }

    const targetDir = path.join(projectPath, "target");
    if (!fs.existsSync(targetDir)) {
      throw new Error("Directory 'target' non trovata. Compila con 'mvn -DskipTests package'.");
    }

    const jars = fs.readdirSync(targetDir)
      .filter((f) => f.endsWith(".jar"))
      .map((f) => ({ f, m: fs.statSync(path.join(targetDir, f)).mtimeMs }))
      .sort((a, b) => b.m - a.m);

    if (jars.length === 0) {
      throw new Error("Nessun .jar trovato in target/. Compila con 'mvn -DskipTests package'.");
    }

    const jarPath = path.join(targetDir, jars[0].f);
    return { executable: "java", args: ["-jar", jarPath] };
  }

  private parseCliCommand(cmd: string): { executable: string; args: string[] } {
    // parsing minimal: split su spazi, supporta virgolette semplici
    const parts = cmd.match(/(?:[^\s"]+|"[^"]*")+/g) ?? [];
    const cleaned = parts.map(p => p.replace(/^"(.*)"$/, "$1"));
    if (cleaned.length === 0) throw new Error("cliCommand vuoto");
    return { executable: cleaned[0], args: cleaned.slice(1) };
  }
}