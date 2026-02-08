package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.LocalDateTimeAdapter;
import model.Status;
import model.Task;
import model.UpdateDueDateRequest;
import service.TaskService;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet("/tasks/*")
public class TaskController extends HttpServlet {

    private final TaskService taskService = new TaskService();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .serializeNulls()
            .create();

    // ========================= GET =========================
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            write(resp, taskService.getTasks());
            return;
        }

        if (path.equals("/test")) {
            write(resp, "OK");
            return;
        }

        if (path.startsWith("/user/")) {
            Long userId = Long.parseLong(path.substring(6));
            write(resp, taskService.getTasksByUser(userId));
            return;
        }

        if (path.startsWith("/shared/")) {
            Long userId = Long.parseLong(path.substring(8));
            write(resp, taskService.getTasksSharedWithUser(userId));
            return;
        }

        // /{id}
        Long id = Long.parseLong(path.substring(1));
        write(resp, taskService.findById(id));
    }

    // ========================= POST =========================
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getPathInfo();

        if (path == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "Invalid path");
            return;
        }

        // /create-task/id/{userId}
        // /create-task/id/{userId}
        if (path.startsWith("/create-task/id/")) {

            String userIdStr = path.substring("/create-task/id/".length());

            if (userIdStr.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                write(resp, "User ID is missing");
                return;
            }

            Long userId = Long.parseLong(userIdStr);

            Task task = readBody(req, Task.class);

            // 🔥 IMPORTANT: use returned task
            Task saved = taskService.createTask(task, userId);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            write(resp, saved);
            return;
        }


        // /{taskId}/share/{userId}
        if (path.matches("/\\d+/share/\\d+")) {
            String[] parts = path.split("/");
            Long taskId = Long.parseLong(parts[1]);
            Long userId = Long.parseLong(parts[3]);

            taskService.shareTask(taskId, userId);
            write(resp, "Task shared successfully!");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        write(resp, "Endpoint not found");
    }

    // ========================= PUT =========================
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getPathInfo();

        if (path == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // /{taskId}/status/{status}
        if (path.matches("/\\d+/status/\\w+")) {
            String[] parts = path.split("/");
            Long taskId = Long.parseLong(parts[1]);
            Status status = Status.valueOf(parts[3]);

            taskService.updateStatus(taskId, status);
            write(resp, "Status updated");
            return;
        }

        // /{taskId}/due-date
        if (path.matches("/\\d+/due-date")) {
            String[] parts = path.split("/");
            Long taskId = Long.parseLong(parts[1]);

            UpdateDueDateRequest body =
                    readBody(req, UpdateDueDateRequest.class);

            taskService.updateDueDate(taskId, body.getDueDate());
            write(resp, "Due date updated");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        write(resp, "Endpoint not found");
    }

    // ========================= HELPERS =========================
    private <T> T readBody(HttpServletRequest req, Class<T> clazz) throws IOException {
        return gson.fromJson(req.getReader(), clazz);
    }

    private void write(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(data));
    }
}

