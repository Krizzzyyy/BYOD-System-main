package com.example.registration;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.example.service.BYODService;
import com.example.Auth;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class RegistrationController {

    @FXML private Label stepStudentInfo;
    @FXML private Label navDateTimeLabel;
    @FXML private Label stepDeviceDetails;
    @FXML private Label stepReview;

    @FXML private VBox stepPanel1;
    @FXML private VBox stepPanel2;
    @FXML private VBox stepPanel3;

    @FXML private TextField lastNameField;
    @FXML private TextField firstNameField;
    @FXML private ComboBox<String> courseCombo;
    @FXML private TextField yearSectionField;
    @FXML private TextField studentIdField;
    @FXML private TextField contactField;

    @FXML private DatePicker scheduledEntryDate;

    @FXML private ComboBox<String> deviceTypeCombo;
    @FXML private TextField        brandModelField;
    @FXML private TextField        colorDescField;
    @FXML private TextField        serialNumberField;
    @FXML private VBox             serialNumberBox;

    @FXML private Region connector1;
    @FXML private Region connector2;

    @FXML private VBox  savedDevicesBox;
    @FXML private Label savedDevicesTitle;
    @FXML private VBox  savedDevicesList;

    @FXML private Label reviewLastName;
    @FXML private Label reviewFirstName;
    @FXML private Label reviewStudentId;
    @FXML private Label reviewYearSection;
    @FXML private Label reviewCourse;
    @FXML private Label reviewContact;
    @FXML private Label reviewScheduledDate;
    @FXML private Label reviewDevicesTitle;
    @FXML private VBox  reviewDevicesList;

    @FXML private Button backBtn;
    @FXML private Button addAnotherDevBtn;
    @FXML private Button saveDeviceBtn;
    @FXML private Button nextStepBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label  formIdLabel;
    @FXML private Button monitoringButton;
    @FXML private Button reportsButton;

    private static final int TOTAL_STEPS = 3;
    private int currentStep = 1;
    private boolean step1Completed = false;
    private boolean step2Completed = false;

    private static class DeviceEntry {
        String type, brand, color, serial;
        DeviceEntry(String t, String b, String c, String s) { type=t; brand=b; color=c; serial=s; }
        static String nvl(String s) { return (s==null||s.isBlank()) ? "—" : s; }
        @Override public String toString() {
            String base = nvl(type) + "  ·  " + nvl(brand) + "  ·  " + nvl(color);
            return (serial != null && !serial.isBlank()) ? base + "  ·  " + nvl(serial) : base;
        }
    }
    private final List<DeviceEntry> savedDevices = new ArrayList<>();

    private static final String[] COURSE_LIST = {
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
    };

    private static final String[] REGISTERED_ITEM_CATEGORIES = {
            "Display devices",
            "Appliances",
            "Sounds and light equipment",
            "Other project prototypes",
            "Rentable items (DDMI)"
    };

    @FXML
    public void initialize() {
        if (formIdLabel != null)
            formIdLabel.setText("Form ID: BYOD-2026-" + String.format("%05d", (int)(Math.random()*99999)));
        if (deviceTypeCombo != null)
            deviceTypeCombo.getItems().addAll(REGISTERED_ITEM_CATEGORIES);
        if (courseCombo != null)
            courseCombo.getItems().addAll(COURSE_LIST);

        // Live-update Add Device button state as the user fills in step 2 fields
        if (deviceTypeCombo != null)
            deviceTypeCombo.valueProperty().addListener((obs, o, n) -> {
                updateSaveDeviceBtnState();
                updateSerialNumberVisibility();
            });
        if (brandModelField != null)
            brandModelField.textProperty().addListener((obs, o, n) -> updateSaveDeviceBtnState());
        if (colorDescField != null)
            colorDescField.textProperty().addListener((obs, o, n) -> updateSaveDeviceBtnState());

        updateSerialNumberVisibility();
        showStep(1);
        startNavClock();
        applyRoleRestrictions();
    }

    /** Hides serial number field for categories that don't apply (e.g., project prototypes). */
    private void updateSerialNumberVisibility() {
        String category = deviceTypeCombo != null ? deviceTypeCombo.getValue() : null;
        boolean showSerial = category != null && !category.equals("Other project prototypes");
        if (serialNumberBox != null) {
            serialNumberBox.setVisible(showSerial);
            serialNumberBox.setManaged(showSerial);
            if (!showSerial && serialNumberField != null) {
                serialNumberField.clear();
            }
        }
    }

    private void applyRoleRestrictions() {
        if (!"Student".equals(Auth.userRole)) return;
        if (monitoringButton != null) { monitoringButton.setManaged(false); monitoringButton.setVisible(false); }
        if (reportsButton != null) { reportsButton.setManaged(false); reportsButton.setVisible(false); }
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

    private void showStep(int step) {
        currentStep = step;
        setPanel(stepPanel1, step == 1);
        setPanel(stepPanel2, step == 2);
        setPanel(stepPanel3, step == 3);

        setBtn(backBtn,          step > 1);
        setBtn(addAnotherDevBtn, step == 2 && !savedDevices.isEmpty());
        setBtn(saveDeviceBtn,    step == 2 && savedDevices.isEmpty());
        setBtn(nextStepBtn,      step < TOTAL_STEPS);
        setBtn(saveBtn,          step == TOTAL_STEPS);
        setBtn(cancelBtn,        step == TOTAL_STEPS);

        updateDots(step);

        if (step == 2) {
            updateSaveDeviceBtnState();
            updateAddAnotherBtnState();
        }
        if (step == TOTAL_STEPS) updateSaveBtnState();
    }

    private void setPanel(Pane p, boolean show) {
        if (p == null) return;
        p.setVisible(show);
        p.setManaged(show);
    }

    private void setBtn(Button b, boolean show) {
        if (b == null) return;
        b.setVisible(show);
        b.setManaged(show);
    }

    private void updateDots(int active) {
        Label[] dots = { stepStudentInfo, stepDeviceDetails, stepReview };
        Region[] connectors = { connector1, connector2 };

        for (int i = 0; i < dots.length; i++) {
            Label d = dots[i];
            if (d == null) continue;
            d.getStyleClass().remove("step-active");
            d.getStyleClass().remove("step-complete");
            d.getStyleClass().remove("step-inactive");

            boolean completed = (i == 0 && step1Completed) || (i == 1 && step2Completed);

            if (i + 1 < active) {
                if (completed) {
                    d.setText(String.valueOf(i + 1));
                    d.getStyleClass().add("step-complete");
                } else {
                    d.setText(String.valueOf(i + 1));
                    d.getStyleClass().add("step-inactive");
                }
            } else if (i + 1 == active) {
                d.setText(String.valueOf(i + 1));
                d.getStyleClass().add("step-active");
            } else {
                d.setText(String.valueOf(i + 1));
                d.getStyleClass().add("step-inactive");
            }

            // Update step label styling (label is the second child of the VBox parent)
            if (d.getParent() instanceof VBox parentBox) {
                var children = parentBox.getChildrenUnmodifiable();
                if (children.size() > 1 && children.get(1) instanceof Label stepLabel) {
                    stepLabel.getStyleClass().remove("step-label-active");
                    stepLabel.getStyleClass().remove("step-label-complete");
                    if (completed) {
                        stepLabel.getStyleClass().add("step-label-complete");
                    } else if (i + 1 == active) {
                        stepLabel.getStyleClass().add("step-label-active");
                    }
                }
            }
        }

        // Update connectors
        for (int i = 0; i < connectors.length; i++) {
            Region c = connectors[i];
            if (c == null) continue;
            c.getStyleClass().remove("step-connector-complete");
            boolean prevCompleted = (i == 0 && step1Completed) || (i == 1 && step2Completed);
            if (prevCompleted) {
                c.getStyleClass().add("step-connector-complete");
            }
        }
    }

    private boolean isStep1Valid() {
        String lastName    = lastNameField   != null ? lastNameField.getText().trim()   : "";
        String firstName   = firstNameField  != null ? firstNameField.getText().trim()  : "";
        String studentId   = studentIdField  != null ? studentIdField.getText().trim()  : "";
        String yearSection = yearSectionField!= null ? yearSectionField.getText().trim() : "";
        String contact     = contactField    != null ? contactField.getText().trim()    : "";
        String course      = courseCombo     != null ? courseCombo.getValue()           : null;

        if (lastName.isEmpty() || firstName.isEmpty() || studentId.isEmpty()
                || yearSection.isEmpty() || contact.isEmpty()) {
            showAlert("Missing Information", "Please fill in all student information fields.");
            return false;
        }
        if (course == null || course.isBlank()) {
            showAlert("Missing Information", "Please select a Course / Program.");
            return false;
        }
        if (scheduledEntryDate != null && scheduledEntryDate.getValue() != null) {
            if (scheduledEntryDate.getValue().isBefore(LocalDate.now())) {
                showAlert("Invalid Date", "Scheduled entry date cannot be in the past.");
                return false;
            }
        }

        String validation = RegistrationValidator.validate(studentId, firstName, lastName, contact, yearSection);
        if (!validation.equals("VALID")) {
            showAlert("Invalid Input", validation);
            return false;
        }
        return true;
    }

    private boolean isStep2Valid() {
        if (savedDevices.isEmpty()) {
            showAlert("No Device Added", "Please add at least one device before continuing.");
            return false;
        }
        return true;
    }

    /** True only if the current item inputs are complete. */
    private boolean isDeviceInputComplete() {
        String type  = deviceTypeCombo != null ? deviceTypeCombo.getValue() : null;
        String brand = brandModelField != null ? brandModelField.getText().trim() : "";
        String color = colorDescField  != null ? colorDescField.getText().trim()  : "";
        return type != null && !type.isBlank() && !brand.isEmpty() && !color.isEmpty();
    }

    /** Updates the Add Device button's enabled state/color based on current input completeness. */
    private void updateSaveDeviceBtnState() {
        if (saveDeviceBtn == null) return;

        // Add Device is only shown for the FIRST item.
        // After at least one item is saved, use Add Another Item for the next entries.
        boolean show = currentStep == 2 && savedDevices.isEmpty();
        boolean complete = isDeviceInputComplete();

        saveDeviceBtn.setVisible(show);
        saveDeviceBtn.setManaged(show);
        saveDeviceBtn.setDisable(!complete);
        saveDeviceBtn.getStyleClass().removeAll("btn-disabled", "btn-save-device");
        saveDeviceBtn.getStyleClass().add(complete ? "btn-save-device" : "btn-disabled");

        updateAddAnotherBtnState();
    }

    private void updateAddAnotherBtnState() {
        if (addAnotherDevBtn == null) return;

        // Show Add Another Item only after the first item is already saved.
        boolean show = currentStep == 2 && !savedDevices.isEmpty();
        addAnotherDevBtn.setVisible(show);
        addAnotherDevBtn.setManaged(show);
        addAnotherDevBtn.setDisable(!isDeviceInputComplete());
    }

    private void updateSaveBtnState() {
        if (saveBtn == null) return;
        boolean ready = !savedDevices.isEmpty();
        saveBtn.setDisable(!ready);
        saveBtn.getStyleClass().removeAll("btn-disabled", "btn-save");
        saveBtn.getStyleClass().add(ready ? "btn-save" : "btn-disabled");
    }

    @FXML
    private void handleBack() {
        if (currentStep > 1) showStep(currentStep - 1);
    }

    @FXML
    private void handleAddAnotherDevice() {
        // "Add Another Device" now simply reserves a fresh, empty slot for the next device.
        // Saving is handled exclusively by handleSaveDevice().
        if (!isDeviceInputComplete()) {
            showAlert("Incomplete Device Info", "Please complete the current device's information before adding another.");
            return;
        }
        handleSaveDevice();
    }

    /** Saves the current device info as a new entry. Only works if all fields are complete. */
    @FXML
    private void handleSaveDevice() {
        if (!isDeviceInputComplete()) {
            showAlert("Incomplete Item Info", "Please fill in Item category, Brand/model, and Color/description before saving.");
            return;
        }

        String type   = deviceTypeCombo.getValue();
        String brand  = brandModelField.getText().trim();
        String color  = colorDescField.getText().trim();
        String serial = serialNumberField != null ? serialNumberField.getText().trim() : "";

        savedDevices.add(new DeviceEntry(type, brand, color, serial));
        step2Completed = true;
        refreshSavedDevicesUI();
        updateDots(currentStep);

        deviceTypeCombo.setValue(null);
        brandModelField.clear();
        colorDescField.clear();
        if (serialNumberField != null) serialNumberField.clear();
        deviceTypeCombo.requestFocus();

        updateSaveDeviceBtnState();
        updateSerialNumberVisibility();
    }

    private void refreshSavedDevicesUI() {
        if (savedDevicesBox == null || savedDevicesList == null) return;
        savedDevicesBox.setVisible(!savedDevices.isEmpty());
        savedDevicesBox.setManaged(!savedDevices.isEmpty());
        if (savedDevicesTitle != null)
            savedDevicesTitle.setText("Saved items (" + savedDevices.size() + ")");
        updateAddAnotherBtnState();

        savedDevicesList.getChildren().clear();
        for (int i = 0; i < savedDevices.size(); i++) {
            DeviceEntry e = savedDevices.get(i);
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:#f7f3ec;-fx-background-radius:6;-fx-padding:8 12;");

            Label num  = new Label((i+1) + ".");
            num.setStyle("-fx-font-weight:700;-fx-text-fill:#888;-fx-min-width:20;");

            Label info = new Label(e.toString());
            info.setStyle("-fx-text-fill:#333;");
            HBox.setHgrow(info, Priority.ALWAYS);

            final int idx = i;
            Button rm = new Button("✕");
            rm.setStyle("-fx-background-color:transparent;-fx-text-fill:#c0392b;-fx-cursor:hand;-fx-font-weight:700;");
            rm.setOnAction(ev -> {
                savedDevices.remove(idx);
                refreshSavedDevicesUI();
                if (savedDevices.isEmpty()) {
                    step2Completed = false;
                    updateDots(currentStep);
                }
                updateSaveDeviceBtnState();
                updateAddAnotherBtnState();
            });

            row.getChildren().addAll(num, info, rm);
            savedDevicesList.getChildren().add(row);
        }
    }

    @FXML
    private void handleNextStep() {
        if (currentStep == 1) {
            if (!isStep1Valid()) return;
            step1Completed = true;
            showStep(2);
        } else if (currentStep == 2) {
            // No auto-save here anymore — item(s) must already be added via "Add Device" or "+ Add Another Item".
            // This prevents creating duplicate entries when navigating back and forth.
            if (!isStep2Valid()) return;
            step2Completed = true;
            populateReview();
            showStep(3);
        }
    }

    private void populateReview() {
        set(reviewLastName,    lastNameField);
        set(reviewFirstName,   firstNameField);
        set(reviewStudentId,   studentIdField);
        set(reviewYearSection, yearSectionField);
        set(reviewContact,     contactField);

        if (reviewCourse != null) {
            String course = courseCombo != null ? courseCombo.getValue() : null;
            reviewCourse.setText(course == null || course.isBlank() ? "-" : course);
        }
        if (reviewScheduledDate != null) {
            if (scheduledEntryDate != null && scheduledEntryDate.getValue() != null) {
                reviewScheduledDate.setText(scheduledEntryDate.getValue().toString());
            } else {
                reviewScheduledDate.setText("-");
            }
        }

        if (reviewDevicesTitle != null)
            reviewDevicesTitle.setText("Registered Items (" + savedDevices.size() + ")");

        if (reviewDevicesList != null) {
            reviewDevicesList.getChildren().clear();
            for (int i = 0; i < savedDevices.size(); i++) {
                DeviceEntry e = savedDevices.get(i);
                VBox card = new VBox(4);
                card.setStyle("-fx-background-color:#f7f3ec;-fx-background-radius:6;-fx-padding:10 14;");

                Label title = new Label("Item " + (i+1) + " — " + DeviceEntry.nvl(e.type));
                title.setStyle("-fx-font-weight:700;-fx-text-fill:#333;");

                VBox details = new VBox(2);
                details.getChildren().addAll(
                        makeReviewField("Brand/Model:", e.brand),
                        makeReviewField("Color:", e.color)
                );
                if (e.serial != null && !e.serial.isBlank()) {
                    details.getChildren().add(makeReviewField("Serial No.:", e.serial));
                }

                card.getChildren().addAll(title, details);
                reviewDevicesList.getChildren().add(card);
            }
        }
    }

    private Label makeReviewField(String label, String value) {
        Label lbl = new Label(label + "  " + DeviceEntry.nvl(value));
        lbl.setStyle("-fx-text-fill:#666;-fx-font-size:12;");
        return lbl;
    }

    private void set(Label lbl, TextField tf) {
        if (lbl != null && tf != null)
            lbl.setText(tf.getText().trim().isEmpty() ? "-" : tf.getText().trim());
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);

        a.showAndWait();
    }

    private final BYODService byodService = new BYODService();

    @FXML
    private void handleSave() {
        if (!isStep1Valid() || !isStep2Valid()) return;

        if (scheduledEntryDate == null || scheduledEntryDate.getValue() == null) {
            showAlert("Missing Information", "Please select a Scheduled Entry Date.");
            return;
        }

        String sid = studentIdField.getText().trim();
        String course = courseCombo != null && courseCombo.getValue() != null ? courseCombo.getValue() : "";
        String scheduledDate = scheduledEntryDate.getValue().toString();

        String validation = byodService.validate(sid, firstNameField.getText().trim(),
                lastNameField.getText().trim(), contactField.getText().trim(),
                yearSectionField.getText().trim());

        if (!validation.equals("VALID")) {
            showAlert("Invalid Input", validation);
            return;
        }

        try {
            for (DeviceEntry d : savedDevices) {
                byodService.registerStudent(
                        sid,
                        lastNameField.getText().trim(),
                        firstNameField.getText().trim(),
                        yearSectionField.getText().trim(),
                        course,
                        contactField.getText().trim(),
                        d.type,
                        d.brand,
                        d.color,
                        d.serial,
                        scheduledDate
                );
            }

            String payload = String.join("|", sid, lastNameField.getText(), firstNameField.getText(),
                    yearSectionField.getText(), course, contactField.getText());
            String qrPath = byodService.generateQR(payload, sid, System.getProperty("user.dir"));

            Stage activeStage = (Stage) cancelBtn.getScene().getWindow();
            com.example.monitoring.QRRegistrationSuccessWindow.show(activeStage, sid, qrPath);

            navigateTo("/fxml/dashboard.fxml");

        } catch (Exception ex) {
            showAlert("Database Error", ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if ("Student".equals(Auth.userRole)) {
            navigateTo("/fxml/dashboard.fxml");
        } else {
            navigateTo("/fxml/monitoring.fxml");
        }
    }

    @FXML private void handleDashboard()    { navigateTo("/fxml/dashboard.fxml"); }
    @FXML private void handleMonitoring()   { navigateTo("/fxml/monitoring.fxml"); }
    @FXML
    private void handleReports() {
        if (Auth.reportUnlocked) {
            navigateTo("/fxml/reports.fxml");
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

        VBox vbox = new VBox(10, new Label("Password:"), passwordField, errorLabel);
        vbox.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(vbox);


        dialog.setResultConverter(btn -> {
            if (btn == loginBtn) return passwordField.getText();
            return null;
        });

        dialog.showAndWait().ifPresent(password -> {
            if ("password".equals(password)) {
                Auth.reportUnlocked = true;
                navigateTo("/fxml/reports.fxml");
            } else {
                errorLabel.setText("Incorrect password. Access denied.");
            }
        });
    }
    @FXML private void handleRegistration() { /* already here */ }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml.toLowerCase()));
            Stage stage = (Stage) cancelBtn.getScene().getWindow();
            Scene current = stage.getScene();
            current.getStylesheets().clear();
            current.getStylesheets().add(getClass().getResource("/css/stylesheet.css").toExternalForm());
            current.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleLogout() {
        Auth.isLoggedIn = false;
        Auth.userRole = null;
        Auth.reportUnlocked = false;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) cancelBtn.getScene().getWindow();
            stage.setMaximized(false);
            stage.setMinWidth(600);
            stage.setMinHeight(550);
            stage.setMaxWidth(600);
            stage.setMaxHeight(550);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/stylesheet.css").toExternalForm());
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }
}