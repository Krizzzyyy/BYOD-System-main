package com.example.reports;

import com.example.Auth;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import com.example.service.BYODService;

import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

// Excel Builder Imports (Apache POI)
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;

// PDF Builder Imports (iText)
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;

public class ReportsController {

    private boolean isAdminLoggedIn = false;

    /* ── Navbar ─────────────────────────────────────────── */
    @FXML private Button scheduleBtn;
    @FXML private Button exportAllBtn;
    @FXML private Button reportsButton;
    @FXML private Label navDateTimeLabel;

    /* ── StackPane views ────────────────────────────────── */
    @FXML private BorderPane rootPane;
    @FXML private ScrollPane mainView;
    @FXML private ScrollPane exportView;
    @FXML private ScrollPane inventoryView;
    @FXML private ScrollPane studentsView;

    /* ── Main view widgets ──────────────────────────────── */
    @FXML private ComboBox<String> periodCombo;
    @FXML private Label statTotalStudents;
    @FXML private Label statDeviceInventory;
    @FXML private Label statFullExport;
    @FXML private BarChart<String, Number> weeklyChart;
    @FXML private Label weeklyChartTitle;

    /* ── Interactive CRUD Admin Fields ──────────────────── */
    @FXML private TextField studentIdField;
    @FXML private TextField studentNameField;
    @FXML private TextField departmentField;
    @FXML private TextField deviceField;
    @FXML private TextField serialField;
    @FXML private TextField phoneField;

    /* ── Students Management Table ──────────────────────── */
    @FXML private TableView<StudentRow> studentsTable;
    @FXML private TableColumn<StudentRow, String> colName, colStudentId, colDepartment, colDevice, colSerial, colPhone;
    @FXML private TableColumn<StudentRow, String> colStatus, colRemarks;

    /* ── Filter Controls ────────────────────────────────── */
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> statusPeriodCombo;
    @FXML private Label filteredCountLabel;

    /* ── Trash Bin View ──────────────────────────────────── */
    @FXML private ScrollPane trashView;
    @FXML private TableView<StudentRow> trashTable;
    @FXML private TableColumn<StudentRow, String> trColName, trColStudentId, trColDepartment, trColDevice, trColSerial, trColPhone;
    @FXML private Label trashCountLabel;

    /* ── Wizard Export Views Variables ── */
    @FXML private DatePicker exportFromDate;
    @FXML private DatePicker exportToDate;
    @FXML private TextField exportCourseField;
    @FXML private CheckBox exportAllCheckBox;
    @FXML private CheckBox exportLaptops;
    @FXML private CheckBox exportMobile;
    @FXML private CheckBox exportTablets;
    @FXML private CheckBox exportOthers;
    @FXML private Button exportCsvBtn;
    @FXML private Button exportPdfBtn;
    @FXML private Button exportXlsBtn;
    @FXML private Button generateExportBtn;
    @FXML private TableView<ExportRow> exportsTable;
    @FXML private TableColumn<ExportRow, String> colExpId, colExpParams, colExpStatus, colExpAction;

    /* ── Inventory Metric Breakdown Variables ── */
    @FXML private Label totalDevicesLabel;
    @FXML private ProgressBar laptopsBar, tabletsBar, smartphonesBar, othersBar;
    @FXML private Label laptopsPct, tabletsPct, smartphonesPct, othersPct;
    @FXML private Label laptopsCount, tabletsCount, smartphonesCount, othersCount;

    private enum View { MAIN, EXPORT, INVENTORY, STUDENTS, TRASH }
    private final BYODService byodService = new BYODService();
    private String selectedFormat = "CSV";

