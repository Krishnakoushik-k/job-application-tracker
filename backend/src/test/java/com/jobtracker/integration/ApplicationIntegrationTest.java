package com.jobtracker.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createReadUpdateDeleteLifecycle() throws Exception {
        String token = registerAndGetToken("alice@example.com");

        Map<String, Object> body = new HashMap<>(applicationBody("Acme", "Java Developer"));
        body.put("location", "Hyderabad");
        body.put("salaryRange", "INR 8-10 LPA");
        body.put("recruiterName", "Priya");
        body.put("recruiterEmail", "priya@acme.example");
        body.put("jobUrl", "https://example.com/jobs/1");
        body.put("notes", "Referred by a friend");

        MvcResult created = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Acme"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.recruiterEmail").value("priya@acme.example"))
                .andReturn();
        long id = readJson(created).get("id").asLong();

        mockMvc.perform(get("/api/applications/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Java Developer"))
                .andExpect(jsonPath("$.notes").value("Referred by a friend"));

        body.put("status", "INTERVIEW");
        body.put("companyName", "Acme Corp");
        mockMvc.perform(put("/api/applications/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"))
                .andExpect(jsonPath("$.companyName").value("Acme Corp"));

        mockMvc.perform(delete("/api/applications/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/applications/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void validationRejectsBadInput() throws Exception {
        String token = registerAndGetToken("alice@example.com");

        Map<String, Object> body = new HashMap<>();
        body.put("companyName", "");
        body.put("jobTitle", "Dev");
        body.put("jobType", "FULL_TIME");
        body.put("applicationDate", "2026-11-01");
        body.put("status", "APPLIED");
        body.put("jobUrl", "ftp://not-http");
        body.put("recruiterEmail", "nope");

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.companyName").exists())
                .andExpect(jsonPath("$.fieldErrors.jobUrl").exists())
                .andExpect(jsonPath("$.fieldErrors.recruiterEmail").exists());
    }

    @Test
    void invalidEnumValueAndMalformedJsonReturnBadRequest() throws Exception {
        String token = registerAndGetToken("alice@example.com");

        Map<String, Object> body = new HashMap<>(applicationBody("Acme", "Dev"));
        body.put("status", "DREAMING");
        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotReadUpdateOrDeleteAnotherUsersApplication() throws Exception {
        String alice = registerAndGetToken("alice@example.com");
        String bob = registerAndGetToken("bob@example.com");
        long aliceApplicationId = createApplication(alice, "Alice Corp", "Secret Role");

        // IDOR attempts: all answer 404, so the existence of the id is not revealed.
        mockMvc.perform(get("/api/applications/" + aliceApplicationId).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/applications/" + aliceApplicationId)
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(applicationBody("Hacked", "Hacked"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/applications/" + aliceApplicationId).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());

        // Alice's data is untouched.
        mockMvc.perform(get("/api/applications/" + aliceApplicationId).header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Alice Corp"));
    }

    @Test
    void listOnlyContainsOwnApplicationsAndIgnoresInjectedUserId() throws Exception {
        String alice = registerAndGetToken("alice@example.com");
        String bob = registerAndGetToken("bob@example.com");
        createApplication(alice, "Alice Corp", "Dev");

        // Bob tries to create an application claiming Alice as owner; the extra field is ignored.
        Map<String, Object> body = new HashMap<>(applicationBody("Bob Corp", "Dev"));
        body.put("userId", 1);
        body.put("user", Map.of("id", 1));
        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/applications").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("Alice Corp"));
        mockMvc.perform(get("/api/applications").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("Bob Corp"));
    }

    @Test
    void searchFilterSortAndPaginationWorkTogether() throws Exception {
        String token = registerAndGetToken("alice@example.com");
        createWithDetails(token, "Acme", "Java Developer", "FULL_TIME", "APPLIED", "2026-09-01");
        createWithDetails(token, "Globex", "Frontend Intern", "INTERNSHIP", "INTERVIEW", "2026-10-01");
        createWithDetails(token, "Initech", "Java Intern", "INTERNSHIP", "OFFER", "2026-10-15");
        createWithDetails(token, "Umbrella", "QA Engineer", "PART_TIME", "REJECTED", "2026-11-01");

        // search by company (case-insensitive) and by title
        mockMvc.perform(get("/api/applications").param("search", "acme").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("Acme"));
        mockMvc.perform(get("/api/applications").param("search", "java").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(2));

        // filters
        mockMvc.perform(get("/api/applications").param("status", "OFFER").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("Initech"));
        mockMvc.perform(get("/api/applications").param("jobType", "INTERNSHIP").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/applications")
                        .param("dateFrom", "2026-10-01").param("dateTo", "2026-10-31")
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(2));

        // combined filters
        mockMvc.perform(get("/api/applications")
                        .param("search", "java").param("jobType", "INTERNSHIP")
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("Initech"));

        // sorting
        mockMvc.perform(get("/api/applications").param("sort", "newest").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.content[0].companyName").value("Umbrella"));
        mockMvc.perform(get("/api/applications").param("sort", "oldest").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.content[0].companyName").value("Acme"));

        // pagination (page is 0-based)
        mockMvc.perform(get("/api/applications")
                        .param("size", "3").param("page", "0").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
        mockMvc.perform(get("/api/applications")
                        .param("size", "3").param("page", "1").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void invalidListParametersReturnBadRequest() throws Exception {
        String token = registerAndGetToken("alice@example.com");

        mockMvc.perform(get("/api/applications").param("status", "NOPE").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/applications").param("sort", "random").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/applications").param("dateFrom", "yesterday").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/applications")
                        .param("dateFrom", "2026-11-10").param("dateTo", "2026-11-01")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void csvExportContainsOnlyTheCallersApplications() throws Exception {
        String alice = registerAndGetToken("alice@example.com");
        String bob = registerAndGetToken("bob@example.com");
        createApplication(alice, "Alice Corp", "Dev");
        createApplication(bob, "Bob Corp", "Dev");

        MvcResult result = mockMvc.perform(get("/api/applications/export").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("job-applications.csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Alice Corp")))
                .andExpect(content().string(not(containsString("Bob Corp"))))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("Company,Job Title");
    }

    @Test
    void unknownApplicationIdReturnsNotFoundWithJsonError() throws Exception {
        String token = registerAndGetToken("alice@example.com");

        mockMvc.perform(get("/api/applications/999999").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Application not found"))
                .andExpect(jsonPath("$.path").value("/api/applications/999999"));

        mockMvc.perform(get("/api/applications/abc").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    private void createWithDetails(String token, String company, String title, String type,
                                   String status, String date) throws Exception {
        Map<String, Object> body = new HashMap<>(applicationBody(company, title));
        body.put("jobType", type);
        body.put("status", status);
        body.put("applicationDate", date);
        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isCreated());
    }
}
