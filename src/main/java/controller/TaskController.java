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

        if (path.matches("/\\d+")) {
            Long id = Long.parseLong(path.substring(1));
            write(resp, taskService.findById(id));
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        write(resp, "Endpoint not found");
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

        // CREATE TASK
        if (path.startsWith("/create-task/id/")) {

            String userIdStr = path.substring("/create-task/id/".length());

            if (userIdStr.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                write(resp, "User ID is missing");
                return;
            }

            try {
                Long userId = Long.parseLong(userIdStr);
                Task task = readBody(req, Task.class);

                Task saved = taskService.createTask(task, userId);

                resp.setStatus(HttpServletResponse.SC_CREATED);
                write(resp, saved);

            } catch (IllegalArgumentException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                write(resp, e.getMessage());
            }

            return;
        }

        // SHARE TASK
        if (path.matches("/\\d+/share/\\d+")) {
            String[] parts = path.split("/");
            Long taskId = Long.parseLong(parts[1]);
            Long userId = Long.parseLong(parts[3]);

            taskService.shareTask(taskId, userId);
            write(resp, "Task shared successfully!");
            return;
        }

        // ADD DEPENDENCY
        if (path.matches("/\\d+/dependency/\\d+")) {

            String[] parts = path.split("/");
            Long taskId = Long.parseLong(parts[1]);
            Long dependencyId = Long.parseLong(parts[3]);

            try {
                taskService.addDependency(taskId, dependencyId);
                write(resp, "Dependency added successfully");

            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                write(resp, e.getMessage());
            }

            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        write(resp, "Endpoint not found");
    }

    // ========================= PUT =========================

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getRequestURI()
                .replace(req.getContextPath(), "")
                .replace("/tasks", "");

        System.out.println("REAL PATH = " + path);

        if (path == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // UPDATE STATUS
        if (path.matches("/\\d+/status/\\w+")) {
            String[] parts = path.split("/");
            Long taskId = Long.parseLong(parts[1]);
            Status status = Status.valueOf(parts[3]);

            taskService.updateStatus(taskId, status);
            write(resp, "Status updated");
            return;
        }

        // UPDATE START (due_date column)
        if (path.matches("/\\d+/due-date")) {
            String[] parts = path.split("/");
            Long taskId = Long.parseLong(parts[1]);

            UpdateDueDateRequest body =
                    readBody(req, UpdateDueDateRequest.class);

            taskService.updateStartDate(taskId, body.getDueDate());
            write(resp, "Start date updated");
            return;
        }

        // UPDATE END DATE
        if (path.matches("/\\d+/end-date")) {
            String[] parts = path.split("/");
            Long taskId = Long.parseLong(parts[1]);

            UpdateDueDateRequest body =
                    readBody(req, UpdateDueDateRequest.class);

            taskService.updateEndDate(taskId, body.getDueDate());
            write(resp, "End date updated");
            return;
        }

        // UPDATE INTERVAL (drag/resize)
        if (path.matches("/\\d+/interval")) {
            String[] parts = path.split("/");
            Long taskId = Long.parseLong(parts[1]);

            Task body = readBody(req, Task.class);

            taskService.updateInterval(
                    taskId,
                    body.getDueDate(), // start
                    body.getEndDate()  // end
            );

            write(resp, "Interval updated");
            return;
        }

        // GOOGLE EVENT LINK
        if (path.contains("/google-event/")) {

            int taskIdEnd = path.indexOf("/google-event/");
            String idPart = path.substring(1, taskIdEnd);
            String eventId =
                    path.substring(taskIdEnd + "/google-event/".length());

            Long taskId = Long.parseLong(idPart);

            taskService.updateGoogleEventId(taskId, eventId);
            write(resp, "Google event linked");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        write(resp, "Endpoint not found");
    }

    // ========================= DELETE =========================

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getPathInfo();

        if (path == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (path.matches("/\\d+/dependency/\\d+")) {

            String[] parts = path.split("/");
            Long taskId = Long.parseLong(parts[1]);
            Long dependencyId = Long.parseLong(parts[3]);

            taskService.removeDependency(taskId, dependencyId);
            write(resp, "Dependency removed successfully");
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
