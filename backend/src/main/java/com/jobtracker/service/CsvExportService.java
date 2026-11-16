package com.jobtracker.service;

import com.jobtracker.entity.JobApplication;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds RFC 4180 style CSV text. Cells that start with =, +, -, @, tab or CR are prefixed with an
 * apostrophe so spreadsheet programs cannot interpret user-entered text as a formula (CSV injection).
 */
@Service
public class CsvExportService {

    private static final String BOM = "﻿"; // lets Excel detect UTF-8
    private static final String NEWLINE = "\r\n";
    private static final String[] HEADERS = {
            "Company", "Job Title", "Location", "Job Type", "Application Date", "Status",
            "Job URL", "Salary Range", "Recruiter Name", "Recruiter Email",
            "Job Description", "Notes", "Created At"
    };

    public String toCsv(List<JobApplication> applications) {
        StringBuilder csv = new StringBuilder(BOM);
        appendRow(csv, HEADERS);
        for (JobApplication a : applications) {
            appendRow(csv,
                    a.getCompanyName(),
                    a.getJobTitle(),
                    a.getLocation(),
                    a.getJobType() == null ? null : a.getJobType().name(),
                    a.getApplicationDate() == null ? null : a.getApplicationDate().toString(),
                    a.getStatus() == null ? null : a.getStatus().name(),
                    a.getJobUrl(),
                    a.getSalaryRange(),
                    a.getRecruiterName(),
                    a.getRecruiterEmail(),
                    a.getJobDescription(),
                    a.getNotes(),
                    a.getCreatedAt() == null ? null : a.getCreatedAt().toString());
        }
        return csv.toString();
    }

    private void appendRow(StringBuilder csv, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(cells[i]));
        }
        csv.append(NEWLINE);
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String safe = value;
        char first = safe.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            safe = "'" + safe;
        }
        boolean needsQuotes = safe.contains(",") || safe.contains("\"")
                || safe.contains("\n") || safe.contains("\r");
        if (needsQuotes) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
