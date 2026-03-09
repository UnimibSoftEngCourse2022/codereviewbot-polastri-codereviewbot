import * as vscode from "vscode";
import { IssueDTO } from "./domain";

export class DiagnosticsPublisher {
  private readonly collection = vscode.languages.createDiagnosticCollection("CodeReviewBot");

  clear() {
    this.collection.clear();
  }

  publish(issues: IssueDTO[]) {
    const byFile = new Map<string, vscode.Diagnostic[]>();

    for (const issue of issues ?? []) {
      if (!issue.file || !issue.line || issue.line <= 0) continue;

      const uri = vscode.Uri.file(issue.file);
      const lineIdx = Math.max(0, issue.line - 1);

      const range = new vscode.Range(lineIdx, 0, lineIdx, 10_000);

      const sev = this.mapSeverity(issue.severity);
      const msg = `[${issue.ruleId ?? "RULE"}] ${issue.message ?? ""}`.trim();

      const d = new vscode.Diagnostic(range, msg, sev);
      d.source = "CodeReviewBot";

      if (!byFile.has(uri.fsPath)) byFile.set(uri.fsPath, []);
      byFile.get(uri.fsPath)!.push(d);
    }

    for (const [fsPath, diags] of byFile.entries()) {
      this.collection.set(vscode.Uri.file(fsPath), diags);
    }
  }

  private mapSeverity(sev: string | undefined): vscode.DiagnosticSeverity {
    const s = (sev ?? "").toUpperCase();
    if (s === "ERROR" || s === "CRITICAL") return vscode.DiagnosticSeverity.Error;
    if (s === "WARNING" || s === "WARN") return vscode.DiagnosticSeverity.Warning;
    return vscode.DiagnosticSeverity.Information;
  }

  dispose() {
    this.collection.dispose();
  }
}