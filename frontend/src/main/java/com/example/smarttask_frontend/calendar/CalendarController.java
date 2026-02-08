package com.example.smarttask_frontend.calendar;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.tasks.service.TaskService;
import com.example.smarttask_frontend.session.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class CalendarController implements Initializable {

    @FXML
    private BorderPane root;

    private Calendar taskCalendar;
    private final TaskService taskService = new TaskService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // 1️⃣ Create calendar
        taskCalendar = new Calendar("My Tasks");
        taskCalendar.setStyle(Calendar.Style.STYLE1);
        taskCalendar.setReadOnly(false);

        // 2️⃣ Calendar source
        CalendarSource source = new CalendarSource("Tasks");
        source.getCalendars().add(taskCalendar);

        // 3️⃣ Calendar view
        CalendarView calendarView = new CalendarView();
        calendarView.getCalendarSources().add(source);
        calendarView.showMonthPage();

        // 4️⃣ Load tasks from DB
        try {
            loadTasksFromDatabase();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 5️⃣ Show in UI
        root.setCenter(calendarView);
    }

    // ================= LOAD TASKS =================

    private void loadTasksFromDatabase() throws Exception {

        Long userId = UserSession.getUser().getId();
        List<Task> tasks = taskService.getTasksByUser(userId);

        for (Task task : tasks) {

            if (task.getDueDate() == null) continue;

            addTaskToCalendar(task);
        }
    }

    private void addTaskToCalendar(Task task) {

        Entry<String> entry = new Entry<>(task.getTitle());

        LocalDateTime due = task.getDueDate();

        entry.changeStartDate(due.toLocalDate());
        entry.changeStartTime(due.toLocalTime());

        entry.changeEndDate(due.toLocalDate());
        entry.changeEndTime(due.toLocalTime().plusHours(1));

        // 🔗 Link Entry → Task ID
        entry.setUserObject(task.getId().toString());

        // 🔥 LISTEN FOR DRAG / RESIZE
        entry.intervalProperty().addListener((obs, oldInterval, newInterval) -> {

            LocalDateTime newDueDate =
                    LocalDateTime.of(
                            newInterval.getStartDate(),
                            newInterval.getStartTime()
                    );

            Long taskId = Long.valueOf(entry.getUserObject());

            System.out.println("Updating task " + taskId + " → " + newDueDate);

            // 🚀 Send update to backend
            taskService.updateDueDate(taskId, newDueDate);
        });

        taskCalendar.addEntry(entry);
    }

}
