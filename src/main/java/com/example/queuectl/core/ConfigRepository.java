package com.example.queuectl.core;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigRepository {
    private final JdbcTemplate jdbc;
    public ConfigRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}

    public int getBackoffBase(){ return jdbc.queryForObject("SELECT value FROM config WHERE key='backoff_base'", Integer.class); }
    public int getDefaultMaxRetries(){ return jdbc.queryForObject("SELECT value FROM config WHERE key='default_max_retries'", Integer.class); }
    public void setBackoffBase(int base){ jdbc.update("UPDATE config SET value=? WHERE key='backoff_base'", base); }
    public void setDefaultMaxRetries(int v){ jdbc.update("UPDATE config SET value=? WHERE key='default_max_retries'", v); }

    public boolean isStopAll(){ return jdbc.queryForObject("SELECT value FROM control WHERE key='stop_all'", Integer.class) == 1; }
    public void setStopAll(boolean b){ jdbc.update("UPDATE control SET value=? WHERE key='stop_all'", b?1:0); }
}
