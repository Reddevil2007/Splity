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

    private JsonNode postJson(String url, String body, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isEmpty() ? null : json.readTree(content);
    }

    @Test
    void fullExpenseAndSettlementFlow() throws Exception {
        long groupId = postJson("/api/groups", "{\"name\":\"Flat 4B\"}", 201).get("id").asLong();
        long a = postJson("/api/groups/" + groupId + "/members",
                "{\"name\":\"Asha\",\"email\":\"asha@example.com\",\"upiId\":\"asha@okaxis\"}", 201).get("id").asLong();
        long b = postJson("/api/groups/" + groupId + "/members", "{\"name\":\"Bilal\"}", 201).get("id").asLong();
        long c = postJson("/api/groups/" + groupId + "/members", "{\"name\":\"Chitra\"}", 201).get("id").asLong();

        // Asha pays 300.00 for everyone, split equally: Bilal and Chitra each owe her 100.00
        postJson("/api/groups/" + groupId + "/expenses", """
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
        postJson("/api/groups/" + groupId + "/settlements",
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
        long groupId = postJson("/api/groups", "{\"name\":\"Trip\"}", 201).get("id").asLong();
        JsonNode error = postJson("/api/groups", "{\"name\":\"  \"}", 400);
        assertEquals(400, error.get("status").asInt());

        postJson("/api/groups/" + groupId + "/expenses", """
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
        JsonNode demo = postJson("/api/demo", "", 201);
        assertEquals(4, demo.get("memberCount").asInt());
        assertEquals(1_950_000, demo.get("totalSpentPaise").asLong());

        mvc.perform(get("/api/groups/" + demo.get("id").asLong() + "/settlement-plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(3))));

        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void peopleCanBeEditedAndRemovedOnlyWhenUnused() throws Exception {
        long groupId = postJson("/api/groups", "{\"name\":\"Hostel\"}", 201).get("id").asLong();
        long a = postJson("/api/groups/" + groupId + "/members", "{\"name\":\"Arjun\"}", 201).get("id").asLong();
        long extra = postJson("/api/groups/" + groupId + "/members", "{\"name\":\"Typo\"}", 201).get("id").asLong();

        mvc.perform(put("/api/groups/" + groupId + "/members/" + a).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arjun K\",\"email\":\"arjun@example.com\",\"upiId\":\"arjun@okaxis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arjun K"))
                .andExpect(jsonPath("$.upiId").value("arjun@okaxis"));

        // A person with no expenses can be removed
        mvc.perform(delete("/api/groups/" + groupId + "/members/" + extra)).andExpect(status().isNoContent());

        // Once someone has paid for something, removing them is refused with 409
        postJson("/api/groups/" + groupId + "/expenses", """
                {"description":"Maggi","amountPaise":6000,"payerId":%d,"splitType":"EQUAL",
                 "participants":[{"memberId":%d}]}
                """.formatted(a, a), 201);
        mvc.perform(delete("/api/groups/" + groupId + "/members/" + a)).andExpect(status().isConflict());
    }

    @Test
    void expensesHaveEditableDatesAndCategories() throws Exception {
        long groupId = postJson("/api/groups", "{\"name\":\"Weekend\"}", 201).get("id").asLong();
        long a = postJson("/api/groups/" + groupId + "/members", "{\"name\":\"Ira\"}", 201).get("id").asLong();

        JsonNode cab = postJson("/api/groups/" + groupId + "/expenses", """
                {"description":"Cab to station","amountPaise":40000,"payerId":%d,"splitType":"EQUAL",
                 "participants":[{"memberId":%d}],"spentOn":"2026-10-20"}
                """.formatted(a, a), 201);
        assertEquals("TRAVEL", cab.get("category").asText());
        assertEquals("2026-10-20", cab.get("spentOn").asText());

        postJson("/api/groups/" + groupId + "/expenses", """
                {"description":"Lunch","amountPaise":30000,"payerId":%d,"splitType":"EQUAL",
                 "participants":[{"memberId":%d}],"spentOn":"2026-10-21","category":"FOOD"}
                """.formatted(a, a), 201);

        // Newest date first
        mvc.perform(get("/api/groups/" + groupId + "/expenses"))
                .andExpect(jsonPath("$[0].description").value("Lunch"))
                .andExpect(jsonPath("$[1].description").value("Cab to station"));

        // Move the cab to a later day and change its category
        mvc.perform(put("/api/groups/" + groupId + "/expenses/" + cab.get("id").asLong())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spentOn\":\"2026-10-22\",\"category\":\"OTHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentOn").value("2026-10-22"))
                .andExpect(jsonPath("$.category").value("OTHER"));

        mvc.perform(get("/api/groups/" + groupId + "/expenses"))
                .andExpect(jsonPath("$[0].description").value("Cab to station"));
    }

    @Test
    void aPaymentMarkedByMistakeCanBeUndone() throws Exception {
        long groupId = postJson("/api/groups", "{\"name\":\"Rent\"}", 201).get("id").asLong();
        long a = postJson("/api/groups/" + groupId + "/members", "{\"name\":\"Neel\"}", 201).get("id").asLong();
        long b = postJson("/api/groups/" + groupId + "/members", "{\"name\":\"Ojas\"}", 201).get("id").asLong();
        postJson("/api/groups/" + groupId + "/expenses", """
                {"description":"Wifi","amountPaise":100000,"payerId":%d,"splitType":"EQUAL",
                 "participants":[{"memberId":%d},{"memberId":%d}]}
                """.formatted(a, a, b), 201);

        long paymentId = postJson("/api/groups/" + groupId + "/settlements",
                "{\"fromMemberId\":%d,\"toMemberId\":%d,\"amountPaise\":50000}".formatted(b, a), 201)
                .get("id").asLong();
        mvc.perform(get("/api/groups/" + groupId + "/settlement-plan")).andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(put("/api/groups/" + groupId + "/settlements/" + paymentId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paidOn\":\"2026-10-23\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidOn").value("2026-10-23"));

        // The UPI transfer never went through: undo it, and Ojas owes again
        mvc.perform(delete("/api/groups/" + groupId + "/settlements/" + paymentId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/groups/" + groupId + "/settlement-plan"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fromName").value("Ojas"));
    }
}
