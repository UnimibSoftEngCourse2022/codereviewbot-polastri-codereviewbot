import * as vscode from "vscode";
import { ReportDTO, IssueDTO } from "./domain";

type DashboardCallbacks = {
  onRunAnalysis: () => Promise<{ report: ReportDTO; humanReportPath?: string } | null>;
  onOpenIssue: (issue: IssueDTO) => Promise<void>;
  onOpenProblems: () => Promise<void>;
  onOpenReport: (path: string) => Promise<void>;
};

export class DashboardView {
  private panel: vscode.WebviewPanel | undefined;

  // ✅ memorizza l'ultimo report "umano" (HTML/PDF) generato
  private lastHumanReportPath?: string;

  constructor(
    private readonly ctx: vscode.ExtensionContext,
    private readonly cb: DashboardCallbacks
  ) {}

  open() {
    if (this.panel) {
      this.panel.reveal(vscode.ViewColumn.One);
      return;
    }

    this.panel = vscode.window.createWebviewPanel(
      "codereviewbot.dashboard",
      "CodeReviewBot Dashboard",
      vscode.ViewColumn.One,
      {
        enableScripts: true,
        retainContextWhenHidden: true
      }
    );

    this.panel.webview.html = this.renderHtml();

    this.panel.webview.onDidReceiveMessage(async (msg) => {
      if (!msg || typeof msg.type !== "string") return;

      if (msg.type === "run") {
        try {
          const result = await this.cb.onRunAnalysis();

          if (result && result.report) {
            // ✅ salva path dell'ultimo report HTML/PDF generato
            this.lastHumanReportPath = result.humanReportPath;

            // la webview riceve sia report che humanReportPath (serve per abilitare bottone)
            this.panel?.webview.postMessage({ type: "report", payload: result });
          } else {
            this.panel?.webview.postMessage({
              type: "error",
              payload: "Risultato analisi non valido (manca report)."
            });
          }
        } catch (e: any) {
          this.panel?.webview.postMessage({
            type: "error",
            payload: e?.message ?? String(e)
          });
        }
      }

      if (msg.type === "openIssue") {
        const issue = msg.payload as IssueDTO;
        await this.cb.onOpenIssue(issue);
      }

      if (msg.type === "openProblems") {
        await this.cb.onOpenProblems();
      }

      if (msg.type === "openReport") {
        // ✅ NON ci fidiamo del payload del webview: usiamo l'ultimo path noto lato extension
        if (!this.lastHumanReportPath) {
          this.panel?.webview.postMessage({
            type: "error",
            payload: "Nessun report HTML/PDF disponibile. Imposta formato HTML/PDF e riesegui l'analisi."
          });
          return;
        }
        await this.cb.onOpenReport(this.lastHumanReportPath);
      }
    });

    this.panel.onDidDispose(() => {
      this.panel = undefined;
      this.lastHumanReportPath = undefined;
    });
  }