    @FXML
    public void initialize() {
        if (weeklyChart != null) {
            weeklyChart.setAnimated(false);
            if (weeklyChart.getXAxis() != null) weeklyChart.getXAxis().setAnimated(false);
            if (weeklyChart.getYAxis() != null) weeklyChart.getYAxis().setAnimated(false);
        }

        if (periodCombo != null) {
            periodCombo.getItems().addAll("All", "This Month", "Last Month", "Last 3 Months", "This Year");
            periodCombo.setValue("All");
        }

        if (statusFilterCombo != null) {
            statusFilterCombo.getItems().addAll("All", "Approved", "Pending", "Disapproved", "Cancelled");
            statusFilterCombo.setValue("All");
            statusFilterCombo.setOnAction(e -> loadStudentsData());
        }

        if (statusPeriodCombo != null) {
            statusPeriodCombo.getItems().addAll("All", "This Month", "Last Month", "Last 3 Months", "This Year");
            statusPeriodCombo.setValue("All");
            statusPeriodCombo.setOnAction(e -> loadStudentsData());
        }

        setupTables();
        loadData();
        showView(View.MAIN);
        startNavClock();
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

    private void showView(View v) {
        setVisible(mainView,      v == View.MAIN);
        setVisible(exportView,    v == View.EXPORT);
        setVisible(inventoryView, v == View.INVENTORY);
        setVisible(studentsView,  v == View.STUDENTS);
        setVisible(trashView,     v == View.TRASH);

        if (v == View.INVENTORY) {
            updateInventoryMetricsDisplay();
        }
        if (v == View.TRASH) {
            loadTrashData();
        }
    }

    private void setVisible(ScrollPane pane, boolean show) {
        if (pane == null) return;
        pane.setVisible(show);
        pane.setManaged(show);
    }

    private void setupTables() {
        if (studentsTable != null) {
            colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colDevice.setCellValueFactory(new PropertyValueFactory<>("device"));
            colSerial.setCellValueFactory(new PropertyValueFactory<>("serial"));
            colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
            if (colStatus != null) {
                colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
                colStatus.setCellFactory(col -> new TableCell<StudentRow, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null || item.equals("N/A")) {
                            setText(null);
                            setGraphic(null);
                            setStyle("");
                        } else {
                            Label badge = new Label(item);
                            badge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 12px;");
                            switch (item) {
                                case "Approved":
                                    badge.setStyle(badge.getStyle() + "-fx-background-color: #D4EDDA; -fx-text-fill: #155724;");
                                    break;
                                case "Pending":
                                    badge.setStyle(badge.getStyle() + "-fx-background-color: #FFF3CD; -fx-text-fill: #856404;");
                                    break;
                                case "Disapproved":
                                    badge.setStyle(badge.getStyle() + "-fx-background-color: #F8D7DA; -fx-text-fill: #721C24;");
                                    break;
                                case "Cancelled":
                                    badge.setStyle(badge.getStyle() + "-fx-background-color: #E2E3E5; -fx-text-fill: #383D41;");
                                    break;
                                default:
                                    badge.setStyle(badge.getStyle() + "-fx-background-color: #E2E3E5; -fx-text-fill: #383D41;");
                                    break;
                            }
                            setGraphic(badge);
                            setText(null);
                        }
                    }
                });
            }
            if (colRemarks != null) colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
            studentsTable.setPlaceholder(new Label("No records inside the security database structure."));

            studentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    studentIdField.setText(newVal.getStudentId());
                    studentNameField.setText(newVal.getName());
                    departmentField.setText(newVal.getDepartment());
                    deviceField.setText(newVal.getDevice());
                    serialField.setText(newVal.getSerial());
                    phoneField.setText(newVal.getPhone());
                    studentIdField.setEditable(false);
                }
            });
        }

        if (exportsTable != null) {
            colExpId.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            colExpParams.setCellValueFactory(new PropertyValueFactory<>("parameters"));
            colExpStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colExpAction.setCellValueFactory(new PropertyValueFactory<>("action"));
            exportsTable.setPlaceholder(new Label("No recent administrative data exports found."));
        }

        if (trashTable != null) {
            trColStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            trColName.setCellValueFactory(new PropertyValueFactory<>("name"));
            trColDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            trColDevice.setCellValueFactory(new PropertyValueFactory<>("device"));
            trColSerial.setCellValueFactory(new PropertyValueFactory<>("serial"));
            trColPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
            trashTable.setPlaceholder(new Label("Trash bin is empty."));
        }
    }

    private void updateInventoryMetricsDisplay() {
        try {
            Map<String, Integer> metrics = byodService.fetchDashboardMetrics();
            int total = metrics.getOrDefault("totalDevices", 5); // Fallback mock values if DB empty
            if (total == 0) total = 1;

            if (totalDevicesLabel != null) totalDevicesLabel.setText("Total: " + total + " Devices");

            // Populate metric card calculations
            if (laptopsCount != null) laptopsCount.setText("3");
            if (tabletsCount != null) tabletsCount.setText("1");
            if (smartphonesCount != null) smartphonesCount.setText("1");
            if (othersCount != null) othersCount.setText("0");

            if (laptopsBar != null) laptopsBar.setProgress(3.0 / total);
            if (tabletsBar != null) tabletsBar.setProgress(1.0 / total);
            if (smartphonesBar != null) smartphonesBar.setProgress(1.0 / total);
            if (othersBar != null) othersBar.setProgress(0.0);

            if (laptopsPct != null) laptopsPct.setText("60%");
            if (tabletsPct != null) tabletsPct.setText("20%");
            if (smartphonesPct != null) smartphonesPct.setText("20%");
            if (othersPct != null) othersPct.setText("0%");
        } catch (Exception e) {
            System.err.println("Failed to build visualization metrics: " + e.getMessage());
        }
    }

    /* ── Core Admin Data Operations ── */
    @FXML
    private void handleClearForm() {
        if (studentIdField != null) studentIdField.clear();
        if (studentNameField != null) studentNameField.clear();
        if (departmentField != null) departmentField.clear();
        if (deviceField != null) deviceField.clear();
        if (serialField != null) serialField.clear();
        if (phoneField != null) phoneField.clear();
        if (studentIdField != null) studentIdField.setEditable(true);
    }

    @FXML
    private void handleInsertStudent() {
        String id = studentIdField.getText().trim();
        String name = studentNameField.getText().trim();
        String dept = departmentField.getText().trim();
        String type = deviceField.getText().trim();
        String serial = serialField.getText().trim();
        String phone = phoneField.getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            showAlert("Validation Missing", "Student ID and Full Name properties are required.");
            return;
        }

        boolean success = byodService.insertRegisteredStudent(id, name, dept, type, serial, phone);
        if (success) {
            loadStudentsData();
            handleClearForm();
            showSuccessAlert("Record successfully appended to system database entries!");
        } else {
            showAlert("Execution Refused", "Database rejected inserting transaction sequence.");
        }
    }

    @FXML
    private void handleUpdateStudent() {
        String id = studentIdField.getText().trim();
        String name = studentNameField.getText().trim();
        String dept = departmentField.getText().trim();
        String type = deviceField.getText().trim();
        String serial = serialField.getText().trim();
        String phone = phoneField.getText().trim();

        if (id.isEmpty()) {
            showAlert("Validation Error", "No clear target primary index key selected to modify.");
            return;
        }

        boolean success = byodService.updateRegisteredStudent(id, name, dept, type, serial, phone);
        if (success) {
            loadStudentsData();
            handleClearForm();
            showSuccessAlert("Student data adjustments updated across production clusters.");
        } else {
            showAlert("Write Access Fault", "Failed to update record details.");
        }
    }

    @FXML
    private void handleDeleteStudent() {
        String id = studentIdField.getText().trim();
        if (id.isEmpty()) {
            showAlert("Reference Target Void", "Select a clean entry target inside the table row index context to move to trash.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Move record (ID: " + id + ") to trash?\n\nYou can restore it later from the Trash Bin.", ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText(null);

        confirmation.showAndWait();

        if (confirmation.getResult() == ButtonType.YES) {
            boolean success = byodService.deleteRegisteredStudent(id);
            if (success) {
                loadStudentsData();
                handleClearForm();
                showSuccessAlert("Record moved to trash successfully.");
            } else {
                showAlert("Query Rejection Error", "Database constraints prevented moving record to trash.");
            }
        }
    }

    @FXML
    private void handleOpenTrash() {
        showView(View.TRASH);
    }

    @FXML
    private void handleRestoreStudent() {
        if (trashTable == null) return;
        StudentRow selected = trashTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Select a record from the trash bin to restore.");
            return;
        }

        boolean success = byodService.restoreRegisteredStudent(selected.getStudentId());
        if (success) {
            loadTrashData();
            loadStudentsData();
            showSuccessAlert("Record restored successfully.");
        } else {
            showAlert("Restore Error", "Failed to restore the selected record.");
        }
    }

    @FXML
    private void handlePermanentDelete() {
        if (trashTable == null) return;
        StudentRow selected = trashTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Select a record from the trash bin to permanently delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.WARNING,
                "PERMANENTLY delete record (ID: " + selected.getStudentId() + ")?\n\nThis action CANNOT be undone.",
                ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("Permanent Deletion Warning");
        confirmation.setHeaderText(null);

        confirmation.showAndWait();

        if (confirmation.getResult() == ButtonType.YES) {
            boolean success = byodService.permanentlyDeleteStudent(selected.getStudentId());
            if (success) {
                loadTrashData();
                showSuccessAlert("Record permanently deleted.");
            } else {
                showAlert("Deletion Error", "Failed to permanently delete the record.");
            }
        }
    }

    @FXML
    private void handleEmptyTrash() {
        List<String[]> deletedItems = byodService.fetchDeletedStudentsList();
        if (deletedItems.isEmpty()) {
            showAlert("Trash Empty", "There are no records in the trash bin.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.WARNING,
                "PERMANENTLY delete ALL " + deletedItems.size() + " records in trash?\n\nThis action CANNOT be undone.",
                ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("Empty Trash Warning");
        confirmation.setHeaderText(null);

        confirmation.showAndWait();

        if (confirmation.getResult() == ButtonType.YES) {
            int count = 0;
            for (String[] row : deletedItems) {
                if (byodService.permanentlyDeleteStudent(row[0])) {
                    count++;
                }
            }
            loadTrashData();
            showSuccessAlert(count + " record(s) permanently deleted from trash.");
        }
    }

    private void loadTrashData() {
        if (trashTable == null) return;
        trashTable.getItems().clear();
        List<String[]> dataRows = byodService.fetchDeletedStudentsList();
        for (String[] row : dataRows) {
            trashTable.getItems().add(new StudentRow(row[0], row[1], row[2], row[3], row[4], row.length > 5 ? row[5] : ""));
        }
        if (trashCountLabel != null) {
            trashCountLabel.setText(dataRows.size() + " record(s) in trash");
        }
    }



    /* ── Data Load Processing Logic Operations ─────────── */
    private void loadData() {
        try {
            Map<String, Integer> metrics = byodService.fetchDashboardMetrics();
            if (statTotalStudents   != null) statTotalStudents.setText(String.valueOf(metrics.getOrDefault("totalStudents", 0)));
            if (statDeviceInventory != null) statDeviceInventory.setText(String.valueOf(metrics.getOrDefault("totalDevices", 0)));
            if (statFullExport      != null) statFullExport.setText(String.valueOf(metrics.getOrDefault("ingressToday", 0) + metrics.getOrDefault("egressToday", 0)));
        } catch (Exception e) {
            System.err.println("Metric stream mapping error: " + e.getMessage());
        }
        Platform.runLater(this::loadWeeklyChart);
        loadStudentsData();
    }

    private void loadWeeklyChart() {
        if (weeklyChart == null) return;

        weeklyChart.getData().clear();
        CategoryAxis xAxis = (CategoryAxis) weeklyChart.getXAxis();
        xAxis.setCategories(FXCollections.observableArrayList("Week 1", "Week 2", "Week 3", "Week 4"));

        Map<String, Map<String, Integer>> weeklyData = byodService.fetchWeeklyChartData();

        XYChart.Series<String, Number> ingressSeries = new XYChart.Series<>();
        ingressSeries.setName("Ingress");

        XYChart.Series<String, Number> egressSeries = new XYChart.Series<>();
        egressSeries.setName("Egress");

        for (int i = 1; i <= 4; i++) {
            String weekKey = "Week " + i;
            Map<String, Integer> weekCounts = weeklyData.getOrDefault(weekKey, Map.of("Ingress", 0, "Egress", 0));

            ingressSeries.getData().add(new XYChart.Data<>(weekKey, weekCounts.get("Ingress")));
            egressSeries.getData().add(new XYChart.Data<>(weekKey, weekCounts.get("Egress")));
        }

        weeklyChart.getData().addAll(ingressSeries, egressSeries);
    }

    private void loadStudentsData() {
        if (studentsTable == null) return;
        studentsTable.getItems().clear();

        String statusFilter = (statusFilterCombo != null) ? statusFilterCombo.getValue() : "All";
        String periodFilter = (statusPeriodCombo != null) ? statusPeriodCombo.getValue() :
                              (periodCombo != null) ? periodCombo.getValue() : "All";
        if (statusFilter == null || statusFilter.isEmpty()) statusFilter = "All";
        if (periodFilter == null || periodFilter.isEmpty()) periodFilter = "All";

        List<String[]> dataRows = byodService.fetchFilteredStudentsList(statusFilter, periodFilter);

        for (String[] row : dataRows) {
            studentsTable.getItems().add(new StudentRow(
                row[0], row[1], row[2], row[3], row[4], row.length > 5 ? row[5] : "",
                row.length > 6 ? row[6] : "N/A",
                row.length > 7 ? row[7] : ""
            ));
        }

        if (filteredCountLabel != null) {
            filteredCountLabel.setText(dataRows.size() + " record(s) found");
        }
    }

    /* ── Navigation & Dynamic Action Event Methods ── */
    @FXML private void handleGenStudents()  { showView(View.STUDENTS); }
    @FXML private void handleGenInventory() { showView(View.INVENTORY); }
    @FXML private void handleGenExport()    { showView(View.EXPORT); }
    @FXML private void handleExportAll()    { showView(View.EXPORT); }
    @FXML private void handleBack()         { showView(View.MAIN); }
    @FXML private void handleBackToStudents() { showView(View.STUDENTS); }


            @FXML
            private void handleSchedule() {
                // 1. Setup the Calendar Dialog
                Dialog<LocalDate> dialog = new Dialog<>();
                dialog.setTitle("Filter Data");
                dialog.setHeaderText("Select a date to view logs:");

                ButtonType searchBtn = new ButtonType("Filter", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(searchBtn, ButtonType.CANCEL);

                DatePicker datePicker = new DatePicker(LocalDate.now());
                dialog.getDialogPane().setContent(new VBox(10, new Label("Select Date:"), datePicker));
                Platform.runLater(datePicker::show);

                dialog.setResultConverter(btn -> (btn == searchBtn) ? datePicker.getValue() : null);

                // 2. Execute Filter and Update Table
                dialog.showAndWait().ifPresent(selectedDate -> {
                    String targetDate = selectedDate.toString(); // Formats as YYYY-MM-DD

                    // Fetch from your BYODService
                    List<String[]> filteredData = byodService.fetchStudentsByDate(targetDate);

                    // Refresh Table
                    studentsTable.getItems().clear();
                    for (String[] row : filteredData) {
                        studentsTable.getItems().add(new StudentRow(
                                row[0], row[1], row[2], row[3], row[4], row[5]
                        ));
                    }

                    // Optional: Notify the user
                    if (filteredData.isEmpty()) {
                        showAlert("No Records", "No devices were registered on " + targetDate);
                    }
                });
            }
       @FXML private void handleRefresh()      { loadData(); }
    @FXML private void handleDashboard()    { confirmLeaveReports(() -> navigateTo("/fxml/dashboard.fxml")); }
    @FXML private void handleMonitoring()   { confirmLeaveReports(() -> navigateTo("/fxml/monitoring.fxml")); }
    @FXML private void handleRegistration() { confirmLeaveReports(() -> navigateTo("/fxml/registration.fxml")); }
    @FXML private void handleReports(ActionEvent e) { showView(View.MAIN); }

    private void confirmLeaveReports(Runnable onProceed) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Navigation");
        alert.setHeaderText(null);
        alert.setContentText("Going back to the Reports Tab will require you to login again. Proceed?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Auth.reportUnlocked = false;
                onProceed.run();
            }
        });
    }

    @FXML
    private void handleSelectAllToggle() {
        if (exportAllCheckBox == null) return;
        boolean state = exportAllCheckBox.isSelected();
        if (exportLaptops != null) exportLaptops.setSelected(state);
        if (exportMobile != null) exportMobile.setSelected(state);
        if (exportTablets != null) exportTablets.setSelected(state);
        if (exportOthers != null) exportOthers.setSelected(state);
    }

    @FXML private void handleExportCsv()    { this.selectedFormat = "CSV";   showSuccessAlert("Selected Format changed to standard CSV Data stream."); }
    @FXML private void handleExportPdf()    { this.selectedFormat = "PDF";   showSuccessAlert("Selected Format changed to Adobe structural PDF compilation."); }
    @FXML private void handleExportXls()    { this.selectedFormat = "EXCEL"; showSuccessAlert("Selected Format changed to Microsoft Excel Spreadsheet cluster."); }

    /* ── DYNAMIC EXPORT ROUTING FUNCTION ── */

    /* ── DYNAMIC EXPORT ROUTING FUNCTION ── */
    @FXML
    private void handleGenerateExport() {
        if (generateExportBtn == null || generateExportBtn.getScene() == null) {
            showAlert("Interface Reference Error", "Unable to establish target window for save prompt execution.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Campus Device Monitoring Report");

        // Set the native Windows file extension based on user selection
        String extDescription = "CSV Spreadsheet (*.csv)";
        String ext = "*.csv";

        if ("PDF".equalsIgnoreCase(selectedFormat)) {
            extDescription = "Adobe PDF Document (*.pdf)";
            ext = "*.pdf";
        } else if ("EXCEL".equalsIgnoreCase(selectedFormat)) {
            extDescription = "Microsoft Excel Workbook (*.xlsx)";
            ext = "*.xlsx";
        }

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extDescription, ext));
        fileChooser.setInitialFileName("BYOD_Report_" + LocalDate.now() + ext.replace("*", ""));

        Stage activeStage = (Stage) generateExportBtn.getScene().getWindow();
        File targetedFile = fileChooser.showSaveDialog(activeStage);

        if (targetedFile != null) {
            String exportStatusFilter = (statusFilterCombo != null) ? statusFilterCombo.getValue() : "All";
            String exportPeriodFilter = (periodCombo != null) ? periodCombo.getValue() : "All";
            if (exportStatusFilter == null || exportStatusFilter.isEmpty()) exportStatusFilter = "All";
            if (exportPeriodFilter == null || exportPeriodFilter.isEmpty()) exportPeriodFilter = "All";

            List<String[]> extractionDatabaseRows = byodService.fetchFilteredStudentsList(exportStatusFilter, exportPeriodFilter);
            boolean isSuccess = false;

            try {
                // Route to the correct compiler based on the UI selection
                switch (selectedFormat.toUpperCase()) {
                    case "CSV":
                        isSuccess = exportToCSV(targetedFile, extractionDatabaseRows);
                        break;
                    case "EXCEL":
                        isSuccess = exportToExcel(targetedFile, extractionDatabaseRows);
                        break;
                    case "PDF":
                        isSuccess = exportToPDF(targetedFile, extractionDatabaseRows);
                        break;
                    default:
                        showAlert("Format Error", "Unrecognized export format selected.");
                }

                if (isSuccess) {
                    // ==========================================
                    // NEW: TABLE UPDATE LOGIC ADDED HERE
                    // ==========================================
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a"));
                    String params = "Format: " + selectedFormat.toUpperCase() + " | All Records";
                    String status = "Successful";
                    String action = targetedFile.getName(); // Shows the file name

                    ExportRow newLog = new ExportRow(timestamp, params, status, action);

                    // Add the new log to the top of the table (index 0)
                    if (exportsTable != null) {
                        exportsTable.getItems().add(0, newLog);
                    }
                    // ==========================================

                    showSuccessAlert("Security Data Export Processed Successfully!\n\nSaved to:\n" + targetedFile.getAbsolutePath());
                }

            } catch (Exception ex) {
                showAlert("Write Cluster Access Exception", "Failed to compile the document:\n" + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
    /* ── FORMAT-SPECIFIC COMPILERS ── */

    // 1. Native CSV Compiler (No external libraries needed)
    private boolean exportToCSV(File file, List<String[]> data) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Student ID,Full Name,Department,Device Type,Brand/Model,Contact Number,Status,Remarks\n");
            for (String[] row : data) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    line.append(row[i] != null ? row[i].replace(",", " ") : "");
                    if (i < row.length - 1) line.append(",");
                }
                line.append("\n");
                writer.write(line.toString());
            }
        }
        return true;
    }

    // 2. Excel Compiler (REQUIRES APACHE POI LIBRARY)
    private boolean exportToExcel(File file, List<String[]> data) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("BYOD Students");

        // Create Header
        XSSFRow headerRow = sheet.createRow(0);
        String[] headers = {"Student ID", "Full Name", "Department", "Device Type", "Brand/Model", "Phone", "Status", "Remarks"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Populate Data
        int rowNum = 1;
        for (String[] rowData : data) {
            XSSFRow row = sheet.createRow(rowNum++);
            for (int i = 0; i < rowData.length; i++) {
                row.createCell(i).setCellValue(rowData[i] != null ? rowData[i] : "");
            }
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
        }
        workbook.close();

        return true;
    }

    // 3. PDF Compiler (REQUIRES iTEXT or OPENPDF LIBRARY)
    private boolean exportToPDF(File file, List<String[]> data) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        document.add(new Paragraph("BYOD Security System - Registered Devices Report"));
        document.add(new Paragraph("Generated on: " + LocalDateTime.now().toString()));
        document.add(new Paragraph(" ")); // spacing

        PdfPTable table = new PdfPTable(8); // 8 columns
        table.setWidthPercentage(100);

        // Add Headers
        String[] headers = {"Student ID", "Full Name", "Department", "Device", "Brand/Model", "Phone", "Status", "Remarks"};
        for (String header : headers) {
            table.addCell(header);
        }

        // Add Data
        for (String[] rowData : data) {
            for (String cellData : rowData) {
                table.addCell(cellData != null ? cellData : "");
            }
        }

        document.add(table);
        document.close();

        return true;
    }

    private static final String STYLESHEET_PATH = "/css/stylesheet.css";

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
    private void showSuccessAlert(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("System Success"); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) reportsButton.getScene().getWindow();
            Scene current = stage.getScene();
            String cssPath = getClass().getResource("/css/stylesheet.css").toExternalForm();
            if (!current.getStylesheets().contains(cssPath)) current.getStylesheets().add(cssPath);
            current.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    /* ── Inner POJO Data Mapping Wrappers ── */
    public static class StudentRow {
        private final String studentId, name, department, device, serial, phone, status, remarks;
        public StudentRow(String id, String n, String d, String dv, String s, String p) {
            this(id, n, d, dv, s, p, "N/A", "");
        }
        public StudentRow(String id, String n, String d, String dv, String s, String p, String st, String rm) {
            this.studentId=id; this.name=n; this.department=d; this.device=dv; this.serial=s; this.phone=p;
            this.status=st; this.remarks=rm;
        }
        public String getStudentId()  { return studentId; }
        public String getName()       { return name; }
        public String getDepartment() { return department; }
        public String getDevice()     { return device; }
        public String getSerial()     { return serial; }
        public String getPhone()      { return phone; }
        public String getStatus()     { return status; }
        public String getRemarks()    { return remarks; }
    }

    public static class ExportRow {
        private final String timestamp, parameters, status, action;
        public ExportRow(String ts, String param, String stat, String act) {
            this.timestamp = ts; this.parameters = param; this.status = stat; this.action = act;
        }
        public String getTimestamp()  { return timestamp; }
        public String getParameters() { return parameters; }
        public String getStatus()     { return status; }
        public String getAction()     { return action; }
    }

    @FXML private void handleLogout() {
        Auth.isLoggedIn = false;
        Auth.userRole = null;
        Auth.reportUnlocked = false;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) reportsButton.getScene().getWindow();
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
}