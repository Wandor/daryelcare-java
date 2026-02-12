package com.readykids.cma.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ApplicationService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ApplicationService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    private String generateId(long seqVal) {
        int year = LocalDate.now().getYear();
        return String.format("RK-%d-%05d", year, seqVal);
    }

    private int calculateProgress(JsonNode checks) {
        if (checks == null || !checks.isObject() || checks.isEmpty()) return 0;
        int total = checks.size();
        int complete = 0;
        var it = checks.fields();
        while (it.hasNext()) {
            var entry = it.next();
            JsonNode status = entry.getValue().path("status");
            if ("complete".equals(status.asText(""))) complete++;
        }
        return (int) Math.round((double) complete / total * 100);
    }

    private ObjectNode buildChecksFromForm(JsonNode body) {
        ObjectNode checks = mapper.createObjectNode();
        String[] checkNames = {"dbs", "dbs_update", "la_check", "ofsted", "gp_health",
                "ref_1", "ref_2", "first_aid", "safeguarding", "food_hygiene", "insurance"};
        for (String name : checkNames) {
            ObjectNode c = mapper.createObjectNode();
            c.put("status", "not-started");
            c.putNull("date");
            checks.set(name, c);
        }

        String today = LocalDate.now().toString();

        // DBS from suitability
        JsonNode suit = body.path("suitability");
        if ("Yes".equals(suit.path("hasDBS").asText("")) && suit.has("dbsNumber")) {
            ObjectNode dbs = mapper.createObjectNode();
            dbs.put("status", "pending");
            dbs.put("date", today);
            dbs.put("certificate", suit.path("dbsNumber").asText());
            dbs.put("details", "Certificate number provided on application");
            checks.set("dbs", dbs);
        }

        // Qualifications
        JsonNode quals = body.path("qualifications");
        if ("Yes".equals(quals.path("firstAidCompleted").asText(""))) {
            ObjectNode fa = mapper.createObjectNode();
            fa.put("status", "complete");
            fa.put("date", quals.path("firstAidDate").asText(""));
            fa.put("provider", quals.path("firstAidOrg").asText(""));
            checks.set("first_aid", fa);
        }
        if ("Yes".equals(quals.path("safeguardingCompleted").asText(""))) {
            ObjectNode sg = mapper.createObjectNode();
            sg.put("status", "complete");
            sg.put("date", quals.path("safeguardingDate").asText(""));
            sg.put("provider", quals.path("safeguardingOrg").asText(""));
            checks.set("safeguarding", sg);
        }
        if ("Yes".equals(quals.path("foodHygieneCompleted").asText(""))) {
            ObjectNode fh = mapper.createObjectNode();
            fh.put("status", "complete");
            fh.put("date", quals.path("foodHygieneDate").asText(""));
            fh.put("provider", quals.path("foodHygieneOrg").asText(""));
            checks.set("food_hygiene", fh);
        }

        // References
        JsonNode refs = body.path("references");
        String ref1Name = refs.path("ref1").path("name").asText("");
        if (!ref1Name.isEmpty()) {
            ObjectNode r1 = mapper.createObjectNode();
            r1.put("status", "pending");
            r1.put("date", today);
            r1.put("referee", ref1Name);
            r1.put("relationship", refs.path("ref1").path("relationship").asText(""));
            r1.put("details", "Reference request to be sent");
            checks.set("ref_1", r1);
        }
        String ref2Name = refs.path("ref2").path("name").asText("");
        if (!ref2Name.isEmpty()) {
            ObjectNode r2 = mapper.createObjectNode();
            r2.put("status", "pending");
            r2.put("date", today);
            r2.put("referee", ref2Name);
            r2.put("relationship", refs.path("ref2").path("relationship").asText(""));
            r2.put("details", "Reference request to be sent");
            checks.set("ref_2", r2);
        }

        return checks;
    }

    private ArrayNode buildConnectedPersons(JsonNode body) {
        ArrayNode persons = mapper.createArrayNode();
        JsonNode adults = body.path("household").path("adults");
        if (!adults.isArray()) return persons;

        for (int i = 0; i < adults.size(); i++) {
            JsonNode a = adults.get(i);
            String first = a.path("firstName").asText("");
            String last = a.path("lastName").asText("");
            if (first.isEmpty() || last.isEmpty()) continue;

            ObjectNode p = mapper.createObjectNode();
            p.put("id", String.format("CP-NEW-%03d", i + 1));
            p.put("name", first + " " + last);
            p.put("type", "household");
            p.put("relationship", a.has("relationship") ? a.path("relationship").asText() : "Household member");
            p.put("dob", a.path("dob").asText(""));
            p.put("formStatus", "not-started");
            p.put("formType", "CMA-H2");
            ObjectNode cpChecks = mapper.createObjectNode();
            ObjectNode dbsCheck = mapper.createObjectNode();
            dbsCheck.put("status", "not-started");
            dbsCheck.putNull("date");
            cpChecks.set("dbs", dbsCheck);
            ObjectNode laCheck = mapper.createObjectNode();
            laCheck.put("status", "not-started");
            laCheck.putNull("date");
            cpChecks.set("la_check", laCheck);
            p.set("checks", cpChecks);
            persons.add(p);
        }
        return persons;
    }

    private String buildPremisesAddress(JsonNode body) {
        JsonNode premises = body.path("premises");
        String pType = premises.path("type").asText("Domestic");
        boolean sameAsHome = premises.path("sameAsHome").asBoolean(true);

        JsonNode addr;
        if ("Domestic".equals(pType) && sameAsHome) {
            addr = body.path("homeAddress");
        } else {
            addr = premises.path("address");
        }

        if (addr.isMissingNode() || addr.isNull()) return null;

        List<String> parts = new ArrayList<>();
        for (String key : new String[]{"line1", "line2", "town", "postcode"}) {
            String val = addr.path(key).asText("");
            if (!val.isEmpty()) parts.add(val);
        }
        String result = String.join(", ", parts);
        return result.isEmpty() ? null : result;
    }

    private PGobject jsonb(String json) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType("jsonb");
        obj.setValue(json);
        return obj;
    }

    private PGobject jsonbNode(JsonNode node) throws SQLException {
        if (node == null || node.isNull() || node.isMissingNode()) return jsonb(null);
        try {
            return jsonb(mapper.writeValueAsString(node));
        } catch (JsonProcessingException e) {
            return jsonb(null);
        }
    }

    private String nodeStr(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        try {
            return mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String formatDate(Object val) {
        if (val == null) return null;
        if (val instanceof java.sql.Date d) return d.toLocalDate().toString();
        if (val instanceof Timestamp ts) return ts.toLocalDateTime().toLocalDate().toString();
        return val.toString().length() >= 10 ? val.toString().substring(0, 10) : val.toString();
    }

    private String formatDatetime(Object val) {
        if (val == null) return null;
        if (val instanceof Timestamp ts) {
            return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        String s = val.toString();
        return s.length() >= 16 ? s.substring(0, 16) : s;
    }

    private JsonNode parseJsonb(Object val) {
        if (val == null) return null;
        String str = val.toString();
        if (str.isEmpty()) return null;
        try {
            return mapper.readTree(str);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Map<String, Object> toDashboardShape(ResultSet rs, List<Map<String, Object>> timeline) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        Timestamp lastUpdatedTs = rs.getTimestamp("last_updated");
        LocalDateTime lastUpdated = lastUpdatedTs != null ? lastUpdatedTs.toLocalDateTime() : now;
        long daysInStage = Math.max(0, ChronoUnit.DAYS.between(lastUpdated, now));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", rs.getString("id"));
        result.put("name", rs.getString("name"));
        result.put("email", rs.getString("email"));
        result.put("phone", rs.getString("phone") != null ? rs.getString("phone") : "");
        result.put("dob", formatDate(rs.getDate("dob")));
        result.put("stage", rs.getString("stage"));
        result.put("startDate", formatDate(rs.getDate("start_date")));
        result.put("registrationDate", formatDate(rs.getDate("registration_date")));
        result.put("lastUpdated", formatDate(rs.getTimestamp("last_updated")));
        result.put("daysInStage", daysInStage);
        result.put("risk", rs.getString("risk"));
        result.put("progress", rs.getInt("progress"));
        result.put("premisesType", rs.getString("premises_type") != null ? rs.getString("premises_type") : "");
        result.put("premisesAddress", rs.getString("premises_address") != null ? rs.getString("premises_address") : "");
        result.put("localAuthority", rs.getString("local_authority") != null ? rs.getString("local_authority") : "");

        JsonNode registers = parseJsonb(rs.getString("registers"));
        result.put("registers", registers != null ? registers : mapper.createArrayNode());
        JsonNode checks = parseJsonb(rs.getString("checks"));
        result.put("checks", checks != null ? checks : mapper.createObjectNode());
        JsonNode connected = parseJsonb(rs.getString("connected_persons"));
        result.put("connectedPersons", connected != null ? connected : mapper.createArrayNode());

        List<Map<String, Object>> tlFormatted = new ArrayList<>();
        for (int i = timeline.size() - 1; i >= 0; i--) {
            Map<String, Object> t = timeline.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", formatDatetime(t.get("created_at")));
            entry.put("event", t.get("event"));
            entry.put("type", t.get("type"));
            tlFormatted.add(entry);
        }
        result.put("timeline", tlFormatted);

        String niNumber = rs.getString("ni_number");
        if (niNumber != null) result.put("niNumber", niNumber);
        String regNum = rs.getString("registration_number");
        if (regNum != null) result.put("registrationNumber", regNum);
        JsonNode ofsted = parseJsonb(rs.getString("ofsted_check"));
        if (ofsted != null) result.put("ofstedCheck", ofsted);
        JsonNode household = parseJsonb(rs.getString("household"));
        if (household != null) result.put("household", household);
        JsonNode service = parseJsonb(rs.getString("service"));
        if (service != null) result.put("service", service);
        JsonNode premisesDetails = parseJsonb(rs.getString("premises_details"));
        if (premisesDetails != null) result.put("premisesDetails", premisesDetails);

        return result;
    }

    private List<Map<String, Object>> getTimeline(String applicationId) {
        return jdbc.query(
                "SELECT event, type, created_at FROM timeline_events WHERE application_id = ? ORDER BY created_at ASC",
                (rs, rowNum) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("event", rs.getString("event"));
                    m.put("type", rs.getString("type"));
                    m.put("created_at", rs.getTimestamp("created_at"));
                    return m;
                },
                applicationId
        );
    }

    @Transactional
    public String createApplication(JsonNode body) throws SQLException {
        Long seqVal = jdbc.queryForObject("SELECT nextval('application_id_seq')", Long.class);
        String id = generateId(seqVal);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        ObjectNode checks = buildChecksFromForm(body);
        ArrayNode connected = buildConnectedPersons(body);
        int progress = calculateProgress(checks);
        String premisesAddr = buildPremisesAddress(body);

        JsonNode serviceNode = body.path("service");
        String registers = nodeStr(serviceNode.path("ageGroups"));
        if (registers == null) registers = "[]";

        JsonNode personal = body.path("personal");
        JsonNode premises = body.path("premises");

        ObjectNode premisesDetails = mapper.createObjectNode();
        premisesDetails.set("sameAsHome", premises.path("sameAsHome").deepCopy());
        premisesDetails.set("outdoorSpace", premises.path("outdoorSpace").deepCopy());
        premisesDetails.set("pets", premises.path("pets").deepCopy());
        premisesDetails.set("petsDetails", premises.path("petsDetails").deepCopy());

        String sql = """
                INSERT INTO applications (
                    id, title, first_name, middle_names, last_name,
                    email, phone, dob, gender, right_to_work, ni_number,
                    home_address, premises_type, premises_address,
                    premises_details, local_authority,
                    registers, service, stage, risk, progress,
                    checks, connected_persons,
                    previous_names, address_history, qualifications,
                    employment_history, references_data,
                    household, suitability, declaration,
                    start_date, last_updated, created_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?,
                    ?, ?, 'new', 'low', ?,
                    ?, ?,
                    ?, ?, ?,
                    ?, ?,
                    ?, ?, ?,
                    ?, ?, ?
                )
                """;

        String premisesType = premises.path("type").asText("domestic").toLowerCase();

        jdbc.update(sql,
                id,
                textOrNull(personal.path("title")),
                personal.path("firstName").asText(""),
                textOrNull(personal.path("middleNames")),
                personal.path("lastName").asText(""),
                personal.path("email").asText(""),
                textOrNull(personal.path("phone")),
                textOrNull(personal.path("dob")),
                textOrNull(personal.path("gender")),
                textOrNull(personal.path("rightToWork")),
                textOrNull(personal.path("niNumber")),
                jsonbNode(body.path("homeAddress")),
                premisesType,
                premisesAddr,
                jsonbNode(premisesDetails),
                textOrNull(premises.path("localAuthority")),
                jsonb(registers),
                jsonbNode(serviceNode),
                progress,
                jsonbNode(checks),
                jsonbNode(connected),
                jsonbNode(body.path("previousNames")),
                jsonbNode(body.path("addressHistory")),
                jsonbNode(body.path("qualifications")),
                jsonbNode(body.path("employment")),
                jsonbNode(body.path("references")),
                jsonbNode(body.path("household")),
                jsonbNode(body.path("suitability")),
                jsonbNode(body.path("declaration")),
                now, now, now
        );

        jdbc.update(
                "INSERT INTO timeline_events (application_id, event, type, created_at) VALUES (?, 'Application started', 'action', ?)",
                id, now);
        jdbc.update(
                "INSERT INTO timeline_events (application_id, event, type, created_at) VALUES (?, 'Application form submitted', 'complete', ?)",
                id, new Timestamp(now.getTime() + 1000));

        return id;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return null;
        String val = node.asText("");
        return val.isEmpty() ? null : val;
    }

    public List<Map<String, Object>> getAllApplications() {
        List<Map<String, Object>> results = new ArrayList<>();

        List<Map<String, Object>> rows = jdbc.query(
                "SELECT * FROM applications ORDER BY created_at DESC",
                (rs, rowNum) -> {
                    String appId = rs.getString("id");
                    List<Map<String, Object>> tl = new ArrayList<>();
                    return toDashboardShape(rs, tl);
                }
        );

        for (Map<String, Object> row : rows) {
            String appId = (String) row.get("id");
            List<Map<String, Object>> tl = getTimeline(appId);
            List<Map<String, Object>> tlFormatted = new ArrayList<>();
            for (int i = tl.size() - 1; i >= 0; i--) {
                Map<String, Object> t = tl.get(i);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("date", formatDatetime(t.get("created_at")));
                entry.put("event", t.get("event"));
                entry.put("type", t.get("type"));
                tlFormatted.add(entry);
            }
            row.put("timeline", tlFormatted);
            results.add(row);
        }

        return results;
    }

    public Map<String, Object> getApplication(String id) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT * FROM applications WHERE id = ?",
                (rs, rowNum) -> toDashboardShape(rs, new ArrayList<>()),
                id
        );

        if (rows.isEmpty()) return null;

        Map<String, Object> app = rows.get(0);
        List<Map<String, Object>> tl = getTimeline(id);
        List<Map<String, Object>> tlFormatted = new ArrayList<>();
        for (int i = tl.size() - 1; i >= 0; i--) {
            Map<String, Object> t = tl.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", formatDatetime(t.get("created_at")));
            entry.put("event", t.get("event"));
            entry.put("type", t.get("type"));
            tlFormatted.add(entry);
        }
        app.put("timeline", tlFormatted);
        return app;
    }

    public boolean updateApplication(String id, JsonNode updates) throws SQLException {
        Map<String, String[]> allowed = Map.ofEntries(
                Map.entry("stage", new String[]{"stage", "text"}),
                Map.entry("risk", new String[]{"risk", "text"}),
                Map.entry("progress", new String[]{"progress", "text"}),
                Map.entry("checks", new String[]{"checks", "jsonb"}),
                Map.entry("connected_persons", new String[]{"connected_persons", "jsonb"}),
                Map.entry("connectedPersons", new String[]{"connected_persons", "jsonb"}),
                Map.entry("ofsted_check", new String[]{"ofsted_check", "jsonb"}),
                Map.entry("ofstedCheck", new String[]{"ofsted_check", "jsonb"}),
                Map.entry("registration_date", new String[]{"registration_date", "text"}),
                Map.entry("registrationDate", new String[]{"registration_date", "text"}),
                Map.entry("registration_number", new String[]{"registration_number", "text"}),
                Map.entry("registrationNumber", new String[]{"registration_number", "text"})
        );

        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        var fields = updates.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            String[] mapping = allowed.get(entry.getKey());
            if (mapping == null) continue;
            sets.add(mapping[0] + " = ?");
            if ("jsonb".equals(mapping[1])) {
                params.add(jsonbNode(entry.getValue()));
            } else {
                params.add(entry.getValue().asText());
            }
        }

        if (sets.isEmpty()) return false;

        sets.add("last_updated = NOW()");
        params.add(id);
        String sql = "UPDATE applications SET " + String.join(", ", sets) + " WHERE id = ?";
        int rows = jdbc.update(sql, params.toArray());
        return rows > 0;
    }

    public boolean deleteApplication(String id) {
        return jdbc.update("DELETE FROM applications WHERE id = ?", id) > 0;
    }

    public Map<String, Object> addTimelineEvent(String id, String event, String type) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update(
                "INSERT INTO timeline_events (application_id, event, type, created_at) VALUES (?, ?, ?, ?)",
                id, event, type, now);

        return Map.of(
                "application_id", id,
                "event", event,
                "type", type,
                "created_at", formatDatetime(now)
        );
    }
}
