package com.teachnet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void fullTeacherAndSchoolJourney() throws Exception {
        // --- sign up ---
        JsonNode teacher = register("asha@example.com", "Asha Rao", "TEACHER", null);
        JsonNode teacher2 = register("vikram@example.com", "Vikram Das", "TEACHER", null);
        JsonNode school = register("hr@school.example.com", "Sunrise Public School", "INSTITUTION", "SCHOOL");
        String tToken = teacher.get("token").asText();
        String t2Token = teacher2.get("token").asText();
        String sToken = school.get("token").asText();
        long tId = teacher.at("/user/id").asLong();
        long t2Id = teacher2.at("/user/id").asLong();
        long schoolUserId = school.at("/user/id").asLong();
        long institutionId = school.at("/user/summary/institutionId").asLong();

        // duplicate email is rejected
        call(post("/api/auth/register"), null, Map.of("email", "ASHA@example.com", "password", "password123",
                "fullName", "X", "accountType", "TEACHER")).andExpect(status().isConflict());
        // wrong password
        call(post("/api/auth/login"), null, Map.of("email", "asha@example.com", "password", "nope1234"))
                .andExpect(status().isUnauthorized());
        // login works and is case-insensitive
        call(post("/api/auth/login"), null, Map.of("email", "Asha@Example.com", "password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("TEACHER"));
        // protected endpoint without token
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        // --- profile ---
        call(put("/api/profiles/me"), tToken, Map.of("fullName", "Asha Rao", "headline", "Biology teacher",
                "city", "Pune", "yearsExperience", 5, "openToWork", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Biology teacher"));
        call(post("/api/profiles/me/subjects"), tToken,
                Map.of("subject", "Biology", "board", "CBSE", "gradeFrom", 9, "gradeTo", 12))
                .andExpect(jsonPath("$.subjects[0].subject").value("Biology"))
                .andExpect(jsonPath("$.subjects[0].id").isNumber());
        call(post("/api/profiles/me/subjects"), tToken,
                Map.of("subject", "Biology", "board", "CBSE", "gradeFrom", 12, "gradeTo", 9))
                .andExpect(status().isBadRequest());
        call(post("/api/profiles/me/qualifications"), tToken,
                Map.of("degree", "M.Sc. Botany", "institute", "Pune University", "completionYear", 2015))
                .andExpect(jsonPath("$.qualifications[0].degree").value("M.Sc. Botany"));
        call(post("/api/profiles/me/experiences"), tToken,
                Map.of("title", "Teacher", "organization", "ABC School", "startYear", 2019))
                .andExpect(jsonPath("$.experiences[0].organization").value("ABC School"));
        JsonNode withPortfolio = body(call(post("/api/profiles/me/portfolio"), tToken,
                Map.of("itemType", "DEMO_VIDEO", "title", "Cell division demo", "url", "https://youtu.be/x")));
        long portfolioId = withPortfolio.at("/portfolio/0/id").asLong();
        call(delete("/api/profiles/me/portfolio/" + portfolioId), tToken, null)
                .andExpect(jsonPath("$.portfolio.length()").value(0));
        // institutions have no teacher profile
        call(put("/api/profiles/me"), sToken, Map.of("fullName", "X", "yearsExperience", 1, "openToWork", true))
                .andExpect(status().isForbidden());

        // --- teacher search ---
        call(get("/api/teachers?subject=bio&city=pune"), tToken, null)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Asha Rao"));
        call(get("/api/teachers?board=ICSE"), tToken, null).andExpect(jsonPath("$.totalElements").value(0));

        // --- institution page ---
        call(put("/api/institutions/me"), sToken, Map.of("name", "Sunrise Public School", "institutionType", "SCHOOL",
                "board", "CBSE", "city", "Pune", "about", "A great school"))
                .andExpect(jsonPath("$.owner").value(true));
        call(post("/api/institutions/" + institutionId + "/follow"), tToken, null).andExpect(status().isNoContent());
        call(get("/api/institutions/" + institutionId), tToken, null)
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.followerCount").value(1));

        // --- jobs ---
        JsonNode job = body(call(post("/api/jobs"), sToken, Map.of("title", "PGT Biology",
                "description", "Teach Class 11-12 biology", "subject", "Biology", "board", "CBSE",
                "gradeFrom", 11, "gradeTo", 12, "employmentType", "FULL_TIME", "salaryMin", 40000,
                "salaryMax", 60000)).andExpect(status().isCreated()));
        long jobId = job.at("/job/id").asLong();
        assertThat(job.at("/job/city").asText()).isEqualTo("Pune");
        // teachers cannot post jobs
        call(post("/api/jobs"), tToken, Map.of("title", "x", "description", "x", "subject", "x",
                "employmentType", "FULL_TIME")).andExpect(status().isForbidden());
        // public job search works without login
        mvc.perform(get("/api/jobs?q=biology")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        call(get("/api/jobs/recommended"), tToken, null).andExpect(jsonPath("$[0].id").value(jobId));
        call(get("/api/jobs/" + jobId), tToken, null).andExpect(jsonPath("$.canApply").value(true));

        call(post("/api/jobs/" + jobId + "/apply"), tToken, Map.of("coverNote", "I'd love to join"))
                .andExpect(status().isCreated());
        call(post("/api/jobs/" + jobId + "/apply"), tToken, Map.of()).andExpect(status().isConflict());
        call(get("/api/jobs/" + jobId + "/applications"), tToken, null).andExpect(status().isForbidden());
        JsonNode applicants = body(call(get("/api/jobs/" + jobId + "/applications"), sToken, null));
        assertThat(applicants.size()).isEqualTo(1);
        long applicationId = applicants.get(0).get("applicationId").asLong();
        call(put("/api/applications/" + applicationId + "/status"), sToken, Map.of("status", "SHORTLISTED"))
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));
        call(get("/api/applications/mine"), tToken, null).andExpect(jsonPath("$[0].status").value("SHORTLISTED"));
        call(get("/api/jobs/mine"), sToken, null).andExpect(jsonPath("$[0].applicationCount").value(1));
        call(put("/api/jobs/" + jobId + "/status"), sToken, Map.of("status", "CLOSED"))
                .andExpect(jsonPath("$.job.status").value("CLOSED"));
        mvc.perform(get("/api/jobs")).andExpect(jsonPath("$.totalElements").value(0));
        // applicant can still see the closed job, strangers cannot
        call(get("/api/jobs/" + jobId), tToken, null).andExpect(status().isOk());
        call(get("/api/jobs/" + jobId), t2Token, null).andExpect(status().isNotFound());

        // --- connections ---
        call(get("/api/connections/suggestions"), tToken, null).andExpect(jsonPath("$[0].id").value(t2Id));
        JsonNode conn = body(call(post("/api/connections/request/" + t2Id), tToken, null));
        call(post("/api/connections/request/" + schoolUserId), tToken, null).andExpect(status().isBadRequest());
        call(get("/api/connections/pending"), t2Token, null).andExpect(jsonPath("$.incoming[0].user.id").value(tId));
        call(post("/api/connections/" + conn.get("connectionId").asLong() + "/accept"), tToken, null)
                .andExpect(status().isForbidden());
        call(post("/api/connections/" + conn.get("connectionId").asLong() + "/accept"), t2Token, null)
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        call(get("/api/profiles/" + t2Id), tToken, null).andExpect(jsonPath("$.connectionState").value("CONNECTED"));

        // --- feed ---
        JsonNode schoolPost = body(call(post("/api/posts"), sToken, Map.of("content", "Admissions open!")));
        JsonNode t2Post = body(call(post("/api/posts"), t2Token, Map.of("content", "Hello teachers")));
        call(post("/api/posts"), t2Token, Map.of("content", "   ")).andExpect(status().isBadRequest());
        call(get("/api/posts/feed"), tToken, null)
                .andExpect(jsonPath("$.content.length()").value(2)); // followed school + connection
        long postId = t2Post.get("id").asLong();
        call(post("/api/posts/" + postId + "/like"), tToken, null).andExpect(status().isNoContent());
        call(post("/api/posts/" + postId + "/like"), tToken, null).andExpect(status().isNoContent());
        call(post("/api/posts/" + postId + "/comments"), tToken, Map.of("content", "Welcome!"))
                .andExpect(status().isCreated());
        call(get("/api/posts/" + postId), tToken, null)
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.commentCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));
        call(delete("/api/posts/" + schoolPost.get("id").asLong()), tToken, null).andExpect(status().isForbidden());

        // --- notifications: connection request + like + comment for t2 ---
        call(get("/api/notifications/unread-count"), t2Token, null).andExpect(jsonPath("$.count").value(3));
        call(post("/api/notifications/read-all"), t2Token, null).andExpect(status().isNoContent());
        call(get("/api/notifications/unread-count"), t2Token, null).andExpect(jsonPath("$.count").value(0));

        // --- messaging ---
        JsonNode convo = body(call(post("/api/conversations/with/" + tId), sToken, null));
        long convoId = convo.get("id").asLong();
        call(post("/api/conversations/" + convoId + "/messages"), sToken, Map.of("body", "Can you interview Monday?"))
                .andExpect(status().isCreated());
        call(get("/api/conversations/unread-count"), tToken, null).andExpect(jsonPath("$.count").value(1));
        call(get("/api/conversations"), tToken, null)
                .andExpect(jsonPath("$[0].lastMessage").value("Can you interview Monday?"))
                .andExpect(jsonPath("$[0].otherUser.fullName").value("Sunrise Public School"));
        call(get("/api/conversations/" + convoId + "/messages"), tToken, null)
                .andExpect(jsonPath("$.content[0].mine").value(false));
        call(get("/api/conversations/unread-count"), tToken, null).andExpect(jsonPath("$.count").value(0));
        call(get("/api/conversations/" + convoId + "/messages"), t2Token, null).andExpect(status().isNotFound());

        // --- admin ---
        call(get("/api/admin/stats"), tToken, null).andExpect(status().isForbidden());
        String adminToken = body(call(post("/api/auth/login"), null,
                Map.of("email", "admin@test.local", "password", "admin-password"))).get("token").asText();
        call(get("/api/admin/stats"), adminToken, null)
                .andExpect(jsonPath("$.teachers").value(2))
                .andExpect(jsonPath("$.institutions").value(1));
        call(post("/api/admin/teachers/" + tId + "/verify"), adminToken, null).andExpect(status().isNoContent());
        call(get("/api/profiles/" + tId), t2Token, null).andExpect(jsonPath("$.verified").value(true));
        call(post("/api/admin/users/" + t2Id + "/enabled?enabled=false"), adminToken, null)
                .andExpect(status().isNoContent());
        call(post("/api/auth/login"), null, Map.of("email", "vikram@example.com", "password", "password123"))
                .andExpect(status().isForbidden());
    }

    private JsonNode register(String email, String name, String type, String institutionType) throws Exception {
        var req = new java.util.HashMap<String, Object>(Map.of(
                "email", email, "password", "password123", "fullName", name, "accountType", type));
        if (institutionType != null) {
            req.put("institutionType", institutionType);
        }
        return body(call(post("/api/auth/register"), null, req).andExpect(status().isCreated()));
    }

    private ResultActions call(MockHttpServletRequestBuilder req, String token, Object body) throws Exception {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(req);
    }

    private JsonNode body(ResultActions result) throws Exception {
        String content = result.andReturn().getResponse().getContentAsString();
        return content.isEmpty() ? json.nullNode() : json.readTree(content);
    }
}
