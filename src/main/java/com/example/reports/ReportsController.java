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
import java.util.ArrayList;
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
    @FXML private VBox mainView;
    @FXML private VBox exportView;
    @FXML private VBox inventoryView;
    @FXML private VBox studentsView;

    /* ── Main view widgets ──────────────────────────────── */
    @FXML private ComboBox<String> periodCombo;
    @FXML private Label statTotalStudents;
    @FXML private Label statDeviceInventory;
    @FXML private Label statFullExport;
    @FXML private BarChart<String, Number> weeklyChart;
    @FXML private ComboBox<String> chartPeriodCombo;
    @FXML private Label weeklyChartTitle;

    /* ── Interactive CRUD Admin Fields ──────────────────── */
    @FXML private TextField studentIdField;
    @FXML private TextField studentNameField;
    @FXML private TextField yearSectionField;
    @FXML private TextField colorDescField;
    @FXML private ComboBox<String> userCategoryCombo;
    @FXML private DatePicker entryDatePicker;
    @FXML private ComboBox<String> departmentCombo;
    @FXML private ComboBox<String> deviceCombo;
    @FXML private TextField serialField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> formStatusCombo;
    @FXML private TextField remarksField;

    /* ── Students Management Table ──────────────────────── */
    @FXML private TableView<StudentRow> studentsTable;
    @FXML private TableColumn<StudentRow, String> colFormId, colName, colUserCategory, colStudentId, colDepartment, colYearSection, colPhone, colDevice, colSerial, colColorDesc, colEntryDate, colStatus, colRemarks;

    /* ── Filter Controls ────────────────────────────────── */
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> statusPeriodCombo;
    @FXML private Label filteredCountLabel;

    /* ── Trash Bin View ──────────────────────────────────── */
    @FXML private VBox trashView;
    @FXML private TableView<StudentRow> trashTable;
    @FXML private TableColumn<StudentRow, String> trColFormId, trColName, trColUserCategory, trColStudentId, trColDepartment, trColYearSection, trColPhone, trColDevice, trColSerial, trColColorDesc, trColEntryDate, trColStatus, trColRemarks;
    @FXML private Label trashCountLabel;

    /* ── Wizard Export Views Variables ── */
    @FXML private DatePicker exportFromDate;
    @FXML private DatePicker exportToDate;
    @FXML private TextField exportCourseField;
    @FXML private CheckBox exportAllCategoriesCheckBox;
    @FXML private CheckBox exportStudent;
    @FXML private CheckBox exportStaff;
    @FXML private CheckBox exportGuest;
    @FXML private RadioButton scopePeriodRadio;
    @FXML private RadioButton scopeTemporalRadio;
    @FXML private Label scopePeriodLabel;
    @FXML private Label scopeTemporalLabel;
    @FXML private HBox periodScopeRow;
    @FXML private HBox temporalScopeRow;
    @FXML private ComboBox<String> exportScopeTypeCombo;
    @FXML private ComboBox<String> exportScopeValueCombo;
    private final javafx.scene.control.ToggleGroup scopeToggleGroup = new javafx.scene.control.ToggleGroup();
    @FXML private CheckBox exportAllCheckBox;
    @FXML private CheckBox exportDisplayDevices;
    @FXML private CheckBox exportAppliances;
    @FXML private CheckBox exportSoundsLight;
    @FXML private CheckBox exportProjectPrototypes;
    @FXML private CheckBox exportRentable;
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

        if (chartPeriodCombo != null) {
            chartPeriodCombo.getItems().addAll("Daily", "Weekly", "Monthly", "Quarterly", "Annually");
            chartPeriodCombo.setValue("Daily");
            chartPeriodCombo.setOnAction(e -> loadChartForPeriod(chartPeriodCombo.getValue()));
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

        if (userCategoryCombo != null) {
            userCategoryCombo.getItems().addAll("Student", "Staff", "Guest");
            userCategoryCombo.setValue("Student");
            userCategoryCombo.setOnAction(e -> {
                boolean isStudent = "Student".equals(userCategoryCombo.getValue());
                if (studentIdField != null) { studentIdField.setDisable(!isStudent); if (!isStudent) studentIdField.setText("N/A"); else studentIdField.clear(); }
                if (departmentCombo != null) { departmentCombo.setDisable(!isStudent); if (!isStudent) departmentCombo.setValue("N/A"); else departmentCombo.setValue("Select Course"); }
                if (yearSectionField != null) { yearSectionField.setDisable(!isStudent); if (!isStudent) yearSectionField.setText("N/A"); else yearSectionField.clear(); }
            });
        }

        if (departmentCombo != null) {
            departmentCombo.getItems().add("Select Course");
            departmentCombo.getItems().addAll(
                    "Bachelor of Science in Electronics Engineering (BSECE)",
                    "Bachelor of Science in Business Administration Major in Human Resource Management (BSBA-HRM)",
                    "Bachelor of Science in Business Administration Major in Marketing Management (BSBA-MM)",
                    "Bachelor in Secondary Education Major in English (BSEd-English)",
                    "Bachelor in Secondary Education Major in Filipino (BSEd-Filipino)",
                    "Bachelor in Secondary Education Major in Mathematics (BSEd-Mathematics)",
                    "Bachelor of Science in Industrial Engineering (BSIE)",
                    "Bachelor of Science in Information Technology (BSIT)",
                    "Bachelor of Science in Psychology (BSPSY)",
                    "Bachelor in Technology And Livelihood Education Major in Home Economics (BTLEd-HE)",
                    "Bachelor of Science in Management Accounting (BSMA)"
            );
            departmentCombo.setValue("Select Course");
        }

        if (deviceCombo != null) {
            deviceCombo.getItems().add("Select Items");
            deviceCombo.getItems().addAll(
                    "Display devices",
                    "Appliances",
                    "Sounds and light equipment",
                    "Other project prototypes",
                    "Rentable items (DDMI)"
            );
            deviceCombo.setValue("Select Items");
        }

        if (formStatusCombo != null) {
            formStatusCombo.getItems().add("Select Status");
            formStatusCombo.getItems().addAll("Approved", "Pending", "Disapproved", "Cancelled");
            formStatusCombo.setValue("Select Status");
        }

        setupTables();
        loadData();
        showView(View.MAIN);
        startNavClock();
        if (exportCsvBtn != null) selectFormat("CSV");

        if (exportAllCheckBox != null) exportAllCheckBox.setSelected(true);
        if (exportDisplayDevices != null) exportDisplayDevices.setSelected(true);
        if (exportAppliances != null) exportAppliances.setSelected(true);
        if (exportSoundsLight != null) exportSoundsLight.setSelected(true);
        if (exportProjectPrototypes != null) exportProjectPrototypes.setSelected(true);
        if (exportRentable != null) exportRentable.setSelected(true);
        if (exportAllCategoriesCheckBox != null) exportAllCategoriesCheckBox.setSelected(true);
        if (exportStudent != null) exportStudent.setSelected(true);
        if (exportStaff != null) exportStaff.setSelected(true);
        if (exportGuest != null) exportGuest.setSelected(true);

        if (scopePeriodRadio != null && scopeTemporalRadio != null) {
            scopePeriodRadio.setToggleGroup(scopeToggleGroup);
            scopeTemporalRadio.setToggleGroup(scopeToggleGroup);
            scopePeriodRadio.setSelected(true);
            applyScopeToggle("period");
        }

        if (exportScopeTypeCombo != null) {
            exportScopeTypeCombo.getItems().addAll("Annual", "Quarter", "Month");
            exportScopeTypeCombo.setOnAction(e -> loadExportScopeValues());
        }
    }

    @FXML
    private void handleScopeToggle() {
        boolean periodSelected = scopePeriodRadio != null && scopePeriodRadio.isSelected();
        applyScopeToggle(periodSelected ? "period" : "temporal");
    }

    private void applyScopeToggle(String active) {
        boolean isPeriod = active.equals("period");

        if (periodScopeRow != null)  { periodScopeRow.setDisable(!isPeriod);  periodScopeRow.setOpacity(isPeriod ? 1.0 : 0.4); }
        if (temporalScopeRow != null) { temporalScopeRow.setDisable(isPeriod); temporalScopeRow.setOpacity(isPeriod ? 0.4 : 1.0); }
        if (scopePeriodLabel != null)  scopePeriodLabel.setOpacity(isPeriod ? 1.0 : 0.4);
        if (scopeTemporalLabel != null) scopeTemporalLabel.setOpacity(isPeriod ? 0.4 : 1.0);
    }

    private void loadExportScopeValues() {
        if (exportScopeValueCombo == null || exportScopeTypeCombo == null) return;
        exportScopeValueCombo.getItems().clear();
        String type = exportScopeTypeCombo.getValue();
        if (type == null) return;

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                byodService.getDbUrl(), byodService.getDbUser(), byodService.getDbPass());
             java.sql.Statement st = conn.createStatement()) {

            switch (type) {
                case "Annual" -> {
                    java.sql.ResultSet rs = st.executeQuery(
                            "SELECT DISTINCT YEAR(ingress_time) as yr FROM student_device_logs ORDER BY yr DESC");
                    while (rs.next()) exportScopeValueCombo.getItems().add(String.valueOf(rs.getInt("yr")));
                }
                case "Quarter" -> {
                    java.sql.ResultSet rs = st.executeQuery(
                            "SELECT DISTINCT YEAR(ingress_time) as yr, QUARTER(ingress_time) as qr " +
                                    "FROM student_device_logs ORDER BY yr DESC, qr DESC");
                    while (rs.next()) {
                        int yr = rs.getInt("yr"); int qr = rs.getInt("qr");
                        String label = switch (qr) {
                            case 1 -> "Q1 (Jan-Mar), " + yr;
                            case 2 -> "Q2 (Apr-Jun), " + yr;
                            case 3 -> "Q3 (Jul-Sep), " + yr;
                            default -> "Q4 (Oct-Dec), " + yr;
                        };
                        exportScopeValueCombo.getItems().add(label);
                    }
                }
                case "Month" -> {
                    java.sql.ResultSet rs = st.executeQuery(
                            "SELECT DISTINCT YEAR(ingress_time) as yr, MONTH(ingress_time) as mn " +
                                    "FROM student_device_logs ORDER BY yr DESC, mn DESC");
                    String[] monthNames = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
                    while (rs.next()) {
                        int yr = rs.getInt("yr"); int mn = rs.getInt("mn");
                        exportScopeValueCombo.getItems().add(monthNames[mn - 1] + " " + yr);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
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

    private void setVisible(javafx.scene.Node pane, boolean show) {
        if (pane == null) return;
        pane.setVisible(show);
        pane.setManaged(show);
    }

    private void setupTables() {
        if (studentsTable != null) {
            colFormId.setCellValueFactory(new PropertyValueFactory<>("formId"));
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colUserCategory.setCellValueFactory(new PropertyValueFactory<>("userCategory"));
            colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colYearSection.setCellValueFactory(new PropertyValueFactory<>("yearSection"));
            colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
            colDevice.setCellValueFactory(new PropertyValueFactory<>("device"));
            colSerial.setCellValueFactory(new PropertyValueFactory<>("serial"));
            colColorDesc.setCellValueFactory(new PropertyValueFactory<>("colorDesc"));
            colEntryDate.setCellValueFactory(new PropertyValueFactory<>("entryDate"));
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
                    studentNameField.setText(newVal.getName());
                    if (userCategoryCombo != null) userCategoryCombo.setValue(newVal.getUserCategory());
                    boolean isStudent = "Student".equals(newVal.getUserCategory());
                    if (studentIdField != null) { studentIdField.setText(newVal.getStudentId()); studentIdField.setDisable(!isStudent); studentIdField.setEditable(false); }
                    if (departmentCombo != null) { departmentCombo.setValue(newVal.getDepartment()); departmentCombo.setDisable(!isStudent); }
                    if (yearSectionField != null) { yearSectionField.setText(newVal.getYearSection()); yearSectionField.setDisable(!isStudent); }
                    if (phoneField != null) phoneField.setText(newVal.getPhone());
                    if (deviceCombo != null) deviceCombo.setValue(newVal.getDevice());
                    if (serialField != null) serialField.setText(newVal.getSerial());
                    if (colorDescField != null) colorDescField.setText(newVal.getColorDesc());
                    if (entryDatePicker != null && newVal.getEntryDate() != null && !newVal.getEntryDate().equals("N/A")) {
                        try { entryDatePicker.setValue(java.time.LocalDate.parse(newVal.getEntryDate())); } catch (Exception ignored) {}
                    } else if (entryDatePicker != null) { entryDatePicker.setValue(null); }
                    if (formStatusCombo != null) formStatusCombo.setValue(newVal.getStatus());
                    if (remarksField != null) remarksField.setText(newVal.getRemarks());
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
            trColFormId.setCellValueFactory(new PropertyValueFactory<>("formId"));
            trColName.setCellValueFactory(new PropertyValueFactory<>("name"));
            trColUserCategory.setCellValueFactory(new PropertyValueFactory<>("userCategory"));
            trColStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            trColDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            trColYearSection.setCellValueFactory(new PropertyValueFactory<>("yearSection"));
            trColPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
            trColDevice.setCellValueFactory(new PropertyValueFactory<>("device"));
            trColSerial.setCellValueFactory(new PropertyValueFactory<>("serial"));
            trColColorDesc.setCellValueFactory(new PropertyValueFactory<>("colorDesc"));
            trColEntryDate.setCellValueFactory(new PropertyValueFactory<>("entryDate"));
            trColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            trColRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
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
        if (studentNameField != null) studentNameField.clear();
        if (userCategoryCombo != null) userCategoryCombo.setValue("Student");
        if (studentIdField != null) { studentIdField.clear(); studentIdField.setDisable(false); studentIdField.setEditable(true); }
        if (departmentCombo != null) { departmentCombo.setValue("Select Course"); departmentCombo.setDisable(false); }
        if (yearSectionField != null) { yearSectionField.clear(); yearSectionField.setDisable(false); }
        if (phoneField != null) phoneField.clear();
        if (deviceCombo != null) deviceCombo.setValue("Select Items");
        if (serialField != null) serialField.clear();
        if (colorDescField != null) colorDescField.clear();
        if (entryDatePicker != null) entryDatePicker.setValue(null);
        if (formStatusCombo != null) formStatusCombo.setValue("Select Status");
        if (remarksField != null) remarksField.clear();
    }

    @FXML
    private void handleInsertStudent() {
        String id = studentIdField.getText().trim();
        String name = studentNameField.getText().trim();
        String dept = departmentCombo != null ? departmentCombo.getValue() : "";
        String type = deviceCombo != null ? deviceCombo.getValue() : "";
        String serial = serialField.getText().trim();
        String phone = phoneField.getText().trim();
        String status  = formStatusCombo != null ? formStatusCombo.getValue() : "Pending";
        String remarks = remarksField != null ? remarksField.getText().trim() : "";

        if (id.isEmpty() || name.isEmpty()) {
            showAlert("Validation Missing", "Student ID and Full Name properties are required.");
            return;
        }

        if (dept == null || dept.equals("Select Course")) {
            showAlert("Validation Missing", "Please select a Department / Course.");
            return;
        }
        if (type == null || type.equals("Select Items")) {
            showAlert("Validation Missing", "Please select an Item Category.");
            return;
        }
        if (status == null || status.equals("Select Status")) {
            showAlert("Validation Missing", "Please select a Status.");
            return;
        }

        boolean success = byodService.insertRegisteredStudent(id, name, dept, type, serial, phone, status, remarks);
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
        String name = studentNameField.getText().trim();
        String userCategory = userCategoryCombo != null ? userCategoryCombo.getValue() : "Student";
        String id = studentIdField.getText().trim();
        String dept = departmentCombo != null ? departmentCombo.getValue() : "";
        String yearSection = yearSectionField != null ? yearSectionField.getText().trim() : "";
        String phone = phoneField.getText().trim();
        String type = deviceCombo != null ? deviceCombo.getValue() : "";
        String serial = serialField.getText().trim();
        String colorDesc = colorDescField != null ? colorDescField.getText().trim() : "";
        String entryDate = entryDatePicker != null && entryDatePicker.getValue() != null ? entryDatePicker.getValue().toString() : "";
        String status = formStatusCombo != null ? formStatusCombo.getValue() : "Pending";
        String remarks = remarksField != null ? remarksField.getText().trim() : "";

        if (name.isEmpty()) {
            showAlert("Validation Error", "No record selected to modify.");
            return;
        }
        if (type == null || type.equals("Select Items")) {
            showAlert("Validation Missing", "Please select an Item Category.");
            return;
        }
        if (status == null || status.equals("Select Status")) {
            showAlert("Validation Missing", "Please select a Status.");
            return;
        }

        boolean success = byodService.updateRegisteredStudent(id, name, dept, type, serial, phone, status, remarks);
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
            trashTable.getItems().add(new StudentRow(
                    row[0], row[1], row[2], row[3], row[4],
                    row.length > 5 ? row[5] : "N/A",
                    row.length > 6 ? row[6] : "N/A",
                    row.length > 7 ? row[7] : "Unknown",
                    row.length > 8 ? row[8] : "N/A",
                    row.length > 9 ? row[9] : "N/A",
                    row.length > 10 ? row[10] : "N/A",
                    row.length > 11 ? row[11] : "N/A",
                    row.length > 12 ? row[12] : ""
            ));
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

        String currentPeriod = (chartPeriodCombo != null && chartPeriodCombo.getValue() != null)
                ? chartPeriodCombo.getValue() : "Daily";
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        pause.setOnFinished(e -> loadChartForPeriod(currentPeriod));
        pause.play();
        loadStudentsData();
    }

    private void loadChartForPeriod(String period) {
        if (weeklyChart == null) return;
        weeklyChart.getData().clear();

        CategoryAxis xAxis = (CategoryAxis) weeklyChart.getXAxis();
        javafx.scene.chart.NumberAxis yAxis = (javafx.scene.chart.NumberAxis) weeklyChart.getYAxis();
        int currentYear = java.time.LocalDate.now().getYear();

        Map<String, Map<String, Integer>> data;
        List<String> labels = new ArrayList<>();
        String title;

        switch (period) {
            case "Weekly" -> {
                data = byodService.fetchWeeklyByDayChartData();
                labels.addAll(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
                String monthName = java.time.LocalDate.now().getMonth()
                        .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
                title = "Ingress-egress by: Weekly · " + monthName + " " + currentYear;
            }
            case "Monthly" -> {
                data = byodService.fetchMonthlyChartData();
                labels.addAll(List.of("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"));
                title = "Ingress-egress by: Monthly · " + currentYear;
            }
            case "Quarterly" -> {
                data = byodService.fetchQuarterlyChartData();
                labels.addAll(List.of("Q1 (Jan-Mar)", "Q2 (Apr-Jun)", "Q3 (Jul-Sep)", "Q4 (Oct-Dec)"));
                title = "Ingress-egress by: Quarterly · " + currentYear;
            }
            case "Annually" -> {
                data = byodService.fetchAnnualChartData();
                labels.addAll(data.get("Ingress").keySet());
                title = "Ingress-egress by: Annually";
            }
            default -> {
                data = byodService.fetchDailyChartData();
                labels.addAll(List.of(
                        "12AM","1AM","2AM","3AM","4AM","5AM","6AM","7AM","8AM","9AM","10AM","11AM",
                        "12PM","1PM","2PM","3PM","4PM","5PM","6PM","7PM","8PM","9PM","10PM","11PM"
                ));
                title = "Ingress-egress by: Daily";
            }
        }

        if (weeklyChartTitle != null) weeklyChartTitle.setText(title);

        Map<String, Integer> ingressMap = data.get("Ingress");
        Map<String, Integer> egressMap  = data.get("Egress");

        int maxVal = Math.max(
                ingressMap.values().stream().mapToInt(v -> v).max().orElse(0),
                egressMap.values().stream().mapToInt(v -> v).max().orElse(0)
        );

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(Math.max(5, maxVal + 1));
        yAxis.setTickUnit(1);
        yAxis.setMinorTickCount(0);

        XYChart.Series<String, Number> ingressSeries = new XYChart.Series<>();
        ingressSeries.setName("Ingress");
        XYChart.Series<String, Number> egressSeries = new XYChart.Series<>();
        egressSeries.setName("Egress");

        for (String label : labels) {
            ingressSeries.getData().add(new XYChart.Data<>(label, ingressMap.getOrDefault(label, 0)));
            egressSeries.getData().add(new XYChart.Data<>(label, egressMap.getOrDefault(label, 0)));
        }

        weeklyChart.getData().addAll(ingressSeries, egressSeries);
        xAxis.setAutoRanging(false);
        xAxis.setCategories(FXCollections.observableArrayList(labels));
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
                    row[0], row[1], row[2], row[3], row[4],
                    row.length > 5 ? row[5] : "N/A",
                    row.length > 6 ? row[6] : "N/A",
                    row.length > 7 ? row[7] : "Unknown",
                    row.length > 8 ? row[8] : "N/A",
                    row.length > 9 ? row[9] : "N/A",
                    row.length > 10 ? row[10] : "N/A",
                    row.length > 11 ? row[11] : "N/A",
                    row.length > 12 ? row[12] : ""
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
                                row[0], row[1], row[2], row[3], row[4],
                                row.length > 5 ? row[5] : "N/A",
                                row.length > 6 ? row[6] : "N/A",
                                row.length > 7 ? row[7] : "Unknown",
                                row.length > 8 ? row[8] : "N/A",
                                row.length > 9 ? row[9] : "N/A",
                                row.length > 10 ? row[10] : "N/A",
                                row.length > 11 ? row[11] : "N/A",
                                row.length > 12 ? row[12] : ""
                        ));
                    }

                    // Optional: Notify the user
                    if (filteredData.isEmpty()) {
                        showAlert("No Records", "No devices were registered on " + targetDate);
                    }
                });
            }
       @FXML private void handleRefresh() {
        Stage owner = (Stage) reportsButton.getScene().getWindow();

        Dialog<Void> loadingDialog = new Dialog<>();
        loadingDialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        loadingDialog.initOwner(owner);
        loadingDialog.setTitle("Refreshing");
        loadingDialog.setHeaderText(null);
        loadingDialog.setGraphic(null);
        loadingDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        VBox dialogContent = new VBox(12);
        dialogContent.setAlignment(javafx.geometry.Pos.CENTER);
        dialogContent.setStyle("-fx-padding: 20;");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        Label msg = new Label("Fetching latest data...");
        msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");
        dialogContent.getChildren().addAll(spinner, msg);
        loadingDialog.getDialogPane().setContent(dialogContent);

        loadingDialog.getDialogPane().lookupButton(ButtonType.CANCEL).addEventFilter(
                ActionEvent.ACTION, e -> loadingDialog.close());

        loadingDialog.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 12;");

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override protected Void call() throws Exception {
                Thread.sleep(400);
                loadData();
                return null;
            }
        };
        task.setOnSucceeded(e -> loadingDialog.close());
        task.setOnFailed(e -> loadingDialog.close());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        loadingDialog.showAndWait();
    }
    @FXML private void handleDashboard()    { navigateTo("/fxml/dashboard.fxml"); }
    @FXML private void handleMonitoring()   { navigateTo("/fxml/monitoring.fxml"); }
    @FXML private void handleRegistration() { navigateTo("/fxml/registration.fxml"); }
    @FXML private void handleReports(javafx.event.ActionEvent e) { showView(View.MAIN); }

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
        if (exportDisplayDevices != null) exportDisplayDevices.setSelected(state);
        if (exportAppliances != null) exportAppliances.setSelected(state);
        if (exportSoundsLight != null) exportSoundsLight.setSelected(state);
        if (exportProjectPrototypes != null) exportProjectPrototypes.setSelected(state);
        if (exportRentable != null) exportRentable.setSelected(state);
    }

    @FXML
    private void handleSelectAllCategories() {
        if (exportAllCategoriesCheckBox == null) return;
        boolean state = exportAllCategoriesCheckBox.isSelected();
        if (exportStudent != null) exportStudent.setSelected(state);
        if (exportStaff != null) exportStaff.setSelected(state);
        if (exportGuest != null) exportGuest.setSelected(state);
    }

    @FXML private void handleExportCsv()    { selectFormat("CSV");   showSuccessAlert("Selected Format changed to standard CSV Data stream."); }
    @FXML private void handleExportPdf()    { selectFormat("PDF");   showSuccessAlert("Selected Format changed to Adobe structural PDF compilation."); }
    @FXML private void handleExportXls()    { selectFormat("EXCEL"); showSuccessAlert("Selected Format changed to Microsoft Excel Spreadsheet cluster."); }

    private void selectFormat(String format) {
        this.selectedFormat = format;
        exportCsvBtn.getStyleClass().remove("format-btn-selected");
        exportPdfBtn.getStyleClass().remove("format-btn-selected");
        exportXlsBtn.getStyleClass().remove("format-btn-selected");
        switch (format) {
            case "CSV":   exportCsvBtn.getStyleClass().add("format-btn-selected"); break;
            case "PDF":   exportPdfBtn.getStyleClass().add("format-btn-selected"); break;
            case "EXCEL": exportXlsBtn.getStyleClass().add("format-btn-selected"); break;
        }
    }

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
            boolean noCourseSelected = exportAllCategoriesCheckBox != null && !exportAllCategoriesCheckBox.isSelected()
                    && (exportStudent == null || !exportStudent.isSelected())
                    && (exportStaff == null || !exportStaff.isSelected())
                    && (exportGuest == null || !exportGuest.isSelected());
            if (noCourseSelected) {
                showAlert("Validation Missing", "Please select at least one Student Course.");
                return;
            }

            boolean noItemSelected = exportAllCheckBox != null && !exportAllCheckBox.isSelected()
                    && (exportDisplayDevices == null || !exportDisplayDevices.isSelected())
                    && (exportAppliances == null || !exportAppliances.isSelected())
                    && (exportSoundsLight == null || !exportSoundsLight.isSelected())
                    && (exportProjectPrototypes == null || !exportProjectPrototypes.isSelected())
                    && (exportRentable == null || !exportRentable.isSelected());
            if (noItemSelected) {
                showAlert("Validation Missing", "Please select at least one Item Category.");
                return;
            }

            List<String[]> extractionDatabaseRows;

            boolean usingPeriodScope = scopePeriodRadio != null && scopePeriodRadio.isSelected();
            if (usingPeriodScope) {
                String scopeType  = exportScopeTypeCombo != null ? exportScopeTypeCombo.getValue() : null;
                String scopeValue = exportScopeValueCombo != null ? exportScopeValueCombo.getValue() : null;
                if (scopeType == null || scopeType.isEmpty()) {
                    showAlert("Validation Missing", "Please select a scope type.");
                    return;
                }
                if (scopeValue == null || scopeValue.isEmpty()) {
                    showAlert("Validation Missing", "Please select a scope value.");
                    return;
                }
                extractionDatabaseRows = byodService.fetchExportByPeriodScope(scopeType, scopeValue);
            } else {
                LocalDate from = exportFromDate != null ? exportFromDate.getValue() : null;
                LocalDate to   = exportToDate   != null ? exportToDate.getValue()   : null;
                extractionDatabaseRows = byodService.fetchExportByDateRange(from, to);
            }
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
        private final String formId, name, userCategory, studentId, department, yearSection, phone, device, serial, colorDesc, entryDate, status, remarks;
        public StudentRow(String formId, String name, String userCategory, String studentId, String department, String yearSection, String phone, String device, String serial, String colorDesc, String entryDate, String status, String remarks) {
            this.formId=formId; this.name=name; this.userCategory=userCategory; this.studentId=studentId;
            this.department=department; this.yearSection=yearSection; this.phone=phone;
            this.device=device; this.serial=serial; this.colorDesc=colorDesc;
            this.entryDate=entryDate; this.status=status; this.remarks=remarks;
        }
        public String getFormId()       { return formId; }
        public String getName()         { return name; }
        public String getUserCategory() { return userCategory; }
        public String getStudentId()    { return studentId; }
        public String getDepartment()   { return department; }
        public String getYearSection()  { return yearSection; }
        public String getPhone()        { return phone; }
        public String getDevice()       { return device; }
        public String getSerial()       { return serial; }
        public String getColorDesc()    { return colorDesc; }
        public String getEntryDate()    { return entryDate; }
        public String getStatus()       { return status; }
        public String getRemarks()      { return remarks; }
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
            stage.setMinWidth(600);
            stage.setMinHeight(550);
            stage.setMaxWidth(600);
            stage.setMaxHeight(550);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }
}