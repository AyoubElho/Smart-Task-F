package com.example.smarttask_frontend.subtasks.controller;

import com.example.smarttask_frontend.entity.SubTask;
import com.example.smarttask_frontend.subtasks.service.SubTaskService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class SubtaskController {

    @FXML private TableView<SubTask> subtaskTable;
    @FXML private TableColumn<SubTask, SubTask> statusCol;
    @FXML private TableColumn<SubTask, String> titleCol;
    @FXML private TableColumn<SubTask, SubTask> actionCol;

    @FXML private TextField subtaskField;
    @FXML private Button addButton;
    @FXML private Button closeButton;

    private final SubTaskService subTaskService = new SubTaskService();
    private Long currentTaskId;

    @FXML
    public void initialize() {
        setupColumns();
        setupActions();
    }

    public void setTaskId(Long taskId) {
        this.currentTaskId = taskId;
        refreshList();
    }

    private void refreshList() {
        if(currentTaskId == null) return;
        List<SubTask> subtasks = subTaskService.getSubTasksByTaskId(currentTaskId);
        System.out.println(subtasks);
        subtaskTable.getItems().setAll(subtasks);
    }

    private void setupColumns() {
        // 1. Title Column (Simple Text)
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));

        // Custom Cell for Title to handle Strikethrough
        titleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Get the generic SubTask object associated with this row
                    SubTask currentRow = getTableView().getItems().get(getIndex());
                    if (currentRow.isCompleted()) {
                        setStyle("-fx-text-fill: #adb5bd; -fx-strikethrough: true;");
                    } else {
                        setStyle("-fx-text-fill: #495057; -fx-strikethrough: false;");
                    }
                }
            }
        });

        // 2. Status Column (Checkbox)
        statusCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    SubTask task = getItem();
                    if (task != null) {
                        boolean newStatus = checkBox.isSelected();
                        task.setCompleted(newStatus);

                        // Backend Call
                        try {
                            subTaskService.updateSubTaskStatus(task.getId(), newStatus);
                            // Refresh table to update Strikethrough in Title column
                            subtaskTable.refresh();
                        } catch (Exception ex) {
                            checkBox.setSelected(!newStatus); // Revert on fail
                        }
                    }
                });
            }

            @Override
            protected void updateItem(SubTask item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item.isCompleted());
                    setGraphic(checkBox);
                    setStyle("-fx-alignment: CENTER;"); // Center the checkbox
                }
            }
        });

        // 3. Action Column (Delete Button)
        actionCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.getStyleClass().add("danger-btn");
                deleteBtn.setOnAction(e -> {
                    SubTask task = getItem();
                    if (task != null) {
                        // Backend Call (Assuming you implement delete)
                        // subTaskService.deleteSubTask(task.getId());

                        // UI Update
                        getTableView().getItems().remove(task);
                    }
                });
            }

            @Override
            protected void updateItem(SubTask item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
    }

    private void setupActions() {
        closeButton.setOnAction(e -> ((Stage) closeButton.getScene().getWindow()).close());

        addButton.setOnAction(e -> {
            String title = subtaskField.getText().trim();

            if (title.isEmpty() || currentTaskId == null) {
                showAlert("Please enter a subtask title");
                return;
            }

            try {
                SubTask newSubTask = new SubTask(null, title, false);

                // 🔥 BACKEND CALL
                SubTask saved =
                        subTaskService.addSubTask(currentTaskId, newSubTask);

                // 🔥 UI UPDATE
                subtaskTable.getItems().add(saved);
                subtaskField.clear();

            } catch (Exception ex) {
                showAlert("Failed to add subtask");
            }
        });

    }
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

}