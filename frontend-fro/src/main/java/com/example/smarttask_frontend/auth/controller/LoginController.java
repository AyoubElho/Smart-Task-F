package com.example.smarttask_frontend.auth.controller;

import com.example.smarttask_frontend.auth.service.UserService;
import com.example.smarttask_frontend.session.UserSession;
import com.example.smarttask_frontend.entity.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField email;   // ✅ email, not username

    @FXML
    private PasswordField password;

    @FXML
    private Button loginButton;

    private final UserService userService = new UserService();


    @FXML private TextField regName;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;

    @FXML private VBox loginForm;
    @FXML private VBox registerForm;
    @FXML
    public void initialize() {

        loginForm.setVisible(true);
        loginForm.setManaged(true);

        registerForm.setVisible(false);
        registerForm.setManaged(false);
    }

    @FXML
    private void switchToRegister() {

        loginForm.setVisible(false);
        loginForm.setManaged(false);

        registerForm.setVisible(true);
        registerForm.setManaged(true);
    }

    @FXML
    private void switchToLogin() {

        registerForm.setVisible(false);
        registerForm.setManaged(false);

        loginForm.setVisible(true);
        loginForm.setManaged(true);
    }

    @FXML
    private void register() {

        String name = regName.getText();
        String mail = regEmail.getText();
        String pass = regPassword.getText();

        if (name.isEmpty() || mail.isEmpty() || pass.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        new Thread(() -> {

            boolean success = userService.register(name, mail, pass);

            Platform.runLater(() -> {

                if (success) {

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Verification required");
                    alert.setHeaderText(null);
                    alert.setContentText(
                            "Account created!\nCheck your email for the verification code."
                    );
                    alert.showAndWait();

                    // 🔥 IMPORTANT: ask for verification now
                    askVerification(mail);

                } else {
                    showError("Registration failed. Email may already exist.");
                }

            });

        }).start();
    }

    private void askVerification(String email) {

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Email Verification");
        dialog.setHeaderText("Enter the verification code sent to your email");
        dialog.setContentText("Code:");

        dialog.showAndWait().ifPresent(code -> {

            new Thread(() -> {

                boolean verified =
                        userService.verifyEmail(email, code);

                Platform.runLater(() -> {

                    if (verified) {

                        Alert ok = new Alert(Alert.AlertType.INFORMATION);
                        ok.setHeaderText(null);
                        ok.setContentText("Email verified! You can login now.");
                        ok.showAndWait();

                        switchToLogin();

                    } else {
                        showError("Invalid verification code.");
                    }

                });

            }).start();
        });
    }


    @FXML
    private void login() {

        new Thread(() -> {

            try {

                User user = userService.login(
                        email.getText(),
                        password.getText()
                );

                Platform.runLater(() -> openDashboard(user));

            } catch (RuntimeException e) {

                Platform.runLater(() ->
                        showError(e.getMessage())
                );
            }

        }).start();
    }

    private void openDashboard(User user) {
        try {
            UserSession.setUser(user);

            FXMLLoader loader =
                    new FXMLLoader(getClass()
                            .getResource("/views/DashboardView.fxml"));

            Scene scene = new Scene(loader.load(), 1200, 700);

            Stage stage =
                    (Stage) loginButton.getScene().getWindow();

            stage.setTitle("Smart Task Manager");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

}
