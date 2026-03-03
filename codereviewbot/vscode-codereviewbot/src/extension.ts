import * as vscode from "vscode";
import { ExtensionConfig } from "./config";
import { CliRunner } from "./cliRunner";
import { ReportParser } from "./reportParser";
import { DiagnosticsPublisher } from "./diagnosticsPublisher";
import { DashboardView } from "./dashboardView";
import { ReportDTO } from "./domain";

export function activate(context: vscode.ExtensionContext) {
  const cfg = new ExtensionConfig();
  const runner = new CliRunner(cfg);
  const parser = new ReportParser();
  const publisher = new DiagnosticsPublisher();

  async function openIssueInEditor(issue: any) {
    if (!issue?.file || !issue?.line) return;

    const uri = vscode.Uri.file(issue.file);
    const doc = await vscode.workspace.openTextDocument(uri);
    const editor = await vscode.window.showTextDocument(doc, { preview: false });

    const line = Math.max(0, (issue.line as number) - 1);
    const pos = new vscode.Position(line, 0);
    editor.selection = new vscode.Selection(pos, pos);
    editor.revealRange(
      new vscode.Range(pos, pos),
      vscode.TextEditorRevealType.InCenter
    );
  }

	async function runAnalysisFlow(): Promise<{ report: ReportDTO; humanReportPath?: string }> {
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

      vscode.window.showInformationMessage(
        `CodeReviewBot: analisi completata. Issue: ${(report.issues ?? []).length}`
      );

      // Se l'utente ha scelto HTML/PDF e vuole aprire il report anche senza dashboard
      if (cfg.openGeneratedReport && humanReportPath) {
        await vscode.commands.executeCommand(
          "vscode.open",
          vscode.Uri.file(humanReportPath)
        );
      }
    } catch (e: any) {
      vscode.window.showErrorMessage(`CodeReviewBot: errore - ${e?.message ?? e}`);
    }
  });

  // Dashboard (WebView)
  const dashboard = new DashboardView(context, {
    onRunAnalysis: async () => {
      try {
        return await runAnalysisFlow(); // ritorna { report, humanReportPath }
      } catch (e: any) {
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
    onOpenReport: async (p: string) => {
      if (!p) return;
      await vscode.commands.executeCommand("vscode.open", vscode.Uri.file(p));
    }
  });

  const openDashboardCmd = vscode.commands.registerCommand(
    "codereviewbot.openDashboard",
    () => {
      dashboard.open();
    }
  );

  context.subscriptions.push(runCmd, openDashboardCmd, publisher);
}

export function deactivate() {}