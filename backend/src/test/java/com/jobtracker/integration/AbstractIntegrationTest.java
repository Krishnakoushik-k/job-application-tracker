package com.jobtracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.repository.ApplicationNoteRepository;
import com.jobtracker.repository.InterviewRepository;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full application (real security filter chain, real JPA on in-memory H2) and
 * exercises it through MockMvc, exactly like an HTTP client would.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Secret123";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JobApplicationRepository applicationRepository;
    @Autowired
    private InterviewRepository interviewRepository;
    @Autowired
    private ApplicationNoteRepository noteRepository;

    @BeforeEach
    void cleanDatabase() {
        noteRepository.deleteAll();
        interviewRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Registers a user through the real endpoint and returns the JWT. */
    protected String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("fullName", "Test User", "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("token").asText();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected static Map<String, Object> applicationBody(String company, String title) {
        return Map.of(
                "companyName", company,
                "jobTitle", title,
                "jobType", "FULL_TIME",
                "applicationDate", "2026-11-01",
                "status", "APPLIED");
    }

    /** Creates an application as the given user and returns its id. */
    protected long createApplication(String token, String company, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(applicationBody(company, title))))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }
}
