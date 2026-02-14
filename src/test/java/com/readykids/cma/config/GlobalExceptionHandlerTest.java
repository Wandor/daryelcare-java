package com.readykids.cma.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.readykids.cma.controller.ApplicationController;
import com.readykids.cma.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService service;

    @Test
    void testHandleGenericException_returns500WithSafeMessage() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "test@example.com"
                    }
                }
                """;

        when(service.createApplication(any(JsonNode.class)))
                .thenThrow(new RuntimeException("Internal error details"));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("An error occurred processing your request")));
    }

    @Test
    void testHandleSQLException_returns500WithDatabaseError() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "test@example.com"
                    }
                }
                """;

        when(service.createApplication(any(JsonNode.class)))
                .thenThrow(new SQLException("Database connection failed"));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Database error occurred")));
    }

    @Test
    void testHandlePSQLException_withConstraintViolation_returns400() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "test@example.com"
                    }
                }
                """;

        PSQLException psqlException = new PSQLException("Unique constraint violation", PSQLState.UNIQUE_VIOLATION);

        when(service.createApplication(any(JsonNode.class)))
                .thenThrow(psqlException);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid data: constraint violation")));
    }

    @Test
    void testHandlePSQLException_withNonConstraintError_returns500() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "test@example.com"
                    }
                }
                """;

        PSQLException psqlException = new PSQLException("Syntax error", PSQLState.SYNTAX_ERROR);

        when(service.createApplication(any(JsonNode.class)))
                .thenThrow(psqlException);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Database error occurred")));
    }

    @Test
    void testHandleNullPointerException_returns500WithSafeMessage() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "test@example.com"
                    }
                }
                """;

        when(service.createApplication(any(JsonNode.class)))
                .thenThrow(new NullPointerException("Null value encountered"));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("An error occurred processing your request")));
    }

    @Test
    void testHandleIllegalArgumentException_returns500WithSafeMessage() throws Exception {
        String requestBody = """
                {
                    "personal": {
                        "firstName": "Test",
                        "lastName": "User",
                        "email": "test@example.com"
                    }
                }
                """;

        when(service.createApplication(any(JsonNode.class)))
                .thenThrow(new IllegalArgumentException("Invalid argument"));

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("An error occurred processing your request")));
    }
}
