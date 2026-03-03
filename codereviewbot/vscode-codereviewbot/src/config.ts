import * as vscode from "vscode";
import { ReportFormat } from "./domain";

export class ExtensionConfig {
  private readonly section = "codereviewbot";

  get cliCommand(): string {
    return vscode.workspace.getConfiguration(this.section).get<string>(
      "cliCommand",
      "java -jar CodeReviewBot.jar"
    );
  }

  get format(): ReportFormat {
    return vscode.workspace.getConfiguration(this.section).get<ReportFormat>("format", "JSON");
  }

  get outputDir(): string {
    return vscode.workspace.getConfiguration(this.section).get<string>("outputDir", ".codereviewbot");
  }

  get openGeneratedReport(): boolean {
    return vscode.workspace.getConfiguration(this.section).get<boolean>("openGeneratedReport", true);
  }
}