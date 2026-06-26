package com.example.login;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Point this directly to your main dashboard layout FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
        Parent root = loader.load();

        // 2. Set up the window scene
        Scene scene = new Scene(root);

        // 3. Load your global CSS styling sheet
        String cssPath = getClass().getResource("/css/stylesheet.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        // 4. Show the window
        stage.setTitle("Campus BYOD Tracker");
        stage.setScene(scene);
        stage.show();
    }
}