export type ReportFormat = "JSON" | "HTML" | "PDF";
export type Severity = "INFO" | "WARNING" | "ERROR" | string;

export interface IssueDTO {
  file: string;
  line: number;
  ruleId?: string;
  category?: string;
  severity?: Severity;
  message?: string;
}

export interface ReportDTO {
  reportId?: string;
  generatedAt?: string;
  format?: string;
  qualityScore?: number;
  analysis?: {
    analysisId?: string;
    projectPath?: string;
  };
  issues?: IssueDTO[];
}