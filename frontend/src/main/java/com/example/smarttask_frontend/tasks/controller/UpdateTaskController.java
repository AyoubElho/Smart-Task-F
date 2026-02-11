package com.example.smarttask_frontend.tasks.controller;

import com.example.smarttask_frontend.entity.Priority;
import com.example.smarttask_frontend.entity.Task;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.tasks.service.TaskService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;


public class UpdateTaskController {


    @FXML
    private ComboBox<Priority> priorityBox;

    private final TaskService taskService = new TaskService();

    // ✅ Called automatically by JavaFX
    @FXML
    public void initialize() {
        priorityBox.getItems().setAll(Priority.values());
        priorityBox.setValue(Priority.LOW); // default
    }
}
