package com.jobtracker.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewAndNoteIntegrationTest extends AbstractIntegrationTest {

    private Map<String, Object> interviewBody(String round, LocalDateTime when) {
        return Map.of(
                "roundName", round,
                "interviewType", "ONLINE",
                "scheduledAt", when.toString(),
                "locationOrLink", "https://meet.example.com/abc",
                "notes", "Prepare Spring Security");
    }

    private long createInterview(String token, long applicationId, String round, LocalDateTime when) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(interviewBody(round, when))))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }

    @Test
    void interviewLifecycle() throws Exception {
        String token = registerAndGetToken("alice@example.com");
        long applicationId = createApplication(token, "Acme", "Developer");
        LocalDateTime when = LocalDateTime.now().plusDays(3).withNano(0);

        MvcResult created = mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(interviewBody("Technical Round", when))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roundName").value("Technical Round"))
                .andExpect(jsonPath("$.interviewType").value("ONLINE"))
                .andExpect(jsonPath("$.applicationId").value(applicationId))
                .andExpect(jsonPath("$.companyName").value("Acme"))
                .andReturn();
        long interviewId = readJson(created).get("id").asLong();

        mockMvc.perform(get("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        Map<String, Object> updated = Map.of(
                "roundName", "HR Round",
                "interviewType", "PHONE",
                "scheduledAt", when.plusDays(1).toString(),
                "locationOrLink", "Recruiter will call",
                "notes", "Updated");
        mockMvc.perform(put("/api/interviews/" + interviewId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundName").value("HR Round"))
                .andExpect(jsonPath("$.interviewType").value("PHONE"));

        mockMvc.perform(get("/api/interviews").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/interviews/" + interviewId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/interviews").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void interviewValidationRejectsMissingFields() throws Exception {
        String token = registerAndGetToken("alice@example.com");
        long applicationId = createApplication(token, "Acme", "Developer");

        mockMvc.perform(post("/api/applications/" + applicationId + "/interviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("roundName", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.roundName").exists())
                .andExpect(jsonPath("$.fieldErrors.interviewType").exists())
                .andExpect(jsonPath("$.fieldErrors.scheduledAt").exists());
    }

    @Test
    void upcomingListContainsOnlyFutureInterviewsSoonestFirst() throws Exception {
        String token = registerAndGetToken("alice@example.com");
        long applicationId = createApplication(token, "Acme", "Developer");
        createInterview(token, applicationId, "Past Round", LocalDateTime.now().minusDays(5));
        createInterview(token, applicationId, "Later Round", LocalDateTime.now().plusDays(10));
        createInterview(token, applicationId, "Soon Round", LocalDateTime.now().plusDays(2));

        mockMvc.perform(get("/api/interviews/upcoming").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].roundName").value("Soon Round"))
                .andExpect(jsonPath("$[1].roundName").value("Later Round"));
    }

    @Test
    void userCannotAccessAnotherUsersInterviews() throws Exception {
        String alice = registerAndGetToken("alice@example.com");
        String bob = registerAndGetToken("bob@example.com");
        long aliceApplicationId = createApplication(alice, "Alice Corp", "Dev");
        long aliceInterviewId = createInterview(alice, aliceApplicationId, "Round 1", LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/applications/" + aliceApplicationId + "/interviews")
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/applications/" + aliceApplicationId + "/interviews")
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(interviewBody("Injected", LocalDateTime.now().plusDays(1)))))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/interviews/" + aliceInterviewId)
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(interviewBody("Hacked", LocalDateTime.now().plusDays(1)))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/interviews/" + aliceInterviewId).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/interviews").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/interviews").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roundName").value("Round 1"));
    }

    @Test
    void deletingAnApplicationRemovesItsInterviewsAndNotes() throws Exception {
        String token = registerAndGetToken("alice@example.com");
        long applicationId = createApplication(token, "Acme", "Developer");
        createInterview(token, applicationId, "Round 1", LocalDateTime.now().plusDays(1));
        mockMvc.perform(post("/api/applications/" + applicationId + "/notes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("category", "GENERAL", "content", "A note"))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/applications/" + applicationId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/interviews").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void noteLifecycle() throws Exception {
        String token = registerAndGetToken("alice@example.com");
        long applicationId = createApplication(token, "Acme", "Developer");

        MvcResult created = mockMvc.perform(post("/api/applications/" + applicationId + "/notes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("category", "RECRUITER_CONVERSATION", "content", "Called Priya"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("RECRUITER_CONVERSATION"))
                .andExpect(jsonPath("$.content").value("Called Priya"))
                .andReturn();
        long noteId = readJson(created).get("id").asLong();

        mockMvc.perform(get("/api/applications/" + applicationId + "/notes").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(put("/api/notes/" + noteId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("category", "FOLLOW_UP", "content", "Email her on Friday"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("FOLLOW_UP"))
                .andExpect(jsonPath("$.content").value("Email her on Friday"));

        mockMvc.perform(post("/api/applications/" + applicationId + "/notes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("category", "GENERAL", "content", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").exists());

        mockMvc.perform(delete("/api/notes/" + noteId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/applications/" + applicationId + "/notes").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void userCannotAccessAnotherUsersNotes() throws Exception {
        String alice = registerAndGetToken("alice@example.com");
        String bob = registerAndGetToken("bob@example.com");
        long aliceApplicationId = createApplication(alice, "Alice Corp", "Dev");
        MvcResult created = mockMvc.perform(post("/api/applications/" + aliceApplicationId + "/notes")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("category", "PREPARATION", "content", "Private prep"))))
                .andExpect(status().isCreated())
                .andReturn();
        long noteId = readJson(created).get("id").asLong();

        mockMvc.perform(get("/api/applications/" + aliceApplicationId + "/notes").header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/notes/" + noteId)
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("category", "GENERAL", "content", "Hacked"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/notes/" + noteId).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/applications/" + aliceApplicationId + "/notes")
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("category", "GENERAL", "content", "Injected"))))
                .andExpect(status().isNotFound());
    }
}
