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
exports.activate = activate;
exports.deactivate = deactivate;
const vscode = __importStar(require("vscode"));
const config_1 = require("./config");
const cliRunner_1 = require("./cliRunner");
const reportParser_1 = require("./reportParser");
const diagnosticsPublisher_1 = require("./diagnosticsPublisher");
const dashboardView_1 = require("./dashboardView");
function activate(context) {
    const cfg = new config_1.ExtensionConfig();
    const runner = new cliRunner_1.CliRunner(cfg);
    const parser = new reportParser_1.ReportParser();
    const publisher = new diagnosticsPublisher_1.DiagnosticsPublisher();
    async function openIssueInEditor(issue) {
        if (!issue?.file || !issue?.line)
            return;
        const uri = vscode.Uri.file(issue.file);
        const doc = await vscode.workspace.openTextDocument(uri);
        const editor = await vscode.window.showTextDocument(doc, { preview: false });
        const line = Math.max(0, issue.line - 1);
        const pos = new vscode.Position(line, 0);
        editor.selection = new vscode.Selection(pos, pos);
        editor.revealRange(new vscode.Range(pos, pos), vscode.TextEditorRevealType.InCenter);
    }
    async function runAnalysisFlow() {
        const ws = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath;
        if (!ws) {
            throw new Error("Apri una cartella progetto (workspace) prima di eseguire l'analisi.");
        }
        publisher.clear();
        const requestedFormat = cfg.format;
        const { jsonPath, humanReportPath } = await runner.runAnalysis(ws, requestedFormat);
        const report = parser.parseFromFile(jsonPath);
        publisher.publish(report.issues ?? []);
        // Debug utilissimo per capire cosa sta analizzando
        console.log("CodeReviewBot workspace:", ws);
        console.log("CodeReviewBot jsonPath:", jsonPath);
        console.log("CodeReviewBot humanReportPath:", humanReportPath ?? "(none)");
        return { report, humanReportPath };
    }
    // Comando "Run Analysis" (senza dashboard)
    const runCmd = vscode.commands.registerCommand("codereviewbot.runAnalysis", async () => {
        try {
            const { report, humanReportPath } = await runAnalysisFlow();
            vscode.window.showInformationMessage(`CodeReviewBot: analisi completata. Issue: ${(report.issues ?? []).length}`);
            // Se l'utente ha scelto HTML/PDF e vuole aprire il report anche senza dashboard
            if (cfg.openGeneratedReport && humanReportPath) {
                await vscode.commands.executeCommand("vscode.open", vscode.Uri.file(humanReportPath));
            }
        }
        catch (e) {
            vscode.window.showErrorMessage(`CodeReviewBot: errore - ${e?.message ?? e}`);
        }
    });
    // Dashboard (WebView)
    const dashboard = new dashboardView_1.DashboardView(context, {
        onRunAnalysis: async () => {
            try {
                return await runAnalysisFlow(); // ritorna { report, humanReportPath }
            }
            catch (e) {
                vscode.window.showErrorMessage(`CodeReviewBot: errore - ${e?.message ?? e}`);
                return null;
            }
        },
        onOpenIssue: async (issue) => {
            await openIssueInEditor(issue);
        },
        onOpenProblems: async () => {
            await vscode.commands.executeCommand("workbench.actions.view.problems");
        },
        onOpenReport: async (p) => {
            if (!p)
                return;
            await vscode.commands.executeCommand("vscode.open", vscode.Uri.file(p));
        }
    });
    const openDashboardCmd = vscode.commands.registerCommand("codereviewbot.openDashboard", () => {
        dashboard.open();
    });
    context.subscriptions.push(runCmd, openDashboardCmd, publisher);
}
function deactivate() { }
//# sourceMappingURL=extension.js.map