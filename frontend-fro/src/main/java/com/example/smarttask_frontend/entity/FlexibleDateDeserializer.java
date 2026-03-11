package com.example.smarttask_frontend.entity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FlexibleDateDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter[] formats = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    };

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        String text = p.getText();

        for (DateTimeFormatter f : formats) {
            try {
                return LocalDateTime.parse(text, f);
            } catch (Exception ignored) {}
        }

        throw new IOException("Invalid date: " + text);
    }
}
