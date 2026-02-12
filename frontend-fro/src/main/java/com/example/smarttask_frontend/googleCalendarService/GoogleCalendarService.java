package com.example.smarttask_frontend.googleCalendarService;

import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.tasks.service.TaskService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

public class GoogleCalendarService {

    private Calendar service;
    private final TaskService taskService;

    public GoogleCalendarService(TaskService taskService) {
        this.taskService = taskService;
    }

    public void connect() throws Exception {

        Credential credential = GoogleAuth.authorize();

        service = new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("SmartTask")
                .build();
    }

    public boolean isConnected() {
        return service != null;
    }

    public void createOrUpdateEvent(Task task) {

        if (!isConnected()) return;

        try {

            if (task.getDueDate() == null || task.getEndDate() == null) return;

            var zone = java.time.ZoneId.systemDefault();

            DateTime start = new DateTime(
                    task.getDueDate().atZone(zone).toInstant().toEpochMilli()
            );

            DateTime end = new DateTime(
                    task.getEndDate().atZone(zone).toInstant().toEpochMilli()
            );

            Event event = new Event()
                    .setSummary(task.getTitle())
                    .setStart(new EventDateTime().setDateTime(start))
                    .setEnd(new EventDateTime().setDateTime(end));

            try {

                if (task.getGoogleEventId() != null) {

                    service.events()
                            .update("primary", task.getGoogleEventId(), event)
                            .execute();

                    return;
                }

            } catch (GoogleJsonResponseException ex) {

                if (ex.getStatusCode() == 404) {
                    task.setGoogleEventId(null);
                }

                if (ex.getStatusCode() == 403) {
                    Thread.sleep(1000);
                }
            }

            Event created = service.events().insert("primary", event).execute();

            String id = created.getId();

            task.setGoogleEventId(id);
            taskService.updateGoogleEventId(task.getId(), id);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
