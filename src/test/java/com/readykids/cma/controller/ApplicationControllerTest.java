package com.readykids.cma.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readykids.cma.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationService service;

    @Test
    void testGetAll_returnsApplicationsList() throws Exception {
        List<Map<String, Object>> applications = new ArrayList<>();
        Map<String, Object> app1 = new LinkedHashMap<>();
        app1.put("id", "RK-2026-00001");
        app1.put("name", "John Doe");
        app1.put("email", "john@example.com");
        applications.add(app1);

        when(service.getAllApplications()).thenReturn(applications);

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("RK-2026-00001")))
                .andExpect(jsonPath("$[0].name", is("John Doe")));
    }

    @Test
    void testGetOne_withExistingId_returnsApplication() throws Exception {
        Map<String, Object> app = new LinkedHashMap<>();
        app.put("id", "RK-2026-00001");
        app.put("name", "Jane Smith");
        app.put("email", "jane@example.com");

        when(service.getApplication("RK-2026-00001")).thenReturn(app);

        mockMvc.perform(get("/api/applications/RK-2026-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("RK-2026-00001")))
                .andExpect(jsonPath("$.name", is("Jane Smith")));
    }

    @Test
    void testGetOne_withNonExistingId_returns404() throws Exception {
        when(service.getApplication("RK-2026-99999")).thenReturn(null);

        mockMvc.perform(get("/api/applications/RK-2026-99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Application not found")));
    }

    @Test
    void testCreate_withValidData_returnsCreated() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Alice",
                        "lastName": "Johnson",
                        "email": "alice@example.com"
                    }
                }
                """;

        when(service.createApplication(any())).thenReturn("RK-2026-00001");

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("RK-2026-00001")))
                .andExpect(jsonPath("$.message", is("Application submitted successfully")));
    }

    @Test
    void testCreate_withMissingFirstName_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "",
                        "lastName": "Johnson",
                        "email": "alice@example.com"
                    }
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("First name is required and must not exceed 200 characters")));
    }

    @Test
    void testCreate_withFirstNameTooLong_returnsBadRequest() throws Exception {
        String longName = "a".repeat(201);
        String requestBody = String.format("""
                {
                    "personal": {
                        "firstName": "%s",
                        "lastName": "Johnson",
                        "email": "alice@example.com"
                    }
                }
                """, longName);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("First name is required and must not exceed 200 characters")));
    }

    @Test
    void testCreate_withMissingLastName_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Alice",
                        "lastName": "",
                        "email": "alice@example.com"
                    }
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Last name is required and must not exceed 200 characters")));
    }

    @Test
    void testCreate_withLastNameTooLong_returnsBadRequest() throws Exception {
        String longName = "b".repeat(201);
        String requestBody = String.format("""
                {
                    "personal": {
                        "firstName": "Alice",
                        "lastName": "%s",
                        "email": "alice@example.com"
                    }
                }
                """, longName);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Last name is required and must not exceed 200 characters")));
    }

    @Test
    void testCreate_withMissingEmail_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Alice",
                        "lastName": "Johnson",
                        "email": ""
                    }
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Email is required and must not exceed 254 characters")));
    }

    @Test
    void testCreate_withEmailTooLong_returnsBadRequest() throws Exception {
        String longEmail = "a".repeat(246) + "@test.com"; // Total 255 chars, exceeds 254
        String requestBody = String.format("""
                {
                    "personal": {
                        "firstName": "Alice",
                        "lastName": "Johnson",
                        "email": "%s"
                    }
                }
                """, longEmail);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Email is required and must not exceed 254 characters")));
    }

    @Test
    void testCreate_withInvalidEmailFormat_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Alice",
                        "lastName": "Johnson",
                        "email": "not-an-email"
                    }
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid email format")));
    }

    @Test
    void testCreate_withEmailMissingAt_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Alice",
                        "lastName": "Johnson",
                        "email": "aliceexample.com"
                    }
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid email format")));
    }

    @Test
    void testCreate_withEmailMissingDomain_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Alice",
                        "lastName": "Johnson",
                        "email": "alice@"
                    }
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid email format")));
    }

    @Test
    void testUpdate_withValidStage_returnsOk() throws Exception {
        String requestBody = """
                {
                    "stage": "approved"
                }
                """;

        when(service.updateApplication(anyString(), any())).thenReturn(true);

        mockMvc.perform(patch("/api/applications/RK-2026-00001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Application updated")));
    }

    @Test
    void testUpdate_withInvalidStage_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "stage": "invalid-stage"
                }
                """;

        mockMvc.perform(patch("/api/applications/RK-2026-00001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid stage value")));
    }

    @Test
    void testUpdate_withAllValidStages_returnsOk() throws Exception {
        String[] validStages = {"new", "form-submitted", "checks", "review", "approved", "blocked", "registered"};

        when(service.updateApplication(anyString(), any())).thenReturn(true);

        for (String stage : validStages) {
            String requestBody = String.format("{\"stage\": \"%s\"}", stage);

            mockMvc.perform(patch("/api/applications/RK-2026-00001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Application updated")));
        }
    }

    @Test
    void testUpdate_withNonExistingId_returns404() throws Exception {
        String requestBody = """
                {
                    "stage": "approved"
                }
                """;

        when(service.updateApplication(anyString(), any())).thenReturn(false);

        mockMvc.perform(patch("/api/applications/RK-2026-99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Application not found or no valid fields")));
    }

    @Test
    void testDelete_withExistingId_returnsOk() throws Exception {
        when(service.deleteApplication("RK-2026-00001")).thenReturn(true);

        mockMvc.perform(delete("/api/applications/RK-2026-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Application deleted")));
    }

    @Test
    void testDelete_withNonExistingId_returns404() throws Exception {
        when(service.deleteApplication("RK-2026-99999")).thenReturn(false);

        mockMvc.perform(delete("/api/applications/RK-2026-99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Application not found")));
    }

    @Test
    void testAddTimeline_withValidData_returnsCreated() throws Exception {
        String requestBody = """
                {
                    "event": "Application reviewed",
                    "type": "action"
                }
                """;

        Map<String, Object> timelineEntry = new LinkedHashMap<>();
        timelineEntry.put("application_id", "RK-2026-00001");
        timelineEntry.put("event", "Application reviewed");
        timelineEntry.put("type", "action");
        timelineEntry.put("created_at", "2026-02-14 10:30");

        when(service.addTimelineEvent(anyString(), anyString(), anyString())).thenReturn(timelineEntry);

        mockMvc.perform(post("/api/applications/RK-2026-00001/timeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.application_id", is("RK-2026-00001")))
                .andExpect(jsonPath("$.event", is("Application reviewed")))
                .andExpect(jsonPath("$.type", is("action")));
    }

    @Test
    void testAddTimeline_withMissingEvent_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "type": "action"
                }
                """;

        mockMvc.perform(post("/api/applications/RK-2026-00001/timeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Event text is required")));
    }

    @Test
    void testAddTimeline_withEmptyEvent_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "event": "",
                    "type": "action"
                }
                """;

        mockMvc.perform(post("/api/applications/RK-2026-00001/timeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Event text is required")));
    }

    @Test
    void testAddTimeline_withEventTooLong_returnsBadRequest() throws Exception {
        String longEvent = "a".repeat(2001);
        String requestBody = String.format("""
                {
                    "event": "%s",
                    "type": "action"
                }
                """, longEvent);

        mockMvc.perform(post("/api/applications/RK-2026-00001/timeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Event text must not exceed 2000 characters")));
    }

    @Test
    void testAddTimeline_withInvalidType_returnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "event": "Test event",
                    "type": "invalid-type"
                }
                """;

        mockMvc.perform(post("/api/applications/RK-2026-00001/timeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid type value")));
    }

    @Test
    void testAddTimeline_withAllValidTypes_returnsCreated() throws Exception {
        String[] validTypes = {"action", "complete", "alert", "note"};

        for (String type : validTypes) {
            String requestBody = String.format("""
                    {
                        "event": "Test event for %s",
                        "type": "%s"
                    }
                    """, type, type);

            Map<String, Object> timelineEntry = new LinkedHashMap<>();
            timelineEntry.put("application_id", "RK-2026-00001");
            timelineEntry.put("event", "Test event for " + type);
            timelineEntry.put("type", type);
            timelineEntry.put("created_at", "2026-02-14 10:30");

            when(service.addTimelineEvent(anyString(), anyString(), anyString())).thenReturn(timelineEntry);

            mockMvc.perform(post("/api/applications/RK-2026-00001/timeline")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type", is(type)));
        }
    }

    @Test
    void testAddTimeline_withDefaultType_usesAction() throws Exception {
        String requestBody = """
                {
                    "event": "Test event without type"
                }
                """;

        Map<String, Object> timelineEntry = new LinkedHashMap<>();
        timelineEntry.put("application_id", "RK-2026-00001");
        timelineEntry.put("event", "Test event without type");
        timelineEntry.put("type", "action");
        timelineEntry.put("created_at", "2026-02-14 10:30");

        when(service.addTimelineEvent(anyString(), anyString(), anyString())).thenReturn(timelineEntry);

        mockMvc.perform(post("/api/applications/RK-2026-00001/timeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("action")));
    }
}
