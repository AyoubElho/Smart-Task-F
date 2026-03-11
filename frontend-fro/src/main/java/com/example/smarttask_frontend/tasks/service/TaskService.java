package com.example.smarttask_frontend.tasks.service;

import com.example.smarttask_frontend.AppConfig;
import com.example.smarttask_frontend.category.service.CategoryService;
import com.example.smarttask_frontend.dto.UpdateDueDateRequest;
import com.example.smarttask_frontend.entity.CategoryDTO;
import com.example.smarttask_frontend.entity.Task;
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

    // ================= GOOGLE =================

    public void updateGoogleEventId(Long taskId, String eventId) {

        try {
            String url = BASE_URL + taskId + "/google-event/" + eventId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= LOAD TASKS =================
    public List<Task> getSharedTasks(Long userId) {
        try {
            String url = BASE_URL + "shared/" + userId;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<Task>>() {
                });
            }
            throw new RuntimeException("Failed to load shared tasks");
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
    public boolean deleteTask(Long taskId) {

        try {
            String url = BASE_URL + taskId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean updateTask(Task task) {

        try {
            String url = BASE_URL + task.getId();

            String json = objectMapper.writeValueAsString(task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public List<Task> getTasksByUser(Long userId) throws Exception {

        String url = BASE_URL + "user/" + userId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to load tasks");
        }

        List<Task> tasks = objectMapper.readValue(
                response.body(),
                new TypeReference<List<Task>>() {
                }
        );

        // resolve category names
        for (Task task : tasks) {

            if (task.getCategoryId() != null) {
                CategoryDTO category =
                        categoryService.getCategoryById(task.getCategoryId());

                task.setCategoryName(
                        category != null ? category.getName() : "General"
                );
            } else {
                task.setCategoryName("General");
            }
        }

        return tasks;
    }

    // ================= CREATE =================

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

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return objectMapper.readValue(response.body(), Task.class);
            }

            throw new RuntimeException("Create task failed: " + response.body());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Create task failed");
        }
    }

    // ================= UPDATE START =================

    public void updateDueDate(Long taskId, LocalDateTime newStart) {

        try {
            String url = BASE_URL + taskId + "/due-date";

            UpdateDueDateRequest body =
                    new UpdateDueDateRequest(newStart);

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

    // ================= UPDATE INTERVAL =================

    public void updateInterval(Long taskId,
                               LocalDateTime start,
                               LocalDateTime end) {

        try {
            String url = BASE_URL + taskId + "/interval";

            UpdateIntervalRequest body =
                    new UpdateIntervalRequest(start, end);


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


    // ================= STATUS =================

    public void updateTaskStatus(Long taskId, String status) {

        try {
            String url = BASE_URL + taskId + "/status/" + status.toUpperCase();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= SHARE =================

    public boolean shareTaskWithUser(Long taskId, Long userId, String taskTitle) {

        try {
            String url = BASE_URL + taskId + "/share/" + userId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 200 || response.statusCode() == 204) {

                notificationService.createNotification(
                        userId,
                        "Task '" + taskTitle + "' shared with you"
                );

                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // ================= DEPENDENCIES =================

    public boolean addDependency(Long taskId, Long dependencyId) {

        try {
            String url = BASE_URL + taskId + "/dependency/" + dependencyId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeDependency(Long taskId, Long dependencyId) {

        try {
            String url = BASE_URL + taskId + "/dependency/" + dependencyId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updateTaskRecurrence(Long id, String recurrence) throws Exception {
        String url = BASE_URL + "/tasks/" + id + "/recurrence/" + recurrence;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
