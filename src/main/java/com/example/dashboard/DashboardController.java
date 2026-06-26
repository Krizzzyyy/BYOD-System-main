package com.example.dashboard;

import javafx.application.Platform;
import javafx.collections.FXCollections; // Added for explicit category array binding
import javafx.event.ActionEvent;
import com.example.Auth;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.example.service.BYODService;
import com.example.monitoring.QRScannerWindow;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class DashboardController {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String STYLESHEET_PATH = "/css/stylesheet.css";
    private static final String LOGIN_FXML = "/fxml/login.fxml";
    private static final String MONITORING_FXML = "/fxml/monitoring.fxml";
    private static final String REGISTRATION_FXML = "/fxml/registration.fxml";
    private static final String REPORTS_FXML = "/fxml/reports.fxml";
    private static final String ACCOUNT_FXML = "/fxml/account.fxml";

    private static final String SYNC_STATUS_LIVE = "Live";
    private static final String SYNC_STATUS_OFFLINE = "Offline";
    private static final String SYNC_STATUS_LOADING = "Loading";

    // Stats cards
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalDevicesLabel;
    @FXML private Label devicesInsideLabel;
    @FXML private Label ingressTodayLabel;
    @FXML private Label egressTodayLabel;

    // Header
    @FXML private Label dateLabel;
    @FXML private Label syncStatusLabel;
    @FXML private Label navDateTimeLabel;

    // Buttons
    @FXML private Button refreshButton;
    @FXML private Button dashboardButton;
    @FXML private Button monitoringButton;
    @FXML private Button registrationButton;
    @FXML private Button reportsButton;
    @FXML private Button seeAllButton;
    @FXML private Button scanQrButton;
    @FXML private Button searchButton;
    @FXML private Button ingressButton;
    @FXML private Button egressButton;

    // Chart
    @FXML private BarChart<String, Number> activityChart;

    // Log entries (5 rows)
    @FXML private Label logName0, logName1, logName2, logName3, logName4;
    @FXML private Label logId0, logId1, logId2, logId3, logId4;
    @FXML private Label logStatus0, logStatus1, logStatus2, logStatus3, logStatus4;
    @FXML private Label logTime0, logTime1, logTime2, logTime3, logTime4;

    private final Label[] logNames = new Label[5];
    private final Label[] logIds = new Label[5];
    private final Label[] logStatuses = new Label[5];
    private final Label[] logTimes = new Label[5];

    private final BYODService byodService = new BYODService();

    @FXML
    public void initialize() {
        initLogArrays();
        addStylesheetToScene();
        dateLabel.setText(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy").format(LocalDate.now()));
        startNavClock();

        if (dashboardButton != null) {
            dashboardButton.getStyleClass().add("active");
        }

        // CRITICAL FIX: Disable layout animations to stop text elements from clumping up at (0,0)
        if (activityChart != null) {
            activityChart.setAnimated(false);
            activityChart.getXAxis().setAnimated(false);
            activityChart.getYAxis().setAnimated(false);
        }

        loadLiveDatabaseData();
    }

    private static final DateTimeFormatter NAV_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/yyyy  hh:mm:ss a");

    private void startNavClock() {
        if (navDateTimeLabel == null) return;
        navDateTimeLabel.setText(NAV_DATETIME_FORMATTER.format(LocalDateTime.now()));
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                navDateTimeLabel.setText(NAV_DATETIME_FORMATTER.format(LocalDateTime.now()))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void initLogArrays() {
        logNames[0] = logName0; logNames[1] = logName1; logNames[2] = logName2;
        logNames[3] = logName3; logNames[4] = logName4;
        logIds[0] = logId0;     logIds[1] = logId1;     logIds[2] = logId2;
        logIds[3] = logId3;     logIds[4] = logId4;
        logStatuses[0] = logStatus0; logStatuses[1] = logStatus1; logStatuses[2] = logStatus2;
        logStatuses[3] = logStatus3; logStatuses[4] = logStatus4;
        logTimes[0] = logTime0; logTimes[1] = logTime1; logTimes[2] = logTime2;
        logTimes[3] = logTime3; logTimes[4] = logTime4;
    }

    private void addStylesheetToScene() {
        if (dateLabel != null && dateLabel.getScene() != null) {
            Scene scene = dateLabel.getScene();
            String css = getClass().getResource(STYLESHEET_PATH).toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }
        }
    }

    private void loadLiveDatabaseData() {
        updateSyncStatus(SYNC_STATUS_LOADING);

        Platform.runLater(() -> {
            try {
                // 1. Counters
                Map<String, Integer> metrics = byodService.fetchDashboardMetrics();
                if (!metrics.isEmpty()) {
                    totalStudentsLabel.setText(String.valueOf(metrics.getOrDefault("totalStudents", 0)));
                    totalDevicesLabel.setText(String.valueOf(metrics.getOrDefault("totalDevices", 0)));
                    devicesInsideLabel.setText(String.valueOf(metrics.getOrDefault("devicesInside", 0)));
                    ingressTodayLabel.setText(String.valueOf(metrics.getOrDefault("ingressToday", 0)));
                    egressTodayLabel.setText(String.valueOf(metrics.getOrDefault("egressToday", 0)));
                }

                // 2. Clear out log rows
                for (int i = 0; i < 5; i++) {
                    if (logNames[i] != null) {
                        logNames[i].setText(""); logIds[i].setText("");
                        logStatuses[i].setText(""); logTimes[i].setText("");
                    }
                }

                // 3. Populate logs table list data views
                List<Object[]> databaseLogs = byodService.fetchLogs();
                int displayLimit = Math.min(databaseLogs.size(), 5);

                for (int i = 0; i < displayLimit; i++) {
                    Object[] row = databaseLogs.get(i);
                    String studentId = (String) row[1];
                    String studentName = (String) row[2];
                    String egressTime = (String) row[5];

                    String statusText = (egressTime == null) ? "📥 In" : "📤 Out";
                    String timestampText = (egressTime == null) ? (String) row[4] : egressTime;

                    if (timestampText != null && timestampText.contains(" ")) {
                        String[] parts = timestampText.split(" ");
                        if (parts.length > 1) timestampText = parts[1].substring(0, 5);
                    }

                    if (logNames[i] != null) {
                        logNames[i].setText(studentName);
                        logIds[i].setText(studentId);
                        logStatuses[i].setText(statusText);
                        logTimes[i].setText(timestampText);
                        applyStatusStyle(logStatuses[i], logTimes[i], statusText);
                    }
                }

                // 4. Update Activity Chart Visuals
                Map<String, Map<String, Integer>> weeklyData = byodService.fetchWeeklyChartData();
                updateChart(weeklyData);

                updateSyncStatus(SYNC_STATUS_LIVE);

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load live tracking logs dashboard metrics", ex);
                updateSyncStatus(SYNC_STATUS_OFFLINE);
            }
        });
    }

    private void applyStatusStyle(Label statusLabel, Label timeLabel, String statusText) {
        if (statusLabel == null || timeLabel == null) return;
        statusLabel.getStyleClass().removeAll("log-status-in", "log-status-out");
        timeLabel.getStyleClass().removeAll("log-time-in", "log-time-out");
        if (statusText.contains("In")) {
            statusLabel.getStyleClass().add("log-status-in");
            timeLabel.getStyleClass().add("log-time-in");
        } else if (statusText.contains("Out")) {
            statusLabel.getStyleClass().add("log-status-out");
            timeLabel.getStyleClass().add("log-time-out");
        }
    }

    // FIXED: Formats CategoryAxis labels instantly without dynamic layout scaling conflicts
    private void updateChart(Map<String, Map<String, Integer>> chartData) {
        if (activityChart == null) return;
        activityChart.getData().clear();

        CategoryAxis xAxis = (CategoryAxis) activityChart.getXAxis();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        xAxis.setCategories(FXCollections.observableArrayList(days));

        XYChart.Series<String, Number> ingressSeries = new XYChart.Series<>();
        ingressSeries.setName("Ingress");

        XYChart.Series<String, Number> egressSeries = new XYChart.Series<>();
        egressSeries.setName("Egress");

        // Use the exact keys we put in the Map in the Service
        Map<String, Integer> ingressMap = chartData.get("Ingress");
        Map<String, Integer> egressMap = chartData.get("Egress");

        for (String day : days) {
            ingressSeries.getData().add(new XYChart.Data<>(day, ingressMap.getOrDefault(day, 0)));
            egressSeries.getData().add(new XYChart.Data<>(day, egressMap.getOrDefault(day, 0)));
        }

        activityChart.getData().addAll(ingressSeries, egressSeries);
    }
    private void updateSyncStatus(String status) {
        if (syncStatusLabel == null) return;
        syncStatusLabel.getStyleClass().removeAll("sync-status-live", "sync-status-offline", "sync-status-connecting");
        if (SYNC_STATUS_LIVE.equalsIgnoreCase(status)) {
            syncStatusLabel.setText("⏺ Live");
            syncStatusLabel.getStyleClass().add("sync-status-live");
        } else if (SYNC_STATUS_OFFLINE.equalsIgnoreCase(status)) {
            syncStatusLabel.setText("⏺ Offline");
            syncStatusLabel.getStyleClass().add("sync-status-offline");
        } else {
            syncStatusLabel.setText("⏳ Loading...");
            syncStatusLabel.getStyleClass().add("sync-status-connecting");
        }
    }

    @FXML private void handleRefresh() { loadLiveDatabaseData(); }

    @FXML
    private void handleLogout() {
        Platform.exit();
        System.exit(0);
    }

    private void loadLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());

            Stage loginStage = new Stage();
            loginStage.setScene(scene);
            loginStage.setTitle("BYOD Monitoring System - Login");
            loginStage.setResizable(false);
            loginStage.centerOnScreen();
            loginStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load login screen", e);
        }
    }

    @FXML private void handleDashboard() { handleRefresh(); }
    @FXML private void handleMonitoring() { navigateTo(MONITORING_FXML, "Monitoring - BYOD System"); }
    @FXML private void handleRegistration() { navigateTo(REGISTRATION_FXML, "Registration - BYOD System"); }
    @FXML
    private void handleReports() {
        if (Auth.reportUnlocked) {
            navigateTo(REPORTS_FXML, "Reports - BYOD System");
            return;
        }
        try {
            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
            Parent loginRoot = loginLoader.load();
            Stage loginStage = new Stage();
            Scene loginScene = new Scene(loginRoot);
            loginScene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());
            loginStage.setScene(loginScene);
            loginStage.setTitle("Login Required - Reports Access");
            loginStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            loginStage.showAndWait();

            if (Auth.reportUnlocked) {
                navigateTo(REPORTS_FXML, "Reports - BYOD System");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to open login popup", e);
        }
    }
    @FXML private void handleAccount() { navigateTo(ACCOUNT_FXML, "Account - BYOD System"); }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath.toLowerCase()));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());

            Stage stage = (Stage) dateLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to view " + fxmlPath, e);
        }
    }

    @FXML private void handleSeeAll() { navigateTo(MONITORING_FXML, "Monitoring - BYOD System"); }

    @FXML
    private void handleScanQr() {
        Stage currentStage = (Stage) dateLabel.getScene().getWindow();
        QRScannerWindow.openScanner(currentStage, qrPayload -> {
            try {
                String studentId = qrPayload;
                if (qrPayload.contains("|")) {
                    studentId = qrPayload.split("\\|")[0];
                }
                byodService.updateEgress(studentId);
                loadLiveDatabaseData();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed tool barcode scan operation processing", ex);
            }
        });
    }

    @FXML
    private void handleSearch() { navigateTo(MONITORING_FXML, "Monitoring - BYOD System"); }
    @FXML private void handleIngress() { navigateTo(MONITORING_FXML, "Monitoring - BYOD System"); }
    @FXML private void handleEgress() { navigateTo(MONITORING_FXML, "Monitoring - BYOD System"); }
    @FXML private void handleExport() { navigateTo(REPORTS_FXML, "Reports - BYOD System"); }
}