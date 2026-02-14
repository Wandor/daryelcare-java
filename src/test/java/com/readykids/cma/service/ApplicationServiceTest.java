package com.readykids.cma.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationServiceTest {

    private ApplicationService service;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // null JdbcTemplate is safe: tested methods (escapeHtml, generateId, calculateProgress) don't use it
        service = new ApplicationService(null, mapper);
    }

    @Test
    void testEscapeHtml_withNull_returnsNull() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, (String) null);
        assertNull(result);
    }

    @Test
    void testEscapeHtml_withPlainText_returnsUnchanged() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, "Hello World");
        assertEquals("Hello World", result);
    }

    @Test
    void testEscapeHtml_withAmpersand_escapesCorrectly() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, "Tom & Jerry");
        assertEquals("Tom &amp; Jerry", result);
    }

    @Test
    void testEscapeHtml_withLessThan_escapesCorrectly() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, "5 < 10");
        assertEquals("5 &lt; 10", result);
    }

    @Test
    void testEscapeHtml_withGreaterThan_escapesCorrectly() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, "10 > 5");
        assertEquals("10 &gt; 5", result);
    }

    @Test
    void testEscapeHtml_withDoubleQuote_escapesCorrectly() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, "She said \"Hello\"");
        assertEquals("She said &quot;Hello&quot;", result);
    }

    @Test
    void testEscapeHtml_withSingleQuote_escapesCorrectly() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, "It's mine");
        assertEquals("It&#x27;s mine", result);
    }

    @Test
    void testEscapeHtml_withAllSpecialChars_escapesCorrectly() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("escapeHtml", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, "<script>alert('XSS & \"danger\"')</script>");
        assertEquals("&lt;script&gt;alert(&#x27;XSS &amp; &quot;danger&quot;&#x27;)&lt;/script&gt;", result);
    }

    @Test
    void testGenerateId_withValidSequence_returnsCorrectFormat() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("generateId", long.class);
        method.setAccessible(true);

        int currentYear = LocalDate.now().getYear();
        String result = (String) method.invoke(service, 1L);
        assertEquals(String.format("RK-%d-00001", currentYear), result);
    }

    @Test
    void testGenerateId_withLargeSequence_padsCorrectly() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("generateId", long.class);
        method.setAccessible(true);

        int currentYear = LocalDate.now().getYear();
        String result = (String) method.invoke(service, 12345L);
        assertEquals(String.format("RK-%d-12345", currentYear), result);
    }

    @Test
    void testGenerateId_withSmallSequence_padsWithZeros() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("generateId", long.class);
        method.setAccessible(true);

        int currentYear = LocalDate.now().getYear();
        String result = (String) method.invoke(service, 42L);
        assertEquals(String.format("RK-%d-00042", currentYear), result);
    }

    @Test
    void testCalculateProgress_withNull_returnsZero() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("calculateProgress", JsonNode.class);
        method.setAccessible(true);

        int result = (int) method.invoke(service, (JsonNode) null);
        assertEquals(0, result);
    }

    @Test
    void testCalculateProgress_withEmptyObject_returnsZero() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("calculateProgress", JsonNode.class);
        method.setAccessible(true);

        ObjectNode checks = mapper.createObjectNode();
        int result = (int) method.invoke(service, checks);
        assertEquals(0, result);
    }

    @Test
    void testCalculateProgress_withNoCompleteChecks_returnsZero() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("calculateProgress", JsonNode.class);
        method.setAccessible(true);

        ObjectNode checks = mapper.createObjectNode();
        ObjectNode check1 = mapper.createObjectNode();
        check1.put("status", "pending");
        checks.set("dbs", check1);

        ObjectNode check2 = mapper.createObjectNode();
        check2.put("status", "not-started");
        checks.set("ref_1", check2);

        int result = (int) method.invoke(service, checks);
        assertEquals(0, result);
    }

    @Test
    void testCalculateProgress_withSomeCompleteChecks_returnsCorrectPercentage() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("calculateProgress", JsonNode.class);
        method.setAccessible(true);

        ObjectNode checks = mapper.createObjectNode();

        ObjectNode check1 = mapper.createObjectNode();
        check1.put("status", "complete");
        checks.set("dbs", check1);

        ObjectNode check2 = mapper.createObjectNode();
        check2.put("status", "pending");
        checks.set("ref_1", check2);

        ObjectNode check3 = mapper.createObjectNode();
        check3.put("status", "not-started");
        checks.set("ref_2", check3);

        ObjectNode check4 = mapper.createObjectNode();
        check4.put("status", "complete");
        checks.set("first_aid", check4);

        // 2 out of 4 = 50%
        int result = (int) method.invoke(service, checks);
        assertEquals(50, result);
    }

    @Test
    void testCalculateProgress_withAllCompleteChecks_returnsOneHundred() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("calculateProgress", JsonNode.class);
        method.setAccessible(true);

        ObjectNode checks = mapper.createObjectNode();

        ObjectNode check1 = mapper.createObjectNode();
        check1.put("status", "complete");
        checks.set("dbs", check1);

        ObjectNode check2 = mapper.createObjectNode();
        check2.put("status", "complete");
        checks.set("ref_1", check2);

        ObjectNode check3 = mapper.createObjectNode();
        check3.put("status", "complete");
        checks.set("ref_2", check3);

        int result = (int) method.invoke(service, checks);
        assertEquals(100, result);
    }

    @Test
    void testCalculateProgress_withRoundingNeeded_roundsCorrectly() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("calculateProgress", JsonNode.class);
        method.setAccessible(true);

        ObjectNode checks = mapper.createObjectNode();

        // Create 3 checks, 1 complete = 33.33% rounds to 33
        ObjectNode check1 = mapper.createObjectNode();
        check1.put("status", "complete");
        checks.set("dbs", check1);

        ObjectNode check2 = mapper.createObjectNode();
        check2.put("status", "pending");
        checks.set("ref_1", check2);

        ObjectNode check3 = mapper.createObjectNode();
        check3.put("status", "not-started");
        checks.set("ref_2", check3);

        int result = (int) method.invoke(service, checks);
        assertEquals(33, result);
    }

    @Test
    void testCalculateProgress_withMissingStatus_treatsAsNotComplete() throws Exception {
        Method method = ApplicationService.class.getDeclaredMethod("calculateProgress", JsonNode.class);
        method.setAccessible(true);

        ObjectNode checks = mapper.createObjectNode();

        ObjectNode check1 = mapper.createObjectNode();
        check1.put("status", "complete");
        checks.set("dbs", check1);

        ObjectNode check2 = mapper.createObjectNode();
        // No status field
        checks.set("ref_1", check2);

        // 1 out of 2 = 50%
        int result = (int) method.invoke(service, checks);
        assertEquals(50, result);
    }
}
