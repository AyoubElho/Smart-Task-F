package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import mail.VerifyRequest;
import model.User;
import model.LocalDateTimeAdapter;
import model.LoginRequest;
import service.UserService;


import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet("/user/*")
public class UserController extends HttpServlet {

    private final UserService userService = new UserService();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        write(resp, userService.findAllUsers());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String path = req.getPathInfo(); // may be null

        if (path == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(resp, "Invalid path");
            return;
        }

        if (path.equals("/register")) {
            User user = read(req, User.class);
            write(resp, userService.register(user));
            return;
        }
        if (path.equals("/verify")) {

            VerifyRequest request = read(req, VerifyRequest.class);

            userService.verifyEmail(
                    request.getEmail(),
                    request.getCode()
            );

            write(resp, "Email verified successfully");
            return;
        }
// Inside doPost method in UserController.java
        if (path.equals("/update")) {
            try {
                // Read user data (must include ID)
                User userToUpdate = read(req, User.class);

                // Call service
                User updatedUser = userService.updateUser(userToUpdate);

                write(resp, updatedUser);

            } catch (RuntimeException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                write(resp, e.getMessage());
            }
            return;
        }
        if (path.equals("/login")) {
            try {
                LoginRequest request = read(req, LoginRequest.class);

                write(resp, userService.login(
                        request.getEmail(),
                        request.getPassword()
                ));

            } catch (RuntimeException e) {

                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 🔥 401
                write(resp, e.getMessage());
            }

            return;
        }


        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        write(resp, "Endpoint not found");
    }


    private <T> T read(HttpServletRequest req, Class<T> clazz) throws IOException {
        BufferedReader reader = req.getReader();
        return gson.fromJson(reader, clazz);
    }

    private void write(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(data));
    }
}
