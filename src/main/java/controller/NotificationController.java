package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dao.NotificationDao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.LocalDateTimeAdapter;
import model.Notification;
import service.NotificationService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet("/notifications/*")
public class NotificationController extends HttpServlet {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private final NotificationService notificationService = new NotificationService(new NotificationDao());

    // ======================= GET =======================
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo(); // may be null

        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "Bad request");
            return;
        }

        path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

        // /user/{userId}
        if (path.matches("/user/\\d+")) {
            Long userId = Long.parseLong(path.split("/")[2]);
            List<Notification> notifications = notificationService.getUserNotifications(userId);
            write(resp, notifications);
            return;
        }

        // /user/{userId}/unread-count
        if (path.matches("/user/\\d+/unread-count")) {
            Long userId = Long.parseLong(path.split("/")[2]);
            int count = notificationService.countUnread(userId);
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(String.valueOf(count));
            return;
        }

        // /{notifId}/user/{userId}
        if (path.matches("/\\d+/user/\\d+")) {
            String[] parts = path.split("/");
            Long notifId = Long.parseLong(parts[1]);
            Long userId = Long.parseLong(parts[3]);

            Notification notification = notificationService.getNotification(notifId, userId);
            if (notification == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                write(resp, "Notification not found");
                return;
            }
            write(resp, notification);
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        write(resp, "Endpoint not found");
    }
    
    // ======================= POST =======================
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getPathInfo(); // /user/{userId}

        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String[] parts = path.replaceFirst("^/", "").split("/");

        try {
            // POST /notifications/user/{userId}
            if (parts.length != 2 || !"user".equals(parts[0])) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            Long userId = Long.parseLong(parts[1]);

            Notification payload =
                    gson.fromJson(req.getReader(), Notification.class);

            if (payload == null ||
                payload.getMessage() == null ||
                payload.getMessage().isBlank()) {

                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            Notification created =
                    notificationService.create(userId, payload.getMessage());

            resp.setStatus(HttpServletResponse.SC_CREATED);
            write(resp, created);

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
    }

    // ======================= PUT =======================
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String path = req.getPathInfo(); // /{id}/user/{userId}/read

        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String[] parts = path.replaceFirst("^/", "").split("/");

        try {
            if (parts.length != 4 ||
                !"user".equals(parts[1]) ||
                !"read".equals(parts[3])) {

                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            Long notifId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[2]);

            boolean updated =
                    notificationService.markAsRead(notifId, userId);

            if (!updated) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
    }

    // ======================= UTIL =======================
    private void write(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(gson.toJson(data));
    }
}