  private renderHtml(): string {
    return /* html */ `
<!doctype html>
<html lang="it">
<head>
  <meta charset="UTF-8" />
  <meta http-equiv="Content-Security-Policy"
        content="default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline';">
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>CodeReviewBot Dashboard</title>
  <style>
    body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial; margin: 16px; }
    .row { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
    .card { border: 1px solid #3333; border-radius: 12px; padding: 12px 14px; min-width: 220px; }
    .title { font-size: 18px; font-weight: 700; margin: 0 0 8px; }
    .muted { color: #888; font-size: 12px; }
    button {
      border: 0; border-radius: 10px; padding: 10px 14px; cursor: pointer;
      font-weight: 600;
      background: #222; color: #ddd;
    }
    button.primary { background: #3b82f6; color: white; }
    button:disabled { opacity: .6; cursor: default; }
    table { width: 100%; border-collapse: collapse; margin-top: 14px; }
    th, td { text-align: left; padding: 10px; border-bottom: 1px solid #3333; }
    tr:hover { background: #3331; }
    .pill { display: inline-block; padding: 2px 8px; border-radius: 999px; border: 1px solid #3334; font-size: 12px; }
    .right { margin-left: auto; }
    .warn { color: #f59e0b; }
    .err  { color: #ef4444; }
    .info { color: #60a5fa; }
    .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace; }
    .small { font-size: 12px; }
  </style>
</head>
<body>
  <div class="row">
    <div>
      <div class="title">CodeReviewBot Dashboard</div>
      <div class="muted">Avvia l'analisi e visualizza score + issue. Clicca una issue per aprire il file.</div>
    </div>

    <div class="right row" style="gap:10px;">
      <button id="problemsBtn">Problems</button>
      <button id="reportBtn" disabled>Open Report</button>
      <button id="runBtn" class="primary">Run Analysis</button>
    </div>
  </div>

  <div class="row" style="margin-top: 12px;">
    <div class="card">
      <div class="muted">Quality score</div>
      <div id="score" style="font-size: 28px; font-weight: 800;">—</div>
    </div>
    <div class="card">
      <div class="muted">Issue totali</div>
      <div id="count" style="font-size: 28px; font-weight: 800;">—</div>
    </div>
    <div class="card" style="flex: 1; min-width: 260px;">
      <div class="muted">Ultima analisi</div>
      <div id="meta" class="small mono">—</div>
    </div>
    <div class="card" style="flex: 1; min-width: 260px;">
      <div class="muted">Issue per regola</div>
      <div id="byRule" class="small mono">—</div>
    </div>
  </div>

  <table>
    <thead>
      <tr>
        <th>Severità</th>
        <th>Regola</th>
        <th>Messaggio</th>
        <th>File</th>
        <th>Riga</th>
      </tr>
    </thead>
    <tbody id="issuesBody">
      <tr><td colspan="5" class="muted">Nessun dato. Premi "Run Analysis".</td></tr>
    </tbody>
  </table>

  <script>
    const vscode = acquireVsCodeApi();

    const runBtn = document.getElementById('runBtn');
    const problemsBtn = document.getElementById('problemsBtn');
    const reportBtn = document.getElementById('reportBtn');

    const scoreEl = document.getElementById('score');
    const countEl = document.getElementById('count');
    const metaEl  = document.getElementById('meta');
    const byRuleEl = document.getElementById('byRule');
    const bodyEl  = document.getElementById('issuesBody');

    let lastHumanReportPath = '';

    runBtn.addEventListener('click', () => {
      runBtn.disabled = true;
      runBtn.textContent = 'Running...';
      vscode.postMessage({ type: 'run' });
    });

    problemsBtn.addEventListener('click', () => {
      vscode.postMessage({ type: 'openProblems' });
    });

    reportBtn.addEventListener('click', () => {
      // ✅ non passiamo path: lo gestisce lato extension (più robusto)
      vscode.postMessage({ type: 'openReport' });
    });

    function sevClass(sev) {
      const s = (sev || '').toUpperCase();
      if (s.includes('ERR') || s.includes('CRIT')) return 'err';
      if (s.includes('WARN') || s.includes('MED')) return 'warn';
      return 'info';
    }

    function escapeHtml(s) {
      return String(s || '').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }

    // payload = { report, humanReportPath }
    function render(payload) {
      const report = payload?.report;
      const issues = report?.issues ?? [];

      lastHumanReportPath = payload?.humanReportPath || '';
      reportBtn.disabled = !lastHumanReportPath;

      scoreEl.textContent = (report && typeof report.qualityScore === 'number') ? String(report.qualityScore) : '—';
      countEl.textContent = String(issues.length);

      const genAt = report?.generatedAt || '';
      const proj  = report?.analysis?.projectPath || '';
      metaEl.textContent = (genAt || proj) ? \`\${genAt}  \${proj}\` : '—';

      const counts = {};
      for (const it of issues) {
        const k = it.ruleId || 'UNKNOWN';
        counts[k] = (counts[k] || 0) + 1;
      }
      const lines = Object.entries(counts)
        .sort((a,b) => b[1] - a[1])
        .map(([k,v]) => \`\${k}: \${v}\`)
        .join('  |  ');
      byRuleEl.textContent = issues.length ? lines : '—';

      if (issues.length === 0) {
        bodyEl.innerHTML = '<tr><td colspan="5" class="muted">Nessuna issue trovata 🎉</td></tr>';
        return;
      }

      bodyEl.innerHTML = '';
      for (const it of issues) {
        const tr = document.createElement('tr');
        tr.innerHTML = \`
          <td><span class="pill \${sevClass(it.severity)}">\${escapeHtml(it.severity || 'INFO')}</span></td>
          <td class="mono small">\${escapeHtml(it.ruleId || '')}</td>
          <td>\${escapeHtml(it.message || '')}</td>
          <td class="mono small">\${escapeHtml((it.file || '').split(/[\\\\/]/).slice(-2).join('/'))}</td>
          <td class="mono small">\${escapeHtml(it.line || '')}</td>
        \`;
        tr.addEventListener('click', () => {
          vscode.postMessage({ type: 'openIssue', payload: it });
        });
        bodyEl.appendChild(tr);
      }
    }

    window.addEventListener('message', (event) => {
      const msg = event.data;
      if (!msg || !msg.type) return;

      if (msg.type === 'report') {
        render(msg.payload);
        runBtn.disabled = false;
        runBtn.textContent = 'Run Analysis';
      }

      if (msg.type === 'error') {
        runBtn.disabled = false;
        runBtn.textContent = 'Run Analysis';
        bodyEl.innerHTML = \`<tr><td colspan="5" class="err">Errore: \${escapeHtml(msg.payload || '')}</td></tr>\`;
      }
    });
  </script>
</body>
</html>
`;
  }
}