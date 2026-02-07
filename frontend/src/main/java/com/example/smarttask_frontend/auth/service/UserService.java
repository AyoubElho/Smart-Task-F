package com.example.smarttask_frontend.auth.service;

import com.example.smarttask_frontend.AppConfig;
import com.example.smarttask_frontend.dto.LoginRequest;
import com.example.smarttask_frontend.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class UserService {

    private static final String LOGIN_URL = AppConfig.get("backend.base-url") + "user/login";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public User login(String email, String password) {

        try {
            LoginRequest loginRequest = new LoginRequest(email, password);

            String json = objectMapper.writeValueAsString(loginRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LOGIN_URL))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("STATUS: " + response.statusCode());
            System.out.println("BODY: " + response.body());

            if (response.statusCode() == 200) {
                System.out.println(objectMapper.readValue(response.body(), User.class).getEmail());
                return objectMapper.readValue(response.body(), User.class);
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<User> getUsers() {
        try {
            String url = AppConfig.get("backend.base-url") + "user";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(
                        response.body(),
                        new TypeReference<List<User>>() {}
                );
            }

            throw new RuntimeException("Failed to load users");

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }



}
