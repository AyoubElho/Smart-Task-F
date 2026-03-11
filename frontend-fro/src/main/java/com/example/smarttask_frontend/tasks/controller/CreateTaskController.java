package com.example.smarttask_frontend.tasks.controller;

import com.example.smarttask_frontend.entity.Priority;
import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.tasks.service.TaskService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.List;import com.example.smarttask_frontend.entity.Recurrence;


public class CreateTaskController {

    @FXML
    private TextField titleField;
    @FXML
    private ComboBox<Recurrence> recurrenceBox;

    @FXML
    private TextArea descriptionField;

    @FXML
    private DatePicker dueDatePicker;

    @FXML
    private ComboBox<Priority> priorityBox;

    // ✅ NEW: ListView to select multiple dependent tasks
    @FXML
    private ListView<Task> dependencyListView;

    private final TaskService taskService = new TaskService();
    private boolean created = false;
    @FXML
    private DatePicker endDatePicker;

    @FXML
    public void initialize() {

        priorityBox.getItems().setAll(Priority.values());
        priorityBox.setValue(Priority.LOW);

        recurrenceBox.getItems().setAll(Recurrence.values());
        recurrenceBox.setValue(Recurrence.NONE);

        // 🔁 Listener
        recurrenceBox.valueProperty().addListener((obs, oldV, newV) -> {
            boolean recurring = newV != Recurrence.NONE;
            endDatePicker.setDisable(recurring);
        });

        setupDependencyList();
    }




    private void loadExistingTasks() {
        try {
            Long userId = UserSession.getUserId();
            List<Task> existingTasks = taskService.getTasksByUser(userId);
            dependencyListView.getItems().setAll(existingTasks);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load existing tasks for dependencies.");
        }
    }

    public boolean isCreated() {
        return created;
    }

    @FXML
    private void save() {

        if (titleField.getText().isBlank()) {
            showError("Title is required");
            return;
        }

        Recurrence recurrence = recurrenceBox.getValue();

        if (dueDatePicker.getValue() == null) {
            showError("Start date is required");
            return;
        }

        if (recurrence == Recurrence.NONE && endDatePicker.getValue() == null) {
            showError("End date is required for non-recurring tasks");
            return;
        }

        if (recurrence == Recurrence.NONE &&
                endDatePicker.getValue().isBefore(dueDatePicker.getValue())) {
            showError("End date cannot be before start date");
            return;
        }

        Task task = new Task();
        task.setTitle(titleField.getText());
        task.setDescription(descriptionField.getText());
        task.setDueDate(dueDatePicker.getValue().atStartOfDay());

        if (recurrence == Recurrence.NONE) {
            task.setEndDate(endDatePicker.getValue().atStartOfDay());
        } else {
            task.setEndDate(null);
        }

        task.setPriority(priorityBox.getValue());
        task.setRecurrence(recurrence);

        Task createdTask = taskService.createTask(task, UserSession.getUserId());

        if (createdTask != null) {
            saveDependencies(createdTask.getId());
            created = true;
            close();
        } else {
            showError("Task could not be created");
        }
    }



    private void saveDependencies(Long newTaskId) {
        // Get all selected tasks from the ListView
        List<Task> selectedDependencies = dependencyListView.getSelectionModel().getSelectedItems();
        System.out.println(selectedDependencies);
        if (selectedDependencies.isEmpty()) return;

        System.out.println("Saving " + selectedDependencies.size() + " dependencies...");

        for (Task dep : selectedDependencies) {
            // Call the service method we added earlier to link them
            boolean success = taskService.addDependency(newTaskId, dep.getId());
            if (!success) {
                System.err.println("Failed to link dependency: " + dep.getId());
            }
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        // Get stage from any control
        if (titleField.getScene() != null) {
            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.close();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void setupDependencyList() {
        // Enable multiple selection (Ctrl+Click or Command+Click)
        dependencyListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Custom Cell Factory to display Task Titles instead of object hash codes
        dependencyListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Task> call(ListView<Task> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Task item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            // Display format: "Title (Status)"
                            setText(item.getTitle() + " (" + item.getStatus() + ")");
                        }
                    }
                };
            }
        });

        // Load existing tasks from the server to populate the list
        loadExistingTasks();
    }

}