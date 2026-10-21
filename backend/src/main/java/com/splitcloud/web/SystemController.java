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

/** Operational endpoints: health check, reminder feed for the Lambda, and demo data. */
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
     * Polled every minute by the health-check Lambda. Returns 503 if the database
     * is unreachable, which makes the Lambda fail and the CloudWatch alarm fire.
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

    /** Read by the scheduled reminder Lambda, which emails a dues digest through SNS. */
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
