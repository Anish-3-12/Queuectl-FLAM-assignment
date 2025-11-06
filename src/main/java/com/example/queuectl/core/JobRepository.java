package com.example.queuectl.core;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JobRepository {
    private final JdbcTemplate jdbc;
    public JobRepository(JdbcTemplate jdbc){ this.jdbc=jdbc; }

    private static final RowMapper<JobRecord> M = (rs, i) -> map(rs);

    // ---------- SAFE TIMESTAMP HANDLING FOR SQLITE ----------
    private static Instant colInstant(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        if (o == null) return null;

        // Already a Timestamp
        if (o instanceof java.sql.Timestamp t) {
            return t.toInstant();
        }

        // SQLite sometimes returns epoch millis as integer/long
        if (o instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue());
        }

        // String-based values from SQLite
        if (o instanceof String s) {
            s = s.trim();

            // Epoch millis as string
            try { return Instant.ofEpochMilli(Long.parseLong(s)); } catch (Exception ignore) {}

            // ISO-8601 timestamps
            try { return Instant.parse(s); } catch (Exception ignore) {}

            // SQLite default datetime: "yyyy-MM-dd HH:mm:ss" or with .SSS
            try {
                java.time.format.DateTimeFormatter fmt =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]");
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s, fmt);
                return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant();
            } catch (Exception ignore) {}
        }

        throw new SQLException("Unsupported timestamp type for column '" + col + "': " + o + " (" + o.getClass() + ")");
    }

    private static java.sql.Timestamp ts(Instant i){
        return i == null ? null : java.sql.Timestamp.from(i);
    }

    // ---------- ROW MAPPER ----------
    private static JobRecord map(ResultSet rs) throws SQLException {
        return new JobRecord(
                rs.getString("id"),
                rs.getString("command"),
                JobState.valueOf(rs.getString("state")),
                rs.getInt("attempts"),
                rs.getInt("max_retries"),
                colInstant(rs, "created_at"),
                colInstant(rs, "updated_at"),
                colInstant(rs, "run_at"),
                rs.getInt("priority"),
                rs.getString("last_error"),
                rs.getString("locked_by"),
                colInstant(rs, "locked_until")
        );
    }

    // ---------- CRUD ----------
    public void insert(JobRecord j){
        jdbc.update(
            "INSERT INTO jobs(id,command,state,attempts,max_retries,created_at,updated_at,run_at,priority) " +
            "VALUES(?,?,?,?,?,?,?,?,?)",
            j.id(), j.command(), j.state().name(),
            j.attempts(), j.maxRetries(),
            ts(j.createdAt()), ts(j.updatedAt()), ts(j.runAt()),
            j.priority()
        );
    }

    public List<JobRecord> findAll(int limit){
        return jdbc.query("SELECT * FROM jobs ORDER BY created_at DESC LIMIT ?", M, limit);
    }

    public List<JobRecord> findByState(JobState s){
        return jdbc.query("SELECT * FROM jobs WHERE state=? ORDER BY created_at DESC", M, s.name());
    }

    public List<JobRecord> findDead(int limit){
        return jdbc.query("SELECT * FROM jobs WHERE state='dead' ORDER BY updated_at DESC LIMIT ?", M, limit);
    }

    public Map<String, Integer> countsByState(){
        Map<String,Integer> map = new LinkedHashMap<>();
        jdbc.query(
            "SELECT state, COUNT(*) c FROM jobs GROUP BY state",
            (org.springframework.jdbc.core.ResultSetExtractor<Void>) rs -> {
                while (rs.next()) {
                    map.put(rs.getString("state"), rs.getInt("c"));
                }
                return null;
            }
        );
        return map;
    }

    public int countActiveWorkers(){
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM workers WHERE last_heartbeat > datetime('now','-30 seconds')",
            Integer.class
        );
    }

    public void retryDead(String id){
        jdbc.update(
            "UPDATE jobs SET state='pending', attempts=0, last_error=NULL, " +
            "locked_by=NULL, locked_until=NULL, updated_at=datetime('now'), run_at=datetime('now') " +
            "WHERE id=? AND state='dead'",
            id
        );
    }

    // ---------- JOB CLAIMING ----------
    public JobRecord claimNext(String workerName){
        List<String> ids = jdbc.queryForList(
            "SELECT id FROM jobs WHERE state='pending' AND run_at <= datetime('now') " +
            "AND (locked_until IS NULL OR locked_until < datetime('now')) " +
            "ORDER BY priority DESC, created_at ASC LIMIT 5",
            String.class
        );

        for (String id : ids){
            int updated = jdbc.update(
                "UPDATE jobs SET state='processing', locked_by=?, locked_until=datetime('now','+5 minutes'), " +
                "updated_at=datetime('now') WHERE id=? AND state='pending' " +
                "AND (locked_until IS NULL OR locked_until < datetime('now'))",
                workerName, id
            );

            if (updated == 1){
                return get(id);
            }
        }
        return null;
    }

    public JobRecord get(String id){
        return jdbc.queryForObject("SELECT * FROM jobs WHERE id=?", M, id);
    }

    public void heartbeat(String workerName){
        jdbc.update(
            "INSERT INTO workers(name,last_heartbeat) VALUES(?,datetime('now')) " +
            "ON CONFLICT(name) DO UPDATE SET last_heartbeat=excluded.last_heartbeat",
            workerName
        );
    }

    public void complete(String id){
        jdbc.update(
            "UPDATE jobs SET state='completed', locked_by=NULL, locked_until=NULL, updated_at=datetime('now') WHERE id=?",
            id
        );
    }

    public void failAndScheduleRetry(String id, int attempts, int max, int delaySeconds, String error){
        if (attempts + 1 > max){
            jdbc.update(
                "UPDATE jobs SET state='dead', attempts=?, last_error=?, locked_by=NULL, locked_until=NULL, " +
                "updated_at=datetime('now') WHERE id=?",
                attempts+1, truncate(error, 8000), id
            );
        } else {
            jdbc.update(
                "UPDATE jobs SET state='pending', attempts=?, last_error=?, locked_by=NULL, locked_until=NULL, " +
                "run_at=datetime('now', ?), updated_at=datetime('now') WHERE id=?",
                attempts+1, truncate(error, 8000), "+" + delaySeconds + " seconds", id
            );
        }
    }

    private static String truncate(String s, int n){
        if (s == null) return null;
        return s.length() <= n ? s : s.substring(0, n);
    }

    // ---------- REPAIR ----------
    public int repairProcessing() {
        return jdbc.update(
            "UPDATE jobs SET state='pending', locked_by=NULL, locked_until=NULL WHERE state='processing'"
        );
    }
}
