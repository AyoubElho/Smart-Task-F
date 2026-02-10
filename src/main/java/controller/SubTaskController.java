package controller;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.SubTask;
import service.SubTaskService;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

@WebServlet("/subtasks/*")
public class SubTaskController extends HttpServlet {

    private final SubTaskService subTaskService = new SubTaskService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String path = req.getPathInfo(); // /task/1

        if (path.startsWith("/task/")) {
            Long taskId = Long.parseLong(path.substring(6));
            write(resp, subTaskService.getSubTasksByTaskId(taskId));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String path = req.getPathInfo(); // /add/1

        if (path.startsWith("/add/")) {
            Long taskId = Long.parseLong(path.substring(5));
            SubTask subTask = read(req);
            write(resp, subTaskService.addSubTask(taskId, subTask));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String path = req.getPathInfo(); // /{id}/status

        if (path.endsWith("/status")) {
            String[] parts = path.split("/");
            Long id = Long.parseLong(parts[1]);
            boolean completed =
                    Boolean.parseBoolean(req.getParameter("is_completed"));
            write(resp, subTaskService.markSubTaskAsDone(id, completed));
        }
    }

    private SubTask read(HttpServletRequest req) throws IOException {
        BufferedReader reader = req.getReader();
        return gson.fromJson(reader, SubTask.class);
    }

    private void write(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(data));
    }
}
