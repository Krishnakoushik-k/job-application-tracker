package com.jobtracker.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIntegrationTest extends AbstractIntegrationTest {

    private long create(String token, String company, String status) throws Exception {
        Map<String, Object> body = new HashMap<>(applicationBody(company, "Developer"));
        body.put("status", status);
        body.put("applicationDate", LocalDate.now().toString());
        String response = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void emptyDashboardHasZeroesAndNoNaN() throws Exception {
        String token = registerAndGetToken("empty@example.com");

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(0))
                .andExpect(jsonPath("$.successRate").value(0.0))
                .andExpect(jsonPath("$.statusDistribution.length()").value(6))
                .andExpect(jsonPath("$.monthlyTrend.length()").value(12))
                .andExpect(jsonPath("$.upcomingInterviews.length()").value(0))
                .andExpect(jsonPath("$.recentApplications.length()").value(0));
    }

    @Test
    void statisticsAreCalculatedFromTheUsersOwnRecordsOnly() throws Exception {
        String alice = registerAndGetToken("alice@example.com");
        String bob = registerAndGetToken("bob@example.com");

        create(alice, "A1", "APPLIED");
        create(alice, "A2", "APPLIED");
        create(alice, "A3", "ASSESSMENT");
        long interviewApp = create(alice, "A4", "INTERVIEW");
        create(alice, "A5", "OFFER");
        create(alice, "A6", "REJECTED");
        create(alice, "A7", "WITHDRAWN");
        create(alice, "A8", "REJECTED");

        // Bob's data must not influence Alice's numbers.
        create(bob, "B1", "OFFER");
        create(bob, "B2", "OFFER");

        mockMvc.perform(post("/api/applications/" + interviewApp + "/interviews")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "roundName", "Technical",
                                "interviewType", "ONLINE",
                                "scheduledAt", LocalDateTime.now().plusDays(2).withNano(0).toString()))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/applications/" + interviewApp + "/interviews")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "roundName", "Old round",
                                "interviewType", "PHONE",
                                "scheduledAt", LocalDateTime.now().minusDays(9).withNano(0).toString()))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(8))
                .andExpect(jsonPath("$.appliedCount").value(2))
                .andExpect(jsonPath("$.assessmentCount").value(1))
                .andExpect(jsonPath("$.interviewCount").value(1))
                .andExpect(jsonPath("$.offerCount").value(1))
                .andExpect(jsonPath("$.rejectedCount").value(2))
                .andExpect(jsonPath("$.withdrawnCount").value(1))
                .andExpect(jsonPath("$.upcomingInterviewCount").value(1))
                .andExpect(jsonPath("$.successRate").value(12.5))
                .andExpect(jsonPath("$.upcomingInterviews.length()").value(1))
                .andExpect(jsonPath("$.upcomingInterviews[0].roundName").value("Technical"))
                .andExpect(jsonPath("$.recentApplications.length()").value(5))
                // the current month is always the last entry of the trend
                .andExpect(jsonPath("$.monthlyTrend[11].count").value(8));

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.totalApplications").value(2))
                .andExpect(jsonPath("$.offerCount").value(2))
                .andExpect(jsonPath("$.successRate").value(100.0))
                .andExpect(jsonPath("$.upcomingInterviewCount").value(0));
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }
}
