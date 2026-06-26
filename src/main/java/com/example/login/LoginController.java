package com.example.login;

import com.example.Auth;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final String STYLESHEET_PATH = "/css/stylesheet.css";
    private static final String REGISTRATION_FXML = "/fxml/registration.fxml";

    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "password";

    @FXML private ImageView logoImage;
    @FXML private Label appTitleLabel;
    @FXML private Label subtitleLabel;
    @FXML private VBox roleSelectionPane;
    @FXML private Button studentButton;
    @FXML private Button facultyButton;
    @FXML private VBox loginFormPane;
    @FXML private Label appTitleLabel2;
    @FXML private Label usernameLabel;
    @FXML private TextField usernameField;
    @FXML private Label passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button cancelButton;

    @FXML
    public void initialize() {
        addStylesheetToScene();
        setupEventHandlers();
        setupInputValidation();
    }

    private void addStylesheetToScene() {
        Scene scene = loginFormPane.getScene();
        if (scene != null) {
            String css = getClass().getResource(STYLESHEET_PATH).toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }
        }
    }

    private void setupEventHandlers() {
        studentButton.setOnAction(event -> handleStudent());
        facultyButton.setOnAction(event -> handleFaculty());
        loginButton.setOnAction(event -> handleLogin());
        cancelButton.setOnAction(event -> handleCancel());
        passwordField.setOnAction(event -> handleLogin());
        usernameField.setOnAction(event -> handleLogin());
    }

    private void setupInputValidation() {
        usernameField.textProperty().addListener((obs, old, newVal) ->
                usernameField.getStyleClass().remove("error-field"));
        passwordField.textProperty().addListener((obs, old, newVal) ->
                passwordField.getStyleClass().remove("error-field"));
    }

    @FXML
    private void handleStudent() {
        Auth.isLoggedIn = true;
        Auth.userRole = "Student";
        closeWindow();
        openRegistration();
    }

    @FXML
    private void handleFaculty() {
        roleSelectionPane.setManaged(false);
        roleSelectionPane.setVisible(false);
        loginFormPane.setManaged(true);
        loginFormPane.setVisible(true);
        Platform.runLater(() -> usernameField.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Validation Error", "Invalid Input", "Please enter username and password.");
            usernameField.getStyleClass().add("error-field");
            passwordField.getStyleClass().add("error-field");
            return;
        }

        if (authenticate(username, password)) {
            loginSuccess();
        } else {
            showError("Authentication Failed", "Login Error",
                    "Invalid username or password.");
        }
    }

    private boolean authenticate(String username, String password) {
        return VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
    }

    private void loginSuccess() {
        clearErrorStyles();
        LOGGER.info("Login successful for faculty: " + usernameField.getText());

        Auth.isLoggedIn = true;
        Auth.userRole = "Faculty";
        Auth.reportUnlocked = true;

        closeWindow();
    }

    private void openRegistration() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(REGISTRATION_FXML));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());

            Stage regStage = new Stage();
            regStage.setScene(scene);
            regStage.setTitle("Device Registration - BYOD System");
            regStage.setMinWidth(1024);
            regStage.setMinHeight(700);
            regStage.setMaximized(true);
            regStage.centerOnScreen();
            regStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to open registration", e);
        }
    }

    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void clearErrorStyles() {
        usernameField.getStyleClass().remove("error-field");
        passwordField.getStyleClass().remove("error-field");
    }

    @FXML
    private void handleCancel() {
        usernameField.clear();
        passwordField.clear();
        loginFormPane.setManaged(false);
        loginFormPane.setVisible(false);
        roleSelectionPane.setManaged(true);
        roleSelectionPane.setVisible(true);
    }

    private void closeWindow() {
        if (loginFormPane.getScene() == null) return;
        Stage stage = (Stage) loginFormPane.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
