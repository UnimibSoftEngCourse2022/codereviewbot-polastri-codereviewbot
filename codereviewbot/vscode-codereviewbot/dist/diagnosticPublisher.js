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
exports.DiagnosticsPublisher = void 0;
const vscode = __importStar(require("vscode"));
class DiagnosticsPublisher {
    constructor() {
        this.collection = vscode.languages.createDiagnosticCollection("CodeReviewBot");
    }
    clear() {
        this.collection.clear();
    }
    publish(issues) {
        const byFile = new Map();
        for (const issue of issues ?? []) {
            if (!issue.file || !issue.line || issue.line <= 0)
                continue;
            const uri = vscode.Uri.file(issue.file);
            const lineIdx = Math.max(0, issue.line - 1);
            const range = new vscode.Range(lineIdx, 0, lineIdx, 10000);
            const sev = this.mapSeverity(issue.severity);
            const msg = `[${issue.ruleId ?? "RULE"}] ${issue.message ?? ""}`.trim();
            const d = new vscode.Diagnostic(range, msg, sev);
            d.source = "CodeReviewBot";
            if (!byFile.has(uri.fsPath))
                byFile.set(uri.fsPath, []);
            byFile.get(uri.fsPath).push(d);
        }
        for (const [fsPath, diags] of byFile.entries()) {
            this.collection.set(vscode.Uri.file(fsPath), diags);
        }
    }
    mapSeverity(sev) {
        const s = (sev ?? "").toUpperCase();
        if (s === "ERROR" || s === "CRITICAL")
            return vscode.DiagnosticSeverity.Error;
        if (s === "WARNING" || s === "WARN")
            return vscode.DiagnosticSeverity.Warning;
        return vscode.DiagnosticSeverity.Information;
    }
    dispose() {
        this.collection.dispose();
    }
}
exports.DiagnosticsPublisher = DiagnosticsPublisher;
//# sourceMappingURL=diagnosticPublisher.js.map