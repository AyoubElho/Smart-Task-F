package com.example.smarttask_frontend;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SmartTaskApplication extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/views/LoginView.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Smart Task Manager");
        stage.setScene(scene);
        stage.show();
    }
}
