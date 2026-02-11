package ai;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.CategoryDTO;
import model.Task;

import java.net.URI;
import java.net.http.*;

public class AIClient {

    private static final String PARSE_URL = "http://localhost:9090/ai/parse";


    public Task parseTask(String text) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PARSE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(text)
                    ))
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient()
                            .send(request, HttpResponse.BodyHandlers.ofString());

            return mapper.readValue(response.body(), Task.class);

        } catch (Exception e) {
            throw new RuntimeException("AI parse failed", e);
        }
    }

}
