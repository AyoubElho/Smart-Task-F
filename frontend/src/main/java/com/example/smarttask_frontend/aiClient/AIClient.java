package com.example.smarttask_frontend.aiClient;

import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.entity.User;
import com.example.smarttask_frontend.entity.CategoryDTO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AIClient {

    private static final String INSIGHTS_URL = "http://localhost:9090/ai/insights";
    private static final String PARSE_URL = "http://localhost:9090/ai/parse";
    private static final String CATEGORY_URL = "http://localhost:9090/ai/category";

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final HttpClient client = HttpClient.newHttpClient();

    // ================= INSIGHTS =================
    public static String getInsights(User user) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(INSIGHTS_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(user)
                ))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode node = mapper.readTree(response.body());
        return node.get("insight").asText();
    }

    // ================= PARSE TEXT → TASK =================
    public static Task parseTask(String text) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PARSE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(text)
                ))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return mapper.readValue(response.body(), Task.class);
    }

    // ================= CATEGORY SUGGESTION =================
    public static CategoryDTO suggestCategory(Task task) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CATEGORY_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(task)
                ))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return mapper.readValue(response.body(), CategoryDTO.class);
    }
}
