package com.example.smarttask_frontend.subtasks.service;

import com.example.smarttask_frontend.AppConfig;
import com.example.smarttask_frontend.entity.SubTask;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class SubTaskService {

    // ✅ SAME STYLE AS TaskService (VERY IMPORTANT)
    private static final String BASE_URL =
            AppConfig.get("backend.base-url").endsWith("/")
                    ? AppConfig.get("backend.base-url") + "subtasks"
                    : AppConfig.get("backend.base-url") + "/subtasks";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================
    // GET SUBTASKS BY TASK ID
    // =========================
    public List<SubTask> getSubTasksByTaskId(Long taskId) {
        try {
            String url = BASE_URL + "/task/" + taskId;
            System.out.println("GET " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Failed to fetch subtasks, status=" + response.statusCode());
            }

            return objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<SubTask>>() {}
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch subtasks", e);
        }
    }

    // =========================
    // UPDATE SUBTASK STATUS
    // =========================
    public void updateSubTaskStatus(Long subTaskId, boolean completed) {
        try {
            String url = BASE_URL + "/" + subTaskId + "/status?is_completed=" + completed;
            System.out.println("PUT " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Update failed, status=" + response.statusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to update subtask status", e);
        }
    }

    // =========================
    // ADD SUBTASK
    // =========================
    public SubTask addSubTask(Long taskId, SubTask subTask) {
        try {
            String url = BASE_URL + "/add/" + taskId;
            String json = objectMapper.writeValueAsString(subTask);

            System.out.println("POST " + url);
            System.out.println("BODY " + json);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new RuntimeException(
                        "Failed to add subtask, status=" + response.statusCode());
            }

            return objectMapper.readValue(response.body(), SubTask.class);

        } catch (Exception e) {
            throw new RuntimeException("Error adding subtask", e);
        }
    }
}
