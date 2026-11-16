package com.jobtracker.service;

import com.jobtracker.entity.JobApplication;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.enums.JobType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    private final CsvExportService service = new CsvExportService();

    @Test
    void writesBomHeaderAndOneRowPerApplication() {
        String csv = service.toCsv(List.of(application("Acme", "Developer")));

        assertThat(csv).startsWith("﻿");
        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[0]).contains("Company,Job Title,Location,Job Type");
        assertThat(lines[1]).startsWith("Acme,Developer");
    }

    @Test
    void emptyListProducesHeaderOnly() {
        String csv = service.toCsv(List.of());

        assertThat(csv.split("\r\n")).hasSize(1);
    }

    @Test
    void quotesFieldsWithCommasQuotesAndNewlines() {
        JobApplication app = application("Acme, Inc", "Dev");
        app.setNotes("He said \"hi\"\nsecond line");

        String csv = service.toCsv(List.of(app));

        assertThat(csv).contains("\"Acme, Inc\"");
        assertThat(csv).contains("\"He said \"\"hi\"\"\nsecond line\"");
    }

    @Test
    void neutralisesSpreadsheetFormulas() {
        JobApplication app = application("=HYPERLINK(\"http://evil\")", "+cmd");
        app.setLocation("@SUM(A1)");
        app.setSalaryRange("-5");

        String csv = service.toCsv(List.of(app));

        assertThat(csv).contains("'=HYPERLINK");
        assertThat(csv).contains("'+cmd");
        assertThat(csv).contains("'@SUM(A1)");
        assertThat(csv).contains("'-5");
    }

    @Test
    void nullValuesBecomeEmptyCells() {
        assertThat(CsvExportService.escape(null)).isEmpty();
        assertThat(CsvExportService.escape("")).isEmpty();
        assertThat(CsvExportService.escape("plain")).isEqualTo("plain");
    }

    private static JobApplication application(String company, String title) {
        JobApplication application = new JobApplication();
        application.setCompanyName(company);
        application.setJobTitle(title);
        application.setJobType(JobType.FULL_TIME);
        application.setApplicationDate(LocalDate.of(2026, 11, 1));
        application.setStatus(ApplicationStatus.APPLIED);
        return application;
    }
}
