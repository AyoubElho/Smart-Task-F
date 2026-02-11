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

    private static final String PARSE_URL = "http://localhost:9090/ai/parse";


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

}
