package com.splitcloud.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises the real controllers, service and JPA layer against an in-memory
 * H2 database, the same way the frontend will call the API.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private JsonNode post(String url, String body, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isEmpty() ? null : json.readTree(content);
    }

    @Test
    void fullExpenseAndSettlementFlow() throws Exception {
        long groupId = post("/api/groups", "{\"name\":\"Flat 4B\"}", 201).get("id").asLong();
        long a = post("/api/groups/" + groupId + "/members",
                "{\"name\":\"Asha\",\"email\":\"asha@example.com\",\"upiId\":\"asha@okaxis\"}", 201).get("id").asLong();
        long b = post("/api/groups/" + groupId + "/members", "{\"name\":\"Bilal\"}", 201).get("id").asLong();
        long c = post("/api/groups/" + groupId + "/members", "{\"name\":\"Chitra\"}", 201).get("id").asLong();

        // Asha pays 300.00 for everyone, split equally: Bilal and Chitra each owe her 100.00
        post("/api/groups/" + groupId + "/expenses", """
                {"description":"Groceries","amountPaise":30000,"payerId":%d,"splitType":"EQUAL",
                 "participants":[{"memberId":%d},{"memberId":%d},{"memberId":%d}]}
                """.formatted(a, a, b, c), 201);

        mvc.perform(get("/api/groups/" + groupId + "/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].netPaise").value(20000))
                .andExpect(jsonPath("$[1].netPaise").value(-10000))
                .andExpect(jsonPath("$[2].netPaise").value(-10000));

        mvc.perform(get("/api/groups/" + groupId + "/settlement-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].toName").value("Asha"))
                .andExpect(jsonPath("$[0].upiLink", startsWith("upi://pay?pa=asha%40okaxis")));

        // Bilal pays Asha back; only Chitra should still owe money
        post("/api/groups/" + groupId + "/settlements",
                "{\"fromMemberId\":%d,\"toMemberId\":%d,\"amountPaise\":10000}".formatted(b, a), 201);

        mvc.perform(get("/api/groups/" + groupId + "/settlement-plan"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fromName").value("Chitra"));

        mvc.perform(get("/api/reminders/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.memberName == 'Chitra')].owesPaise").value(contains(10000)));
    }

    @Test
    void invalidInputIsRejectedWith400() throws Exception {
        long groupId = post("/api/groups", "{\"name\":\"Trip\"}", 201).get("id").asLong();
        JsonNode error = post("/api/groups", "{\"name\":\"  \"}", 400);
        assertEquals(400, error.get("status").asInt());

        post("/api/groups/" + groupId + "/expenses", """
                {"description":"Nothing","amountPaise":0,"payerId":1,"splitType":"EQUAL",
                 "participants":[{"memberId":1}]}
                """, 400);
    }

    @Test
    void unknownGroupIs404() throws Exception {
        mvc.perform(get("/api/groups/999999")).andExpect(status().isNotFound());
    }

    @Test
    void demoSeedAndHealthWork() throws Exception {
        JsonNode demo = post("/api/demo", "", 201);
        assertEquals(4, demo.get("memberCount").asInt());
        assertEquals(1_950_000, demo.get("totalSpentPaise").asLong());

        mvc.perform(get("/api/groups/" + demo.get("id").asLong() + "/settlement-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(3))));

        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
