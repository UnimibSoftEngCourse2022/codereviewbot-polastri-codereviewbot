import * as fs from "fs";
import { ReportDTO } from "./domain";

export class ReportParser {
  parseFromFile(jsonPath: string): ReportDTO {
    const raw = fs.readFileSync(jsonPath, "utf-8");
    return JSON.parse(raw) as ReportDTO;
  }
}