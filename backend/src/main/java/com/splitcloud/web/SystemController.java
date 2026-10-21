package com.splitcloud.web;

import com.splitcloud.service.GroupService;
import com.splitcloud.web.Dtos.GroupSummary;
import com.splitcloud.web.Dtos.ReminderView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Operational endpoints: health check, pending-dues feed, and demo data. */
@RestController
@RequestMapping("/api")
public class SystemController {

    private final GroupService service;
    private final JdbcTemplate jdbc;

    public SystemController(GroupService service, JdbcTemplate jdbc) {
        this.service = service;
        this.jdbc = jdbc;
    }

    /**
     * Probed by App Service's health check and by the Application Insights
     * availability test. Returns 503 if the database is unreachable, which fails
     * the test and fires the "api-down" alert.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "splitcloud-api");
        body.put("time", Instant.now().toString());
        try {
            jdbc.queryForObject("select 1", Integer.class);
            body.put("status", "UP");
            body.put("database", "UP");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("status", "DOWN");
            body.put("database", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }

    /** Everyone who currently owes money, across all groups (for reminder jobs). */
    @GetMapping("/reminders/pending")
    public List<ReminderView> pendingReminders() {
        return service.pendingReminders();
    }

    @PostMapping("/demo")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupSummary seedDemo() {
        return service.seedDemo();
    }
}
