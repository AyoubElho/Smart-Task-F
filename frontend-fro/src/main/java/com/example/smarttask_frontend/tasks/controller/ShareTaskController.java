package com.example.smarttask_frontend.tasks.controller;

import com.example.smarttask_frontend.auth.service.UserService;
import com.example.smarttask_frontend.entity.User;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.tasks.service.NotificationService;
import com.example.smarttask_frontend.tasks.service.TaskService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class ShareTaskController {

    @FXML
    private Label taskTitleLabel;

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, User> usernameColumn;

    @FXML
    private TableColumn<User, Void> actionColumn;

    private Long taskId;
    private String taskTitle;

    // Services
    private final UserService userService = new UserService();
    private final TaskService taskService = new TaskService();

    private final ObservableList<User> users = FXCollections.observableArrayList();

    // --- NEW: Palette of nice, readable UI colors ---
    private static final String[] AVATAR_COLORS = {
            "#e57373", "#f06292", "#ba68c8", "#9575cd", "#7986cb",
            "#64b5f6", "#4dd0e1", "#4db6ac", "#81c784", "#aed581",
            "#ffb74d", "#ff8a65", "#a1887f", "#90a4ae"
    };

    public void setTaskId(Long taskId, String taskTitle) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        taskTitleLabel.setText(taskTitle);
        setupUserTable();
        loadUsers();
    }

    private void loadUsers() {
        User connectedUser = UserSession.getUser();
        users.clear();
        if (userService.getUsers() != null) {
            userService.getUsers().stream()
                    .filter(u -> !u.getId().equals(connectedUser.getId()))
                    .forEach(users::add);
        }
    }

    private void setupUserTable() {
        userTable.setItems(users);

        usernameColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));

        usernameColumn.setCellFactory(col -> new TableCell<User, User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);

                if (empty || user == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String initial = "";
                    String name = "";
                    if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                        initial = user.getUsername().substring(0, 1).toUpperCase();
                        name = user.getUsername();
                    }

                    Label avatarLabel = new Label(initial);
                    avatarLabel.getStyleClass().add("avatar-circle");

                    // --- NEW: Apply dynamic color based on username hash ---
                    String color = getColorForUser(name);
                    avatarLabel.setStyle("-fx-background-color: " + color + ";");

                    Label nameLabel = new Label(name);
                    nameLabel.getStyleClass().add("username-text");

                    HBox container = new HBox(12, avatarLabel, nameLabel);
                    container.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(container);
                    setText(null);
                }
            }
        });

        // Setup Action Column (Same as before)
        actionColumn.setCellFactory(col -> new TableCell<User, Void>() {
            private final Button shareButton = new Button("Share");
            {
                shareButton.getStyleClass().add("action-button");
                shareButton.setOnAction(event -> {
                    User selectedUser = getTableView().getItems().get(getIndex());
                    handleShare(selectedUser);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(shareButton);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
    }

    // --- NEW: Helper to pick a consistent color for a user ---
    private String getColorForUser(String username) {
        if (username == null || username.isEmpty()) return "#4285f4";
        int index = Math.abs(username.hashCode()) % AVATAR_COLORS.length;
        return AVATAR_COLORS[index];
    }

    private void handleShare(User user) {
        boolean success = taskService.shareTaskWithUser(taskId, user.getId(), taskTitle);
        if (success) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Success");
            alert.setContentText("Task shared with " + user.getUsername());
            alert.show();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Error");
            alert.setContentText("Failed to share task.");
            alert.show();
        }
    }

    @FXML
    private void closeWindow() {
        ((Stage) taskTitleLabel.getScene().getWindow()).close();
    }
}