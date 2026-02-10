package com.example.smarttask_frontend.tasks.controller;

import com.example.smarttask_frontend.entity.Priority;
import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.tasks.service.TaskService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class CreateTaskController {

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private DatePicker dueDatePicker;

    @FXML
    private ComboBox<Priority> priorityBox;

    private final TaskService taskService = new TaskService();

    // ✅ Called automatically by JavaFX
    @FXML
    public void initialize() {
        priorityBox.getItems().setAll(Priority.values());
        priorityBox.setValue(Priority.LOW); // default
    }

    @FXML
    private void save() {
        if (titleField.getText().isBlank()) {
            showError("Title is required");
            return;
        }

        if (dueDatePicker.getValue() == null) {
            showError("Due date is required");
            return;
        }

        Task task = new Task();
        task.setTitle(titleField.getText());
        task.setDescription(descriptionField.getText());
        task.setDueDate(dueDatePicker.getValue().atStartOfDay());
        task.setPriority(priorityBox.getValue()); // ✅ enum safe

        Task created = taskService.createTask(task, UserSession.getUserId());

        if (created != null) {
            close();
        } else {
            showError("Task could not be created");
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
