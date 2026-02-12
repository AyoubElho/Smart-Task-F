package com.example.smarttask_frontend.calendar;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.googleCalendarService.GoogleCalendarService;
import com.example.smarttask_frontend.tasks.service.TaskService;
import com.example.smarttask_frontend.session.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class CalendarController implements Initializable {

    @FXML
    private BorderPane root;
    @FXML
    private Button syncButton;

    private Calendar taskCalendar;
    private final TaskService taskService = new TaskService();
    private GoogleCalendarService googleService;

    private boolean ignoreChange = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        taskCalendar = new Calendar("My Tasks");
        taskCalendar.setStyle(Calendar.Style.STYLE1);
        taskCalendar.setReadOnly(false);

        CalendarSource source = new CalendarSource("Tasks");
        source.getCalendars().add(taskCalendar);

        CalendarView calendarView = new CalendarView();
        calendarView.getCalendarSources().add(source);
        calendarView.showWeekPage();

        root.setCenter(calendarView);

        googleService = new GoogleCalendarService(taskService);

        try {
            loadTasksFromDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= LOAD =================

    private void loadTasksFromDatabase() throws Exception {

        Long userId = UserSession.getUser().getId();
        List<Task> tasks = taskService.getTasksByUser(userId);

        for (Task task : tasks) {
            addTaskToCalendar(task);
        }
    }

    // ================= ADD ENTRY =================

    private void addTaskToCalendar(Task task) {

        Entry<String> entry = new Entry<>(task.getTitle());
        LocalDateTime start = task.getDueDate();
        LocalDateTime end = task.getEndDate();

        if (start == null) start = LocalDateTime.now();
        if (end == null || !end.isAfter(start)) {
            end = start.plusMinutes(30);
        }

// ✅ also repair the Task object itself
        task.setDueDate(start);
        task.setEndDate(end);


        entry.setInterval(start.toLocalDate(), start.toLocalTime(), end.toLocalDate(), end.toLocalTime());

        entry.setMinimumDuration(Duration.ofMinutes(5));
        entry.setUserObject(task.getId().toString());

        // 🔥 DRAG / RESIZE LISTENER
        entry.intervalProperty().addListener((obs, oldI, newI) -> {

            if (ignoreChange || newI == null) return;

            LocalDateTime newStart = LocalDateTime.of(newI.getStartDate(), newI.getStartTime());

            LocalDateTime newEnd = LocalDateTime.of(newI.getEndDate(), newI.getEndTime());

            Long taskId = Long.valueOf(entry.getUserObject());

            new Thread(() -> {
                try {

                    // update backend
                    taskService.updateInterval(taskId, newStart, newEnd);

                    // update local model
                    task.setDueDate(newStart);
                    task.setEndDate(newEnd);

                    // Google sync
                    googleService.createOrUpdateEvent(task);

                } catch (Exception e) {
                    e.printStackTrace();

                    // revert UI if failed
                    Platform.runLater(() -> {
                        ignoreChange = true;
                        entry.setInterval(oldI);
                        ignoreChange = false;
                    });
                }
            }).start();
        });

        taskCalendar.addEntry(entry);
    }

    // ================= GOOGLE SYNC =================

    @FXML
    private void handleGoogleSync() {

        syncButton.setText("Connecting...");
        syncButton.setDisable(true);

        new Thread(() -> {
            try {

                if (!googleService.isConnected()) {
                    googleService.connect();
                }

                Long userId = UserSession.getUser().getId();
                List<Task> tasks = taskService.getTasksByUser(userId);

                for (Task task : tasks) {
                    if (task.getDueDate() != null) {
                        googleService.createOrUpdateEvent(task);
                        Thread.sleep(300);
                    }
                }

                Platform.runLater(() -> {
                    syncButton.setText("Connected ✓");
                    syncButton.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;");
                });

            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    syncButton.setText("Sync failed");
                    syncButton.setDisable(false);
                });
            }
        }).start();
    }
}
