package com.example.queuectl.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Jsons {
    private static final ObjectMapper M = new ObjectMapper();
    public static JsonNode read(String json){ try { return M.readTree(json);} catch (Exception e){ throw new RuntimeException("Invalid JSON: "+e.getMessage()); } }
}