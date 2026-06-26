    package com.example.monitoring;
    
    import javafx.application.Platform;
    import javafx.collections.FXCollections;
    import javafx.collections.ObservableList;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.fxml.Initializable;
    import javafx.scene.Parent;
    import javafx.scene.Scene;
    import javafx.scene.control.*;
    import javafx.scene.control.cell.PropertyValueFactory;
    import javafx.scene.image.ImageView;
    import javafx.stage.Stage;
    import com.example.service.BYODService;
    import com.example.Auth;
    import java.io.IOException;
    import java.net.URL;
    import java.time.LocalDate; // Added for live calendar tracking
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter; // Added for calendar rendering format
    import java.util.ResourceBundle;
    import java.util.Map;
    import java.util.logging.Level;
    import java.util.logging.Logger;
    import javafx.animation.Animation;
    import javafx.animation.KeyFrame;
    import javafx.animation.Timeline;
    import javafx.util.Duration;
    
    public class MonitoringController implements Initializable {
    
        private static final Logger LOGGER = Logger.getLogger(MonitoringController.class.getName());
        private static final String STYLESHEET_PATH = "/css/stylesheet.css";
        private static final String DASHBOARD_FXML = "/fxml/dashboard.fxml";
        private static final String REGISTRATION_FXML = "/fxml/registration.fxml";
        private static final String REPORTS_FXML = "/fxml/reports.fxml";
        private static final String ACCOUNT_FXML = "/fxml/account.fxml";
        private static final String LOGIN_FXML = "/fxml/login.fxml";
    
        private static final String STATUS_INGRESS = "Ingress";
        private static final String STATUS_EGRESS = "Egress";
        private static final int ROWS_PER_PAGE = 10;
    
        // ==================== NAVIGATION BAR ====================
        @FXML private ImageView logoImage;
        @FXML private Button dashboardButton;
        @FXML private Button monitoringButton;
        @FXML private Button registrationButton;
        @FXML private Button reportsButton;
        @FXML private Label navDateTimeLabel;
    
        // ==================== STATUS & FILTERS ====================
        @FXML private Label syncNoDevices;
        @FXML private TextField searchField;
        @FXML private Button allDeviceTypesButton;
        @FXML private Button allStatusButton;
        @FXML private Button datePickerButton; // The targeted node for modification
        @FXML private Button exportLogButton;
        @FXML private Button logEntryButton;
    
        // ==================== CARD LABELS ====================
        @FXML private Label totalStudentsLabel;
        @FXML private Label totalDevicesLabel;
        @FXML private Label devicesInsideLabel;
        @FXML private Label ingressTodayLabel;
        @FXML private Label egressTodayLabel;
    
        // ==================== TABLE & COLUMNS ====================
        @FXML private TableView<LogEntry> monitoringTableView;
        @FXML private TableColumn<LogEntry, String> studentNameColumn;
        @FXML private TableColumn<LogEntry, String> studentIdColumn;
        @FXML private TableColumn<LogEntry, String> deviceSerialColumn;
        @FXML private TableColumn<LogEntry, String> statusColumn;
        @FXML private TableColumn<LogEntry, String> lastLogColumn;
        @FXML private TableColumn<LogEntry, Void> actionsColumn;
        @FXML private TableColumn<LogEntry, Void> editColumn;
    
        // ==================== PAGINATION ====================
        @FXML private Label paginationStatusLabel;
        @FXML private Button prevPageButton;
        @FXML private Button page1Button;
        @FXML private Button page2Button;
        @FXML private Button page3Button;
        @FXML private Button ellipsisButton;
        @FXML private Button lastPageButton;
        @FXML private Button nextPageButton;
    
        // ==================== DATA MODELS ====================
        private final ObservableList<LogEntry> allLogEntries = FXCollections.observableArrayList();
        private final ObservableList<LogEntry> currentPageData = FXCollections.observableArrayList();
        private int currentPage = 1;
        private int totalPages = 1;
        private int totalItems = 0;
    
        private final BYODService byodService = new BYODService();
    
        @Override
        public void initialize(URL location, ResourceBundle resources) {
            try {
                addStylesheetToScene();
                setupTableColumns();
                setupSearchListener();
                loadInitialData();
                updateCardNumbers();
                updatePagination();
    
                // FIX: Formats the Date Picker Button with the actual current system date
                if (datePickerButton != null) {
                    String currentDateStr = DateTimeFormatter.ofPattern("MMMM d, yyyy").format(LocalDate.now());
                    datePickerButton.setText(currentDateStr);
                }
    
                if (monitoringButton != null) {
                    monitoringButton.getStyleClass().add("active");
                }

                startNavClock();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize monitoring view", e);
                showAlert("Initialization Error", "Failed to initialize monitoring view:\n" + e.getMessage());
            }
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

        private void addStylesheetToScene() {
            if (monitoringButton != null && monitoringButton.getScene() != null) {
                Scene scene = monitoringButton.getScene();
                String css = getClass().getResource(STYLESHEET_PATH).toExternalForm();
                if (!scene.getStylesheets().contains(css)) {
                    scene.getStylesheets().add(css);
                }
            }
        }
    
        private void setupTableColumns() {
            studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
            studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            deviceSerialColumn.setCellValueFactory(new PropertyValueFactory<>("deviceSerial"));
            lastLogColumn.setCellValueFactory(new PropertyValueFactory<>("lastLog"));
    
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            statusColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    getStyleClass().removeAll("table-status", "table-status-in", "table-status-out");
                    if (empty || status == null) {
                        setText(null);
                    } else {
                        setText(status);
                        getStyleClass().add("table-status");
                        if (STATUS_INGRESS.equalsIgnoreCase(status)) {
                            getStyleClass().add("table-status-in");
                        } else if (STATUS_EGRESS.equalsIgnoreCase(status)) {
                            getStyleClass().add("table-status-out");
                        }
                    }
                }
            });
    
            actionsColumn.setCellFactory(param -> new TableCell<>() {
                private final Button historyButton = new Button("👁️‍🗨️");
                {
                    historyButton.getStyleClass().add("table-action");
                    historyButton.setOnAction(e -> {
                        LogEntry entry = getTableView().getItems().get(getIndex());
                        if (entry != null) onViewHistory(entry);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        setGraphic(historyButton);
                    }
                }
            });
    
            editColumn.setCellFactory(param -> new TableCell<>() {
                private final Button ingressButton = new Button("📥 In");
                private final Button egressButton = new Button("📤 Out");
                {
                    ingressButton.getStyleClass().add("table-edit");
                    egressButton.getStyleClass().add("table-edit");
    
                    ingressButton.setOnAction(e -> {
                        LogEntry entry = getTableView().getItems().get(getIndex());
                        if (entry != null) onMarkIngress(entry);
                    });
    
                    egressButton.setOnAction(e -> {
                        LogEntry entry = getTableView().getItems().get(getIndex());
                        if (entry != null) {
                            triggerQRCameraEgress();
                        }
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                        return;
                    }
                    LogEntry entry = getTableView().getItems().get(getIndex());
                    if (STATUS_INGRESS.equalsIgnoreCase(entry.getStatus())) {
                        egressButton.getStyleClass().removeAll("table-edit-in", "table-edit-out");
                        egressButton.getStyleClass().add("table-edit-out");
                        setGraphic(egressButton);
                    } else {
                        ingressButton.getStyleClass().removeAll("table-edit-in", "table-edit-out");
                        ingressButton.getStyleClass().add("table-edit-in");
                        setGraphic(ingressButton);
                    }
                }
            });
        }
    
        private void setupSearchListener() {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                currentPage = 1;
                filterAndPaginate();
            });
        }
    
        private void filterAndPaginate() {
            String searchTerm = searchField.getText().toLowerCase();
            ObservableList<LogEntry> filtered = allLogEntries.filtered(entry ->
                    entry.getStudentName().toLowerCase().contains(searchTerm) ||
                            entry.getStudentId().toLowerCase().contains(searchTerm) ||
                            entry.getDeviceSerial().toLowerCase().contains(searchTerm)
            );
            totalItems = filtered.size();
            totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ROWS_PER_PAGE));
            if (currentPage > totalPages) currentPage = totalPages;
    
            int fromIndex = (currentPage - 1) * ROWS_PER_PAGE;
            int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, totalItems);
            if (fromIndex < toIndex) {
                currentPageData.setAll(filtered.subList(fromIndex, toIndex));
            } else {
                currentPageData.clear();
            }
            monitoringTableView.setItems(currentPageData);
            updatePaginationStatus();
            updatePageButtons();
        }
    
        private void updatePagination() {
            filterAndPaginate();
        }
    
        private void updatePaginationStatus() {
            int start = totalItems == 0 ? 0 : (currentPage - 1) * ROWS_PER_PAGE + 1;
            int end = Math.min(currentPage * ROWS_PER_PAGE, totalItems);
            paginationStatusLabel.setText(String.format("Showing %d-%d of %d log entries today", start, end, totalItems));
        }
    
        private void updatePageButtons() {
            page1Button.setText("1");
            page1Button.setDisable(totalPages < 1);
            page1Button.setVisible(true);
    
            if (totalPages <= 2) {
                if (totalPages == 2) {
                    page2Button.setText("2");
                    page2Button.setDisable(false);
                    page3Button.setVisible(false);
                    page3Button.setDisable(true);
                } else {
                    page2Button.setVisible(false);
                    page3Button.setVisible(false);
                }
                ellipsisButton.setVisible(false);
                lastPageButton.setText(String.valueOf(totalPages));
                lastPageButton.setDisable(currentPage >= totalPages);
                prevPageButton.setDisable(currentPage == 1);
                nextPageButton.setDisable(currentPage >= totalPages);
                updateActivePageButton();
                return;
            }
    
            int startDynamic;
            if (currentPage <= 2) {
                startDynamic = 2;
            } else if (currentPage >= totalPages - 1) {
                startDynamic = totalPages - 2;
            } else {
                if (currentPage == 2 && totalPages > 2) {
                    startDynamic = 2;
                } else {
                    startDynamic = currentPage;
                    if (startDynamic + 1 > totalPages) {
                        startDynamic = totalPages - 1;
                    }
                    if (startDynamic == 1) startDynamic = 2;
                }
            }
    
            int secondPage = startDynamic;
            int thirdPage = Math.min(startDynamic + 1, totalPages);
    
            if (secondPage == 1) secondPage = 2;
            if (thirdPage == 1) thirdPage = 2;
    
            page2Button.setText(String.valueOf(secondPage));
            page3Button.setText(String.valueOf(thirdPage));
            page2Button.setDisable(false);
            page3Button.setDisable(thirdPage > totalPages || thirdPage == secondPage);
    
            boolean hasMore = thirdPage < totalPages;
            ellipsisButton.setVisible(hasMore);
            ellipsisButton.setDisable(!hasMore);
    
            lastPageButton.setText(String.valueOf(totalPages));
            lastPageButton.setDisable(currentPage >= totalPages);
            prevPageButton.setDisable(currentPage == 1);
            nextPageButton.setDisable(currentPage >= totalPages);
    
            updateActivePageButton();
        }
    
        private void updateActivePageButton() {
            page1Button.getStyleClass().remove("active");
            page2Button.getStyleClass().remove("active");
            page3Button.getStyleClass().remove("active");
            lastPageButton.getStyleClass().remove("active");
    
            String cur = String.valueOf(currentPage);
            if (cur.equals(page1Button.getText()))
                page1Button.getStyleClass().add("active");
            else if (cur.equals(page2Button.getText()))
                page2Button.getStyleClass().add("active");
            else if (cur.equals(page3Button.getText()))
                page3Button.getStyleClass().add("active");
            else if (cur.equals(lastPageButton.getText()))
                lastPageButton.getStyleClass().add("active");
        }
    
        private void navigateTo(String fxmlPath, String title) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource(fxmlPath.toLowerCase()));
                Stage stage = (Stage) monitoringButton.getScene().getWindow();
                Scene current = stage.getScene();
                String cssPath = getClass().getResource(STYLESHEET_PATH).toExternalForm();
                if (!current.getStylesheets().contains(cssPath)) current.getStylesheets().add(cssPath);
                current.setRoot(root);
                stage.setTitle(title);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Navigation error to " + fxmlPath, e);
                showAlert("Navigation Error", "Could not load " + fxmlPath);
            }
        }
    
        // ==================== BUTTON ACTIONS ====================
        @FXML private void onDashboardClick() { navigateTo(DASHBOARD_FXML, "Dashboard - BYOD System"); }
        @FXML private void onMonitoringClick() { refreshMonitoringData(); }
        @FXML private void onRegistrationClick() { navigateTo(REGISTRATION_FXML, "Device Registration - BYOD System"); }
        @FXML
        private void onReportsClick() {
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
        @FXML private void onAccountClick() { navigateTo(ACCOUNT_FXML, "Account Settings - BYOD System"); }
    
        @FXML private void onLogoutClick() {
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
    
        @FXML private void onScanQREgressClick() {
            triggerQRCameraEgress();
        }
    
        @FXML private void onSearch() { filterAndPaginate(); }
        @FXML private void onAllDeviceTypesFilter() { filterAndPaginate(); }
        @FXML private void onAllStatusFilter() { filterAndPaginate(); }
        @FXML private void onDatePicker() { LOGGER.info("Date filter clicked"); }
        @FXML private void onExportLog() { LOGGER.info("Export CSV handling triggered"); }
        @FXML private void onLogEntry() { LOGGER.info("Manual row creation requested"); }
    
        @FXML private void onPrevPage() { if (currentPage > 1) { currentPage--; updatePagination(); } }
        @FXML private void onPage1() {
            try {
                int page = Integer.parseInt(page1Button.getText());
                if (page != currentPage && page >= 1 && page <= totalPages) {
                    currentPage = page;
                    updatePagination();
                }
            } catch (NumberFormatException ignored) { }
        }
        @FXML private void onPage2() {
            if (page2Button.isDisabled()) return;
            try { currentPage = Integer.parseInt(page2Button.getText()); updatePagination(); } catch (NumberFormatException ignored) { }
        }
        @FXML private void onPage3() {
            if (page3Button.isDisabled()) return;
            try { currentPage = Integer.parseInt(page3Button.getText()); updatePagination(); } catch (NumberFormatException ignored) { }
        }
        @FXML private void onEllipsis() {
            int third = Integer.parseInt(page3Button.getText());
            if (third < totalPages) { currentPage = Math.min(third + 1, totalPages); updatePagination(); }
        }
        @FXML private void onLastPage() { currentPage = totalPages; updatePagination(); }
        @FXML private void onNextPage() { if (currentPage < totalPages) { currentPage++; updatePagination(); } }
    
        // ==================== TABLE ACTION HANDLERS ====================
        private void onViewHistory(LogEntry entry) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Log Snapshot Context");
            info.setHeaderText("Viewing Log Details");
            info.setContentText("Student ID: " + entry.getStudentId() +
                    "\nStudent Name: " + entry.getStudentName() +
                    "\nDevice Details: " + entry.getDeviceSerial() +
                    "\nLast Recorded Action: " + entry.getLastLog());
            info.showAndWait();
        }
    
        private void onMarkIngress(LogEntry entry) {
            try {
                byodService.updateIngress(entry.getStudentId(), entry.getStudentName(), entry.getDeviceSerial());
                refreshMonitoringData();
            } catch (Exception e) {
                showAlert("Database Ingress Error", e.getMessage());
            }
        }
    
        private void triggerQRCameraEgress() {
            Stage currentStage = (Stage) monitoringTableView.getScene().getWindow();
    
            QRScannerWindow.openScanner(currentStage, qrPayload -> {
                try {
                    String studentId = qrPayload;
                    if (qrPayload.contains("|")) {
                        studentId = qrPayload.split("\\|")[0];
                    }
                    byodService.updateEgress(studentId);
                    showSuccessAlert("QR Scanned! You can now exit the Campus.");
                    refreshMonitoringData();
                } catch (Exception ex) {
                    showAlert("Scanner Database Error", "Failed to process scanned code:\n" + ex.getMessage());
                }
            });
        }
    
        // ==================== DATA LOADING ====================
        private void loadInitialData() {
            allLogEntries.clear();
            try {
                for (Object[] row : byodService.fetchLogs()) {
                    String egress = (String) row[5];
                    String status = (egress == null) ? "Ingress" : "Egress";
                    String timestamp = status.equals("Ingress") ? (String) row[4] : egress;
    
                    allLogEntries.add(new LogEntry(
                            (String) row[2],
                            (String) row[1],
                            (String) row[3],
                            status,
                            timestamp
                    ));
                }
            } catch (Exception e) {
                showAlert("Database Error", e.getMessage());
            }
        }
    
        private void refreshMonitoringData() {
            loadInitialData();
            updateCardNumbers();
            currentPage = 1;
            filterAndPaginate();
        }
    
        private void updateCardNumbers() {
            try {
                Map<String, Integer> metrics = byodService.fetchDashboardMetrics();
                if (!metrics.isEmpty()) {
                    totalStudentsLabel.setText(String.valueOf(metrics.get("totalStudents")));
                    totalDevicesLabel.setText(String.valueOf(metrics.get("totalDevices")));
                    devicesInsideLabel.setText(String.valueOf(metrics.get("devicesInside")));
                    ingressTodayLabel.setText(String.valueOf(metrics.get("ingressToday")));
                    egressTodayLabel.setText(String.valueOf(metrics.get("egressToday")));
    
                    syncNoDevices.setText("Devices in campus: " + devicesInsideLabel.getText());
                    syncNoDevices.getStyleClass().removeAll("sync-status-live");
                    syncNoDevices.getStyleClass().add("sync-status-live");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not load database analytical metrics counters", e);
            }
        }
    
        // ==================== HELPER METHODS ====================
        private void showSuccessAlert(String message) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Access Granted");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    
        private void showAlert(String title, String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }