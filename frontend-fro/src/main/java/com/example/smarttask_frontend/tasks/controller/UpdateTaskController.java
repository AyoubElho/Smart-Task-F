package com.example.smarttask_frontend.tasks.controller;

import com.example.smarttask_frontend.entity.Priority;
import com.example.smarttask_frontend.entity.Recurrence;
import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.tasks.service.TaskService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class UpdateTaskController {

    @FXML
    private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dueDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<Priority> priorityBox;
    @FXML private ComboBox<Recurrence> recurrenceBox;
    @FXML private ListView<Task> dependencyListView;

    private final TaskService taskService = new TaskService();
    private Task taskToEdit; // 👈 important
    private boolean updated = false;

    @FXML
    public void initialize() {

        priorityBox.getItems().setAll(Priority.values());
        recurrenceBox.getItems().setAll(Recurrence.values());
        setupDependencyList(); // already loads tasks
        recurrenceBox.valueProperty().addListener((obs, oldV, newV) -> {
            boolean recurring = newV != Recurrence.NONE;
            endDatePicker.setDisable(recurring);
        });

        setupDependencyList();
    }

    public void setTask(Task task) {
        this.taskToEdit = task;

        titleField.setText(task.getTitle());
        descriptionField.setText(task.getDescription());

        if (task.getDueDate() != null)
            dueDatePicker.setValue(task.getDueDate().toLocalDate());

        if (task.getEndDate() != null)
            endDatePicker.setValue(task.getEndDate().toLocalDate());

        priorityBox.setValue(task.getPriority());
        recurrenceBox.setValue(task.getRecurrence());

        // 🔥 Wait for ListView to finish loading
        Platform.runLater(() -> selectDependencies(task));
    }
    private void selectDependencies(Task task) {

        List<Long> deps = task.getDependencyIds();
        if (deps == null) return;

        dependencyListView.getSelectionModel().clearSelection();

        for (int i = 0; i < dependencyListView.getItems().size(); i++) {
            Task t = dependencyListView.getItems().get(i);

            if (deps.contains(t.getId())) {
                dependencyListView.getSelectionModel().select(i);
            }
        }
    }
    public boolean isUpdated() {
        return updated;
    }

    @FXML
    private void save() {

        taskToEdit.setTitle(titleField.getText());
        taskToEdit.setDescription(descriptionField.getText());
        taskToEdit.setDueDate(dueDatePicker.getValue().atStartOfDay());

        if (recurrenceBox.getValue() == Recurrence.NONE) {
            taskToEdit.setEndDate(endDatePicker.getValue().atStartOfDay());
        } else {
            taskToEdit.setEndDate(null);
        }

        taskToEdit.setPriority(priorityBox.getValue());
        taskToEdit.setRecurrence(recurrenceBox.getValue());

        boolean success = taskService.updateTask(taskToEdit);

        if (success) {
            updated = true;
            close();
        } else {
            showError("Update failed");
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

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }

    private void setupDependencyList() {

        dependencyListView.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);

        loadExistingTasks(); // 🔥 load tasks from service
    }
    private void loadExistingTasks() {

        try {
            Long userId = UserSession.getUserId();
            List<Task> tasks = taskService.getTasksByUser(userId);

            dependencyListView.getItems().setAll(tasks);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void loadTaskForEdit(Task task) {

        this.taskToEdit = task;

        Platform.runLater(() -> {

            titleField.setText(task.getTitle());
            descriptionField.setText(task.getDescription());

            if (task.getDueDate() != null)
                dueDatePicker.setValue(task.getDueDate().toLocalDate());

            if (task.getEndDate() != null)
                endDatePicker.setValue(task.getEndDate().toLocalDate());

            priorityBox.setValue(task.getPriority());
            recurrenceBox.setValue(task.getRecurrence());

            selectDependencies(task);
        });
    }



}