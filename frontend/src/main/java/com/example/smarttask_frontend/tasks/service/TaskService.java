package com.example.smarttask_frontend.tasks.service;

import com.example.smarttask_frontend.AppConfig;
import com.example.smarttask_frontend.category.service.CategoryService;
import com.example.smarttask_frontend.dto.UpdateDueDateRequest;
import com.example.smarttask_frontend.tasks.service.NotificationService;

import com.example.smarttask_frontend.entity.CategoryDTO;
import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

public class TaskService {
    private final CategoryService categoryService = new CategoryService();
    private final NotificationService notificationService = new NotificationService();


    private static final String BASE_URL =
            AppConfig.get("backend.base-url").endsWith("/")
                    ? AppConfig.get("backend.base-url") + "tasks/"
                    : AppConfig.get("backend.base-url") + "/tasks/";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ================= TASKS =================

    public List<Task> getTasksByUser(Long userId) throws Exception {

        String url = BASE_URL + "user/" + userId;
        System.out.println("Calling: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {

            List<Task> tasks = objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<Task>>() {}
            );

            // 🔥 RESOLVE CATEGORY NAME
            for (Task task : tasks) {

                if (task.getCategoryId() != null) {
                    CategoryDTO category =
                            categoryService.getCategoryById(task.getCategoryId());

                    if (category != null) {
                        task.setCategoryName(category.getName());
                    } else {
                        task.setCategoryName("General");
                    }
                } else {
                    task.setCategoryName("General");
                }
            }

            return tasks;
        }

        throw new RuntimeException(
                "Failed to load tasks, status: " + response.statusCode());
    }


    public Task createTask(Task task, Long userId) {
        try {
            String url = BASE_URL + "create-task/id/" + userId;

            String json = objectMapper.writeValueAsString(task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("STATUS: " + response.statusCode());
            System.out.println("BODY: " + response.body());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return objectMapper.readValue(response.body(), Task.class);
            }

            throw new RuntimeException("Create task failed: " + response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updateDueDate(Long taskId, LocalDateTime newDueDate) {
        try {
            String url = BASE_URL + taskId + "/due-date";

            UpdateDueDateRequest body =
                    new UpdateDueDateRequest(newDueDate);

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTaskStatus(Long taskId, String status) {
        try {
            status = status.toUpperCase(); // ✅ REQUIRED

            String url = BASE_URL + taskId + "/status/" + status;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean shareTaskWithUser(Long taskId, Long userId, String taskTitle) {
        try {
            String url = BASE_URL + taskId + "/share/" + userId;
            System.out.println("[TaskService] Sharing task: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                // Create notification for the user being shared with
                String message = String.format("Task '%s' has been shared with you", taskTitle);
                boolean notificationCreated = notificationService.createNotification(userId, message);
                
                if (notificationCreated) {
                    System.out.println("[TaskService] Notification created for user " + userId);
                } else {
                    System.out.println("[TaskService] Failed to create notification for user " + userId);
                }
                
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("[TaskService] Error sharing task: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Task> getSharedTasks(Long userId) {
        try {
            String url = BASE_URL + "shared/" + userId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(
                        response.body(),
                        new TypeReference<List<Task>>() {}
                );
            }

            throw new RuntimeException("Failed to load shared tasks");

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public boolean addDependency(Long taskId, Long dependencyId) {
        try {
            String url = BASE_URL + taskId + "/dependency/" + dependencyId;
            System.out.println("Adding Dependency: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeDependency(Long taskId, Long dependencyId) {
        try {
            String url = BASE_URL + taskId + "/dependency/" + dependencyId;
            System.out.println("Removing Dependency: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
