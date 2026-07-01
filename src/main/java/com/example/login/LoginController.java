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
    private void handleTermsOfUse() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Terms of Use");
        alert.setHeaderText("BYOD System — Terms of Use");
        alert.getDialogPane().setMinWidth(550);
        alert.getDialogPane().setMinHeight(420);
        alert.getDialogPane().setStyle("-fx-font-family: 'Segoe UI', system-ui, sans-serif;");
        alert.setContentText(
            "BYOD System — Terms of Use\n\n" +
            "1. Acceptance of Terms\n" +
            "By accessing or using the Bring Your Own Device (BYOD) System, you agree to be bound by these Terms of Use. " +
            "If you do not agree, you must not use the system.\n\n" +
            "2. Purpose\n" +
            "The BYOD System is an institutional platform used to register, monitor, and manage personal electronic devices " +
            "(e.g., laptops, tablets, projectors, extension cords) brought onto the campus premises. It tracks device " +
            "ingress and egress to ensure campus security and compliance with institutional policies.\n\n" +
            "3. User Responsibilities\n" +
            "• Students must register their devices truthfully and completely before bringing them on campus.\n" +
            "• Users must keep their registration information up to date (e.g., changes in device, course, or contact details).\n" +
            "• Users must comply with all institutional policies regarding device usage on campus.\n" +
            "• Users are responsible for the security and proper use of their registered devices at all times.\n\n" +
            "4. Device Registration\n" +
            "• All personal devices intended for campus use must be registered through this system.\n" +
            "• Registration requires accurate device information including category, brand/model, and serial number (if applicable).\n" +
            "• Registrations are subject to approval by authorized faculty or administrative personnel.\n" +
            "• Approved registrations are valid for the current academic term unless revoked or cancelled.\n\n" +
            "5. Monitoring and Compliance\n" +
            "• The system logs ingress and egress activity of registered devices for security purposes.\n" +
            "• Unauthorized devices found on campus may be flagged or restricted.\n" +
            "• Users are expected to cooperate with monitoring personnel during device verification.\n\n" +
            "6. Account and Access\n" +
            "• Faculty accounts have full access to monitoring, reports, and approval functions.\n" +
            "• Student accounts are limited to registration and personal device management.\n" +
            "• Users must not share their credentials or allow unauthorized access to the system.\n\n" +
            "7. Service Availability\n" +
            "• The BYOD System is provided \"as is\" and may be temporarily unavailable for maintenance or updates.\n" +
            "• The institution reserves the right to modify, suspend, or discontinue the system at any time without prior notice.\n\n" +
            "8. Limitation of Liability\n" +
            "The institution shall not be held liable for any loss, damage, or disruption caused by the use or misuse of the BYOD System, " +
            "including but not limited to data loss, device theft, or unauthorized access."
        );
        alert.showAndWait();
    }

    @FXML
    private void handlePrivacyStatement() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Privacy Statement");
        alert.setHeaderText("BYOD System — Privacy Statement");
        alert.getDialogPane().setMinWidth(550);
        alert.getDialogPane().setMinHeight(420);
        alert.getDialogPane().setStyle("-fx-font-family: 'Segoe UI', system-ui, sans-serif;");
        alert.setContentText(
            "BYOD System — Privacy Statement\n\n" +
            "1. Information We Collect\n" +
            "The BYOD System collects the following personal and device information during registration and monitoring:\n" +
            "• Personal identity: full name, student/staff ID, year and section, course/program, contact number\n" +
            "• Device information: item category, brand/model, color/description, serial number\n" +
            "• Activity logs: ingress and egress timestamps, device status, approval status\n" +
            "• System data: form IDs, registration dates, remarks, and user category (student/staff/guest)\n\n" +
            "2. How We Use Your Information\n" +
            "Your collected information is used solely for the following purposes:\n" +
            "• Device registration, approval, and campus security management\n" +
            "• Tracking device ingress and egress to maintain a safe campus environment\n" +
            "• Generating institutional reports on device distribution and usage patterns\n" +
            "• Communicating registration status and required actions (e.g., approval, disapproval, cancellation)\n\n" +
            "3. Data Storage and Security\n" +
            "• All data is stored in a secure local database hosted on institutional servers.\n" +
            "• Access to the system is restricted to authorized users (faculty and registered students) only.\n" +
            "• Database credentials and connection details are protected and not exposed to end users.\n" +
            "• The system uses role-based access control to limit data visibility based on user type.\n\n" +
            "4. Data Retention\n" +
            "• Active registration records are retained for the duration of the current academic term.\n" +
            "• Deleted records are moved to a soft-deleted trash bin and may be permanently removed by authorized personnel.\n" +
            "• Export logs and activity reports are retained for institutional audit and compliance purposes.\n\n" +
            "5. Data Sharing\n" +
            "• Your personal data is not shared with third-party organizations or external services.\n" +
            "• Data may be accessed by authorized faculty and administrative staff for monitoring and reporting purposes.\n" +
            "• Aggregate, anonymized data may be used for institutional planning and policy development.\n\n" +
            "6. Your Rights\n" +
            "• You may view, update, or request correction of your personal and device information through the system.\n" +
            "• You may request deletion of your records (subject to institutional policy and approval).\n" +
            "• You may contact the system administrator for any concerns regarding your data.\n\n" +
            "7. Contact\n" +
            "For questions or concerns about this privacy statement or your data, please contact the BYOD System administrator."
        );
        alert.showAndWait();
    }

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

        closeWindow();
        openMonitoring();
    }

    private void openMonitoring() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/monitoring.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Monitoring - BYOD System");
            stage.setMinWidth(1024);
            stage.setMinHeight(700);
            stage.setMaximized(true);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to open monitoring", e);
        }
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
