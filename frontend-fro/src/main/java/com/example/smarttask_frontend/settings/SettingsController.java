package com.example.smarttask_frontend.settings;

import com.example.smarttask_frontend.auth.service.UserService; // Import Service
import com.example.smarttask_frontend.entity.User;
import com.example.smarttask_frontend.session.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;

public class SettingsController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label statusLabel;
    @FXML private Label displayNameLabel;
    @FXML private Label avatarLabel;
    @FXML private Circle avatarCircle;

    // Instantiate UserService
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        User currentUser = UserSession.getUser();

        if (currentUser != null) {
            nameField.setText(currentUser.getUsername());
            emailField.setText(currentUser.getEmail());
            updateProfileDisplay(currentUser.getUsername());
        } else {
            updateProfileDisplay("Guest");
        }
    }

    @FXML
    public void saveSettings(ActionEvent event) {
        resetFieldStyles();
        statusLabel.setText("Saving...");
        statusLabel.setStyle("-fx-text-fill: #f39c12;"); // Orange for loading

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // --- Validation ---
        if (name.isEmpty() || email.isEmpty()) {
            showError("Name and Email are required.");
            if(name.isEmpty()) nameField.getStyleClass().add("error");
            if(email.isEmpty()) emailField.getStyleClass().add("error");
            return;
        }

        if (!password.isEmpty() && !password.equals(confirmPassword)) {
            showError("Passwords do not match!");
            passwordField.getStyleClass().add("error");
            confirmPasswordField.getStyleClass().add("error");
            return;
        }

        // --- Prepare Data for Update ---
        User currentUser = UserSession.getUser();
        if (currentUser == null) {
            showError("No user session found. Please login again.");
            return;
        }

        // Create a User object with only the fields we want to update
        User updateRequest = new User();
        updateRequest.setId(currentUser.getId()); // ID is required to find the user in DB
        updateRequest.setUsername(name);
        updateRequest.setEmail(email);

        // Only set password if the user typed one
        if (!password.isEmpty()) {
            updateRequest.setPassword(password);
        }

        // --- Call Backend (Run in background thread to avoid freezing UI) ---
        new Thread(() -> {
            try {
                // 1. Call API
                User updatedUser = userService.updateUser(updateRequest);

                // 2. Update Session
                UserSession.setUser(updatedUser);

                // 3. Update UI on JavaFX Thread
                Platform.runLater(() -> {
                    updateProfileDisplay(updatedUser.getUsername());

                    statusLabel.setText("Profile updated successfully!");
                    statusLabel.setStyle("-fx-text-fill: #27ae60;"); // Green

                    passwordField.clear();
                    confirmPasswordField.clear();
                });

            } catch (RuntimeException e) {
                // Handle API Errors
                Platform.runLater(() -> showError("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void updateProfileDisplay(String fullName) {
        displayNameLabel.setText(fullName);
        if (fullName != null && !fullName.isEmpty()) {
            avatarLabel.setText(fullName.substring(0, 1).toUpperCase());
        } else {
            avatarLabel.setText("?");
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
    }

    private void resetFieldStyles() {
        nameField.getStyleClass().remove("error");
        emailField.getStyleClass().remove("error");
        passwordField.getStyleClass().remove("error");
        confirmPasswordField.getStyleClass().remove("error");
    }
}