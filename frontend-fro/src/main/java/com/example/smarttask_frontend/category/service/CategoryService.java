package com.example.smarttask_frontend.category.service;

import com.example.smarttask_frontend.AppConfig;
import com.example.smarttask_frontend.entity.CategoryDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CategoryService {

    private static final String BASE_URL =
            AppConfig.get("backend.base-url").endsWith("/")
                    ? AppConfig.get("backend.base-url") + "categories/"
                    : AppConfig.get("backend.base-url") + "/categories/";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryDTO getCategoryById(Long id) {
        try {
            String url = BASE_URL + id;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), CategoryDTO.class);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
