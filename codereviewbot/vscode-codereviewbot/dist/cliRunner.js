"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.CliRunner = void 0;
const path = __importStar(require("path"));
const fs = __importStar(require("fs"));
const child_process_1 = require("child_process");
class CliRunner {
    constructor(cfg) {
        this.cfg = cfg;
    }
    async runAnalysis(projectPath, requestedFormat) {
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
        const humanReportPath = (requestedFormat || "JSON").toUpperCase() === "JSON"
            ? undefined
            : path.join(outDirAbs, `report.${humanExt}`);
        const { executable, args } = this.resolveCliCommand(projectPath);
        // Args CLI: sempre projectPath, formato, output
        // JSON (diagnostics) sempre generato
        const cliArgs = [
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
            const humanArgs = [
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
    exec(executable, args, cwd) {
        return new Promise((resolve, reject) => {
            const p = (0, child_process_1.spawn)(executable, args, { cwd, shell: false, windowsHide: true });
            let stderr = "";
            let stdout = "";
            p.stderr.on("data", (d) => (stderr += d.toString()));
            p.stdout.on("data", (d) => (stdout += d.toString()));
            const timeoutMs = 120000; // 2 minuti
            const t = setTimeout(() => {
                try {
                    p.kill();
                }
                catch { }
                reject(new Error("Timeout: analisi troppo lunga (processo terminato)."));
            }, timeoutMs);
            p.on("error", (err) => {
                clearTimeout(t);
                reject(err);
            });
            p.on("close", (code) => {
                clearTimeout(t);
                if (code === 0)
                    return resolve();
                reject(new Error(`CodeReviewBot exit code ${code}. ${stderr || stdout}`));
            });
        });
    }
    // --- helper: parse CLI command config / auto-detect jar ---
    resolveCliCommand(projectPath) {
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
    parseCliCommand(cmd) {
        // parsing minimal: split su spazi, supporta virgolette semplici
        const parts = cmd.match(/(?:[^\s"]+|"[^"]*")+/g) ?? [];
        const cleaned = parts.map(p => p.replace(/^"(.*)"$/, "$1"));
        if (cleaned.length === 0)
            throw new Error("cliCommand vuoto");
        return { executable: cleaned[0], args: cleaned.slice(1) };
    }
}
exports.CliRunner = CliRunner;
//# sourceMappingURL=cliRunner.js.map