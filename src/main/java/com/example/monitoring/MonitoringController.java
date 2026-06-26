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
    import javafx.scene.layout.HBox;
    import javafx.scene.layout.VBox;
    import javafx.geometry.Insets;
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
        @FXML private ComboBox<String> deviceTypeFilter;
        @FXML private ComboBox<String> statusFilter;
        @FXML private DatePicker dateFilter;
        @FXML private Button exportLogButton;
        @FXML private Button logEntryButton;
    
        // ==================== TABLE & COLUMNS ====================
        @FXML private TableView<LogEntry> monitoringTableView;
        @FXML private TableColumn<LogEntry, String> studentNameColumn;
        @FXML private TableColumn<LogEntry, String> studentIdColumn;
        @FXML private TableColumn<LogEntry, String> deviceSerialColumn;
        @FXML private TableColumn<LogEntry, String> statusColumn;
        @FXML private TableColumn<LogEntry, String> lastLogColumn;
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
    
        // ==================== PENDING TAB ====================
        @FXML private TabPane mainTabPane;
        @FXML private Tab pendingTab;
        @FXML private Tab activeTab;
        @FXML private Label pendingCountLabel;
        @FXML private TableView<LogEntry> pendingTableView;
        @FXML private TableColumn<LogEntry, String> pendingNameColumn;
        @FXML private TableColumn<LogEntry, String> pendingIdColumn;
        @FXML private TableColumn<LogEntry, String> pendingDeviceColumn;
        @FXML private TableColumn<LogEntry, String> pendingDateColumn;
        @FXML private TableColumn<LogEntry, Void> pendingActionsColumn;
        @FXML private Button approveAllBtn;
        @FXML private Button disapproveBtn;
        @FXML private Button cancelRegBtn;

        // ==================== DATA MODELS ====================
        private final ObservableList<LogEntry> allLogEntries = FXCollections.observableArrayList();
        private final ObservableList<LogEntry> currentPageData = FXCollections.observableArrayList();
        private final ObservableList<LogEntry> pendingEntries = FXCollections.observableArrayList();
        private int currentPage = 1;
        private int totalPages = 1;
        private int totalItems = 0;
    
        private final BYODService byodService = new BYODService();
    
        @Override
        public void initialize(URL location, ResourceBundle resources) {
            try {
                addStylesheetToScene();
                setupTableColumns();
                setupPendingTableColumns();
                setupSearchListener();
                setupFilters();
                loadInitialData();
                loadPendingData();
                updateCardNumbers();
                updatePagination();
                updatePendingCount();
    
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

        private void setupFilters() {
            deviceTypeFilter.getItems().addAll(
                "Display devices",
                "Appliances",
                "Sounds and light equipment",
                "Other project prototypes",
                "Rentable items"
            );
            statusFilter.getItems().addAll(
                "Approved",
                "Disapproved",
                "Cancelled",
                "Pending"
            );
            deviceTypeFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                currentPage = 1;
                filterAndPaginate();
            });
            statusFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                currentPage = 1;
                filterAndPaginate();
            });
        }
    
        private void filterAndPaginate() {
            String searchTerm = searchField.getText().toLowerCase();
            String deviceType = deviceTypeFilter.getValue();
            String status = statusFilter.getValue();
            LocalDate date = dateFilter.getValue();

            ObservableList<LogEntry> filtered = allLogEntries.filtered(entry -> {
                boolean matchesSearch = searchTerm.isEmpty() ||
                    entry.getStudentName().toLowerCase().contains(searchTerm) ||
                    entry.getStudentId().toLowerCase().contains(searchTerm) ||
                    entry.getDeviceSerial().toLowerCase().contains(searchTerm);

                boolean matchesDeviceType = deviceType == null || deviceType.isEmpty() ||
                    entry.getDeviceSerial().toLowerCase().contains(deviceType.toLowerCase());

                boolean matchesStatus = status == null || status.isEmpty() ||
                    entry.getApprovalStatus().equalsIgnoreCase(status);

                boolean matchesDate = date == null ||
                    entry.getLastLog().contains(date.toString());

                return matchesSearch && matchesDeviceType && matchesStatus && matchesDate;
            });
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

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Reports Access");
            dialog.setHeaderText("Faculty verification required to access Reports.");

            ButtonType loginBtn = new ButtonType("Unlock", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(loginBtn, ButtonType.CANCEL);

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Enter faculty password");
            passwordField.setPrefWidth(280);

            Label errorLabel = new Label("");
            errorLabel.setStyle("-fx-text-fill: #D32F2F; -fx-font-size: 12px;");

            VBox content = new VBox(10, new Label("Password:"), passwordField, errorLabel);
            content.setPadding(new Insets(20));
            dialog.getDialogPane().setContent(content);

            dialog.setResultConverter(btn -> {
                if (btn == loginBtn) return passwordField.getText();
                return null;
            });

            dialog.showAndWait().ifPresent(password -> {
                if ("password".equals(password)) {
                    Auth.reportUnlocked = true;
                    navigateTo(REPORTS_FXML, "Reports - BYOD System");
                } else {
                    errorLabel.setText("Incorrect password. Access denied.");
                }
            });
        }
        @FXML private void onAccountClick() { navigateTo(ACCOUNT_FXML, "Account Settings - BYOD System"); }
    
        @FXML private void handleLogout() {
            Auth.isLoggedIn = false;
            Auth.userRole = null;
            Auth.reportUnlocked = false;
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
                Stage stage = (Stage) monitoringButton.getScene().getWindow();
                stage.setMaximized(false);
                stage.setMinWidth(0);
                stage.setMinHeight(0);
                stage.setMaxWidth(Double.MAX_VALUE);
                stage.setMaxHeight(Double.MAX_VALUE);
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());
                stage.setScene(scene);
                stage.setResizable(false);
                stage.centerOnScreen();
            } catch (Exception e) { e.printStackTrace(); }
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
        @FXML private void onFilterChanged() { currentPage = 1; filterAndPaginate(); }
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
                    int logId = (int) row[0];
                    String egress = (String) row[5];
                    String status = (egress == null) ? "Ingress" : "Egress";
                    String timestamp = status.equals("Ingress") ? (String) row[4] : egress;
                    String approvalStatus = row.length > 6 ? (String) row[6] : "Approved";
                    String scheduledDate = row.length > 7 ? (String) row[7] : null;

                    allLogEntries.add(new LogEntry(
                            logId,
                            (String) row[2],
                            (String) row[1],
                            (String) row[3],
                            status,
                            timestamp,
                            approvalStatus,
                            scheduledDate
                    ));
                }
            } catch (Exception e) {
                showAlert("Database Error", e.getMessage());
            }
        }
    
        private void refreshMonitoringData() {
            loadInitialData();
            loadPendingData();
            updateCardNumbers();
            updatePendingCount();
            currentPage = 1;
            filterAndPaginate();
        }
    
        private void updateCardNumbers() {
            try {
                Map<String, Integer> metrics = byodService.fetchDashboardMetrics();
                if (!metrics.isEmpty()) {
                    String devicesInside = String.valueOf(metrics.get("devicesInside"));
                    syncNoDevices.setText("Devices in campus: " + devicesInside);
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

        // ==================== PENDING APPROVALS TAB ====================
        private void setupPendingTableColumns() {
            pendingNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
            pendingIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            pendingDeviceColumn.setCellValueFactory(new PropertyValueFactory<>("deviceSerial"));
            pendingDateColumn.setCellValueFactory(new PropertyValueFactory<>("scheduledEntryDate"));

            pendingActionsColumn.setCellFactory(param -> new TableCell<>() {
                private final Button approveBtn = new Button("Approve");
                private final Button disapproveBtn = new Button("Disapprove");
                private final HBox box = new HBox(5, approveBtn, disapproveBtn);
                {
                    approveBtn.getStyleClass().add("btn-save");
                    approveBtn.setStyle("-fx-font-size:11px; -fx-padding:4 10;");
                    disapproveBtn.getStyleClass().add("action-button");
                    disapproveBtn.setStyle("-fx-font-size:11px; -fx-padding:4 10;");

                    approveBtn.setOnAction(e -> {
                        LogEntry entry = getTableView().getItems().get(getIndex());
                        if (entry != null) onApproveEntry(entry);
                    });
                    disapproveBtn.setOnAction(e -> {
                        LogEntry entry = getTableView().getItems().get(getIndex());
                        if (entry != null) onDisapproveEntry(entry);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }

        private void loadPendingData() {
            pendingEntries.clear();
            try {
                for (Object[] row : byodService.fetchPendingApprovals()) {
                    pendingEntries.add(new LogEntry(
                            (int) row[0],
                            (String) row[2],
                            (String) row[1],
                            (String) row[3],
                            "Pending",
                            (String) row[4],
                            "Pending",
                            (String) row[4]
                    ));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not load pending approvals", e);
            }
            pendingTableView.setItems(pendingEntries);
            updatePendingCount();
        }

        private void updatePendingCount() {
            if (pendingCountLabel != null) {
                pendingCountLabel.setText("Pending Registrations (" + pendingEntries.size() + ")");
            }
            if (pendingTab != null) {
                pendingTab.setText("Pending Approvals (" + pendingEntries.size() + ")");
            }
        }

        private void onApproveEntry(LogEntry entry) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Approve Registration");
            confirm.setHeaderText("Approve " + entry.getStudentName() + "?");
            confirm.setContentText("Student: " + entry.getStudentName() +
                    "\nStudent ID: " + entry.getStudentId() +
                    "\nScheduled Entry: " + entry.getScheduledEntryDate());

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        byodService.approveRegistration(entry.getLogId(), entry.getStudentId(), "Faculty");
                        refreshMonitoringData();
                    } catch (Exception ex) {
                        showAlert("Approval Error", ex.getMessage());
                    }
                }
            });
        }

        private void onDisapproveEntry(LogEntry entry) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Disapprove Registration");
            dialog.setHeaderText("Disapprove " + entry.getStudentName() + "?");
            dialog.setContentText("Reason for disapproval:");
            dialog.showAndWait().ifPresent(reason -> {
                if (reason != null && !reason.isBlank()) {
                    try {
                        byodService.disapproveRegistration(entry.getStudentId(), reason, "Faculty");
                        refreshMonitoringData();
                    } catch (Exception ex) {
                        showAlert("Disapproval Error", ex.getMessage());
                    }
                }
            });
        }

        @FXML
        private void onApproveSelected() {
            LogEntry selected = pendingTableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("No Selection", "Please select a registration to approve.");
                return;
            }
            onApproveEntry(selected);
        }

        @FXML
        private void onDisapproveSelected() {
            LogEntry selected = pendingTableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("No Selection", "Please select a registration to disapprove.");
                return;
            }
            onDisapproveEntry(selected);
        }

        @FXML
        private void onCancelSelected() {
            LogEntry selected = pendingTableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("No Selection", "Please select a registration to cancel.");
                return;
            }
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Cancel Registration");
            dialog.setHeaderText("Cancel registration for " + selected.getStudentName() + "?");
            dialog.setContentText("Reason for cancellation:");
            dialog.showAndWait().ifPresent(reason -> {
                if (reason != null && !reason.isBlank()) {
                    try {
                        byodService.cancelRegistration(selected.getStudentId(), reason, "Faculty");
                        refreshMonitoringData();
                    } catch (Exception ex) {
                        showAlert("Cancellation Error", ex.getMessage());
                    }
                }
            });
        }
    }