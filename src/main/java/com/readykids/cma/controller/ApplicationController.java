package com.readykids.cma.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.readykids.cma.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return ResponseEntity.ok(service.getAllApplications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id) {
        var app = service.getApplication(id);
        if (app == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Application not found"));
        }
        return ResponseEntity.ok(app);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody JsonNode body) throws SQLException {
        JsonNode personal = body.path("personal");

        String firstName = personal.path("firstName").asText("");
        String lastName = personal.path("lastName").asText("");
        String email = personal.path("email").asText("");

        if (firstName.isEmpty() || firstName.length() > 200) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "First name is required and must not exceed 200 characters"));
        }

        if (lastName.isEmpty() || lastName.length() > 200) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Last name is required and must not exceed 200 characters"));
        }

        if (email.isEmpty() || email.length() > 254) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required and must not exceed 254 characters"));
        }

        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid email format"));
        }

        String id = service.createApplication(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", id, "message", "Application submitted successfully"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody JsonNode body) throws SQLException {
        if (body.has("stage")) {
            String stage = body.get("stage").asText("");
            String[] validStages = {"new", "form-submitted", "checks", "review", "approved", "blocked", "registered"};
            boolean isValid = false;
            for (String validStage : validStages) {
                if (validStage.equals(stage)) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid stage value"));
            }
        }

        boolean updated = service.updateApplication(id, body);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Application not found or no valid fields"));
        }
        return ResponseEntity.ok(Map.of("message", "Application updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        boolean deleted = service.deleteApplication(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Application not found"));
        }
        return ResponseEntity.ok(Map.of("message", "Application deleted"));
    }

    @PostMapping("/{id}/timeline")
    public ResponseEntity<?> addTimeline(@PathVariable String id, @RequestBody JsonNode body) {
        String event = body.has("event") ? body.get("event").asText() : null;
        String type = body.has("type") ? body.get("type").asText("action") : "action";

        if (event == null || event.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Event text is required"));
        }

        if (event.length() > 2000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Event text must not exceed 2000 characters"));
        }

        String[] validTypes = {"action", "complete", "alert", "note"};
        boolean isValidType = false;
        for (String validType : validTypes) {
            if (validType.equals(type)) {
                isValidType = true;
                break;
            }
        }
        if (!isValidType) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid type value"));
        }

        var entry = service.addTimelineEvent(id, event, type);
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }
}
